package vlog.prune

import vlog.command.*
import vlog.format.SequenceCodec

/** Removes "undone" commands from a .vlog command stream.
  *
  * While recording a logfile, VASSAL appends every player action to
  * BasicLogger.logOutput. When the player clicks Undo, BasicLogger.undo()
  * does NOT delete the action that is being taken back — it simply appends a
  * new entry that reverses it (a `UNDO\ttrue` marker, the inverse command,
  * and a closing `UNDO\tfalse` marker, all as one command tree). So a log in
  * which the player backed up a few steps and started again still carries all
  * the false starts plus their reversals, which makes replay slow and the log
  * hard to read.
  *
  * This pruner reproduces VASSAL's own undo bookkeeping to strip those pairs
  * out. The saved command string encodes the beginning-of-game state as the
  * root command, followed by one appended `LogCommand` per logged action
  * (BasicLogger.write). At the top level of the encoding each of those
  * appended actions — including a whole undo entry — is exactly one
  * ESC-delimited token (see VASSAL.build.GameModule.encode), so we can operate
  * one token at a time without disturbing the rest of the stream.
  *
  * The matching mirrors BasicLogger's `nextUndo` cursor: undoable actions form
  * a stack in the order they were logged, and each undo pops the most recent
  * one still on it (VASSAL scans backwards over logOutput to the previous
  * action whose getUndoCommand() is non-null). We therefore remove, for each
  * undo entry, both the undo entry itself and the undoable action it took
  * back. Because an undo reverses exactly its target and no other undoable
  * action can sit between them on the stack, deleting the pair leaves a stream
  * that replays to the same final game state, just without the detour.
  */
object LogPruner:

  private val Esc = CommandTree.CommandSeparator
  private val LogPrefix = "LOG\t"

  /** @param commandString the pruned savedGame command string
    * @param keptTokens number of top-level tokens retained (incl. setup)
    * @param removedUndoneCommands undoable actions that were taken back
    * @param removedUndoBlocks undo entries removed
    * @param unmatchedUndoBlocks undo entries that had nothing left to undo
    *   (dropped anyway); a non-zero value hints the log was unusual
    */
  case class Result(
      commandString: String,
      keptTokens: Int,
      removedUndoneCommands: Int,
      removedUndoBlocks: Int,
      unmatchedUndoBlocks: Int
  ):
    def changed: Boolean = removedUndoBlocks > 0

  private enum Kind:
    case Undo // a `UNDO\ttrue`-rooted entry that takes back an earlier action
    case Undoable // a logged action VASSAL could undo (sets nextUndo)
    case Other // setup / beginning state, or a non-undoable action (e.g. chat)

  /** Whether a single decoded command can make its enclosing action
    * "undoable" (advance BasicLogger.nextUndo), i.e. its myUndoCommand() is
    * non-null in VASSAL.
    *
    *  - Add/Remove/Move always qualify; a ChangePiece only when it carries an
    *    old state to restore (VASSAL.command.ChangePiece.myUndoCommand).
    *  - An [[UnknownCmd]] is a command no encoder here recognizes — most often
    *    the "null" container VASSAL emits for an action whose real, undoable
    *    payload rides in its subcommands (and, as seen in real logs, sometimes
    *    an action that serializes to nothing decodable yet was still undone at
    *    runtime). We must count these as undoable: missing one would shift our
    *    undo-to-action pairing and take back the wrong action. Over-counting a
    *    genuinely non-undoable unknown is only a risk if one sits between an
    *    action and its undo, which does not happen for these container types.
    *  - Chat, audio and null commands are never undoable, so a logged action
    *    made only of those (e.g. a line typed in the chat window) is correctly
    *    skipped by VASSAL's undo and must be skipped here too.
    */
  private def isUndoable(c: Command): Boolean = c match
    case _: AddPieceCmd | _: RemovePieceCmd | _: MovePieceCmd => true
    case ChangePieceCmd(_, _, oldState) => oldState.isDefined
    case _: UnknownCmd => true
    case _ => false

  private def classify(logToken: String): Kind =
    val head = SequenceCodec.decode(logToken, Esc).headOption.getOrElse("")
    if !head.startsWith(LogPrefix) then Kind.Other
    else
      CommandCodec.decode(head) match
        case LogCmd(UndoCmd(true)) => Kind.Undo
        case _ =>
          // Undoability depends on the whole subtree, not just the root: the
          // logged action's root is often an unrecognized container ("null")
          // whose real, undoable content sits in its subcommands.
          val undoable = CommandTree.flatten(logToken).exists {
            case LogCmd(c) => isUndoable(c)
            case _ => false
          }
          if undoable then Kind.Undoable else Kind.Other

  def prune(commandString: String): Result =
    // Two views of the same top-level split, sharing token boundaries:
    //  - rawTokens: verbatim substrings, used to rebuild the output so every
    //    untouched token stays byte-for-byte identical.
    //  - logTokens: one-level-decoded tokens, used only for classification.
    val rawTokens = SequenceCodec.rawSplit(commandString, Esc)
    val logTokens = SequenceCodec.decode(commandString, Esc)

    val keep = Array.fill(rawTokens.size)(true)
    val undoStack = scala.collection.mutable.Stack[Int]()
    var removedUndone = 0
    var removedUndo = 0
    var unmatched = 0

    logTokens.zipWithIndex.foreach { case (token, i) =>
      classify(token) match
        case Kind.Undo =>
          keep(i) = false
          removedUndo += 1
          if undoStack.nonEmpty then
            keep(undoStack.pop()) = false
            removedUndone += 1
          else
            unmatched += 1
        case Kind.Undoable =>
          undoStack.push(i)
        case Kind.Other =>
          ()
    }

    val kept = rawTokens.zipWithIndex.collect { case (t, i) if keep(i) => t }
    Result(
      commandString = kept.mkString(Esc.toString),
      keptTokens = kept.size,
      removedUndoneCommands = removedUndone,
      removedUndoBlocks = removedUndo,
      unmatchedUndoBlocks = unmatched
    )
