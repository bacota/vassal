package vlog.moves

import org.scalatest.funsuite.AnyFunSuite
import vlog.command.*
import vlog.format.SequenceCodec

/** Tests for [[MoveExtractor]]. Fixtures reproduce the on-disk shape: a non-LOG
  * beginning-state root followed by one top-level ESC token per logged action,
  * where a move action wraps its MovePiece command(s) and its report line.
  */
class MoveExtractorTest extends AnyFunSuite:

  private val Esc = CommandTree.CommandSeparator

  private def logged(cmd: Command): String = "LOG\t" + CommandCodec.encode(cmd)

  /** One logged action: the given commands wrapped as a LOG subtree token. */
  private def action(cmds: Command*): String =
    SequenceCodec.encode(cmds.map(logged), Esc)

  private def undoAction(reverse: Command): String =
    SequenceCodec.encode(
      Seq("LOG\tUNDO\ttrue", logged(reverse), "LOG\tUNDO\tfalse"),
      Esc
    )

  private def stream(tokens: String*): String =
    SequenceCodec.encode(
      CommandCodec.encode(AddPieceCmd("setup", "stack", "s")) +: tokens.toVector,
      Esc
    )

  private def move(id: String, from: (Int, Int), to: (Int, Int),
      fromMap: String = "Main Map", toMap: String = "Main Map"): MovePieceCmd =
    MovePieceCmd(id, Some(toMap), to._1, to._2, None, Some(fromMap), from._1, from._2, None, Some("p"))

  test("extracts a move's piece, From and To from the report") {
    val input = stream(
      action(move("p1", (10, 20), (30, 40)), DisplayTextCmd("* Guard moves A1 &rarr; B2 *"))
    )
    val moves = MoveExtractor.extract(input)
    assert(moves.size == 1)
    val m = moves.head
    assert(m.piece == "Guard")
    assert(m.from.contains("A1"))
    assert(m.to.contains("B2"))
    assert(m.pieceIds == Vector("p1"))
    assert(m.fromXY == (10, 20) && m.toXY == (30, 40))
  }

  test("a stack move with several pieces and one report is a single record") {
    val input = stream(
      action(
        move("p1", (10, 20), (30, 40)),
        move("p2", (10, 20), (30, 40)),
        DisplayTextCmd("* Alpha, Bravo moves A1 &rarr; B2 *")
      )
    )
    val m = MoveExtractor.extract(input).head
    assert(m.piece == "Alpha, Bravo")
    assert(m.pieceIds == Vector("p1", "p2"))
    assert(m.from.contains("A1") && m.to.contains("B2"))
  }

  test("a move with no report falls back to piece id and unknown locations") {
    val input = stream(action(move("marker7", (5, 5), (5, 9))))
    val m = MoveExtractor.extract(input).head
    assert(m.piece == "marker7")
    assert(m.from.isEmpty && m.to.isEmpty)
    assert(m.fromXY == (5, 5) && m.toXY == (5, 9))
  }

  test("undo actions are not reported as moves") {
    val input = stream(
      action(move("p1", (10, 20), (30, 40)), DisplayTextCmd("* Guard moves A1 &rarr; B2 *")),
      undoAction(move("p1", (30, 40), (10, 20)))
    )
    val moves = MoveExtractor.extract(input)
    assert(moves.size == 1)
    assert(moves.head.to.contains("B2"))
  }

  test("cross-map moves keep distinct From/To map ids") {
    val input = stream(
      action(
        move("p1", (1, 1), (2, 2), fromMap = "Reserve", toMap = "Board"),
        DisplayTextCmd("* Unit moves Pool &rarr; C3 *")
      )
    )
    val m = MoveExtractor.extract(input).head
    assert(m.fromMap.contains("Reserve") && m.toMap.contains("Board"))
    assert(m.from.contains("Pool") && m.to.contains("C3"))
  }

  test("non-move actions produce no records") {
    val input = stream(action(DisplayTextCmd("* Alice rolls 2d6 = 7 *")))
    assert(MoveExtractor.extract(input).isEmpty)
  }
