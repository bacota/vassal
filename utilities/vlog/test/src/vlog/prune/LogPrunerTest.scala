package vlog.prune

import org.scalatest.funsuite.AnyFunSuite
import vlog.command.*
import vlog.format.SequenceCodec

/** Tests for [[LogPruner]], which removes undone actions and their undo
  * markers from a .vlog command stream.
  *
  * The fixtures reproduce the on-disk shape VASSAL writes (see
  * BasicLogger.write / GameModule.encode): a non-LOG root token carrying the
  * beginning-game state, followed by one top-level ESC-delimited token per
  * logged action. Each logged action is a `LogCommand` wrapper, and a whole
  * undo (UNDO true / inverse / UNDO false) is a single such token.
  */
class LogPrunerTest extends AnyFunSuite:

  private val Esc = CommandTree.CommandSeparator

  /** A logged action with no subcommands: `LOG\t<encoded command>`. */
  private def entry(cmd: Command): String = "LOG\t" + CommandCodec.encode(cmd)

  /** A whole undo action taking back one earlier action, whose inverse is
    * `reverse`: the single top-level token
    * `LOG\tUNDO\ttrue  ESC  LOG\t<reverse>  ESC  LOG\tUNDO\tfalse`.
    */
  private def undo(reverse: Command): String =
    SequenceCodec.encode(
      Seq("LOG\tUNDO\ttrue", "LOG\t" + CommandCodec.encode(reverse), "LOG\tUNDO\tfalse"),
      Esc
    )

  /** An undo that carries no decodable inverse payload (as real logs contain
    * for "null" container actions): `LOG\tUNDO\ttrue ESC LOG\tUNDO\tfalse`. */
  private def emptyUndo: String =
    SequenceCodec.encode(Seq("LOG\tUNDO\ttrue", "LOG\tUNDO\tfalse"), Esc)

  /** Join a beginning-state root and a series of logged tokens into a single
    * savedGame command string, exactly as GameModule.encode would. */
  private def stream(root: String, tokens: String*): String =
    SequenceCodec.encode(root +: tokens.toVector, Esc)

  // A representative non-LOG beginning-state root token.
  private val Root = CommandCodec.encode(AddPieceCmd("setup0", "stack", "state0"))

  private def moveOf(id: String, from: (Int, Int), to: (Int, Int)): MovePieceCmd =
    MovePieceCmd(id, Some("Map"), to._1, to._2, None, Some("Map"), from._1, from._2, None, Some("p"))

  private def undoLeaves(s: String): Int =
    CommandTree.flatten(s).count { case LogCmd(_: UndoCmd) => true; case _ => false }

  test("a log with no undo actions is returned byte-for-byte unchanged") {
    val a = entry(moveOf("p1", (0, 0), (1, 1)))
    val b = entry(DisplayTextCmd("* p1 moves *"))
    val c = entry(moveOf("p1", (1, 1), (2, 2)))
    val input = stream(Root, a, b, c)

    val result = LogPruner.prune(input)

    assert(result.commandString == input)
    assert(!result.changed)
    assert(result.removedUndoBlocks == 0)
    assert(result.removedUndoneCommands == 0)
  }

  test("a single undo removes both the undo action and the action it took back") {
    val a = entry(moveOf("p1", (0, 0), (1, 1)))
    val b = entry(moveOf("p1", (1, 1), (2, 2)))
    val input = stream(Root, a, b, undo(moveOf("p1", (2, 2), (1, 1))))

    val result = LogPruner.prune(input)

    assert(result.removedUndoBlocks == 1)
    assert(result.removedUndoneCommands == 1)
    // Only the beginning state and the first (still-standing) move remain.
    assert(result.commandString == stream(Root, a))
    assert(undoLeaves(result.commandString) == 0)
  }

  test("consecutive undos back out several actions in last-in-first-out order") {
    val a = entry(moveOf("p1", (0, 0), (1, 1)))
    val b = entry(moveOf("p2", (5, 5), (6, 6)))
    val c = entry(moveOf("p3", (9, 9), (8, 8)))
    val input = stream(
      Root, a, b, c,
      undo(moveOf("p3", (8, 8), (9, 9))), // undoes c
      undo(moveOf("p2", (6, 6), (5, 5))) // undoes b
    )

    val result = LogPruner.prune(input)

    assert(result.removedUndoBlocks == 2)
    assert(result.removedUndoneCommands == 2)
    assert(result.commandString == stream(Root, a))
  }

  test("an action logged after an undo is kept") {
    val a = entry(moveOf("p1", (0, 0), (1, 1)))
    val b = entry(moveOf("p1", (1, 1), (2, 2)))
    val d = entry(moveOf("p1", (1, 1), (3, 3)))
    val input = stream(Root, a, b, undo(moveOf("p1", (2, 2), (1, 1))), d)

    val result = LogPruner.prune(input)

    assert(result.removedUndoBlocks == 1)
    assert(result.commandString == stream(Root, a, d))
  }

  test("an undo skips over a non-undoable chat action and takes back the move") {
    val move = entry(moveOf("p1", (0, 0), (1, 1)))
    val chat = entry(DisplayTextCmd("* good luck *"))
    val input = stream(Root, move, chat, undo(moveOf("p1", (1, 1), (0, 0))))

    val result = LogPruner.prune(input)

    assert(result.removedUndoneCommands == 1)
    // The chat line is not undoable, so it survives; the move is removed.
    assert(result.commandString == stream(Root, chat))
  }

  test("an undo of an unrecognized container action is matched, not skipped") {
    // Real logs contain actions that serialize to the bare token "null"; VASSAL
    // still treats them as undoable. Missing one would misalign the pairing, so
    // the pruner must match the undo to it.
    val container = entry(UnknownCmd("null"))
    val move = entry(moveOf("p1", (0, 0), (1, 1)))
    val input = stream(
      Root, container, move,
      undo(moveOf("p1", (1, 1), (0, 0))), // undoes the move
      emptyUndo // undoes the container action
    )

    val result = LogPruner.prune(input)

    assert(result.removedUndoBlocks == 2)
    assert(result.removedUndoneCommands == 2)
    assert(result.unmatchedUndoBlocks == 0)
    assert(result.commandString == stream(Root))
  }

  test("an undo with nothing left to take back is dropped and flagged") {
    val input = stream(Root, emptyUndo)

    val result = LogPruner.prune(input)

    assert(result.removedUndoBlocks == 1)
    assert(result.removedUndoneCommands == 0)
    assert(result.unmatchedUndoBlocks == 1)
    assert(result.commandString == stream(Root))
  }

  test("non-LOG beginning-state tokens are always preserved") {
    // A setup token that looks undoable by content must still never be removed,
    // because it is part of the beginning state, not a logged action.
    val setupPiece = CommandCodec.encode(AddPieceCmd("setup1", "stack", "state1"))
    val move = entry(moveOf("p1", (0, 0), (1, 1)))
    val input = stream(Root, setupPiece, move, undo(moveOf("p1", (1, 1), (0, 0))))

    val result = LogPruner.prune(input)

    assert(result.commandString == stream(Root, setupPiece))
  }
