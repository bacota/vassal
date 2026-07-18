package vlog.moves

import vlog.command.*
import vlog.format.SequenceCodec

/** One piece movement recovered from a .vlog: which piece(s) moved, and the
  * From / To locations as VASSAL itself recorded them.
  *
  * @param piece   human-readable description of what moved, taken from the move
  *                report (e.g. "Subervie, L'Heritier"); falls back to the raw
  *                piece id(s) when the module logged no report
  * @param from    origin location name (e.g. "GG17"), if a report was present
  * @param to      destination location name (e.g. "FF18"), if a report was present
  * @param pieceIds the moved pieces' game ids (always available)
  * @param fromXY  origin in map pixel coordinates (always available)
  * @param toXY    destination in map pixel coordinates (always available)
  * @param fromMap origin map id; differs from `toMap` on cross-map moves
  * @param toMap   destination map id
  */
case class Move(
    piece: String,
    from: Option[String],
    to: Option[String],
    pieceIds: Vector[String],
    fromXY: (Int, Int),
    toXY: (Int, Int),
    fromMap: Option[String],
    toMap: Option[String]
)

/** Extracts the moves recorded in a .vlog command stream.
  *
  * VASSAL never stores a hex/area id in a move command — a MovePiece carries
  * only pixel coordinates. But when a module has a move report format (the
  * common case for hex/area games) it logs a chat line alongside the move,
  * e.g. `* Subervie moves GG17 &rarr; FF18 *`, which names the piece and both
  * ends of the move. This extractor pairs each logged move action with that
  * report, so the From/To it returns are the module's own labels — correct
  * even for modules whose grids use custom numbering that we could not
  * recompute from geometry.
  *
  * Each top-level token in the stream is one logged action (see
  * [[vlog.prune.LogPruner]]); an action that contains one or more MovePiece
  * commands becomes one [[Move]], with the report (if any) supplying the piece
  * name and the location labels. Undo actions are skipped, so reversed moves
  * are not reported as real ones.
  */
object MoveExtractor:

  private val Esc = CommandTree.CommandSeparator

  def extract(commandString: String): Vector[Move] =
    if commandString == null then Vector.empty
    else SequenceCodec.decode(commandString, Esc).flatMap(actionToMove)

  private def actionToMove(token: String): Option[Move] =
    val head = SequenceCodec.decode(token, Esc).headOption.getOrElse("")
    CommandCodec.decode(head) match
      case LogCmd(UndoCmd(true)) => None // an undo action, not a real move
      case _ =>
        val leaves = CommandTree.flatten(token).map {
          case LogCmd(c) => c
          case c => c
        }
        val moves = leaves.collect { case m: MovePieceCmd => m }
        if moves.isEmpty then None
        else
          val report = leaves
            .collect { case DisplayTextCmd(text) => text }
            .flatMap(MoveReport.parse)
            .headOption
          val primary = moves.head
          Some(
            Move(
              piece = report.map(_.piece).filter(_.nonEmpty)
                .getOrElse(moves.map(_.id).mkString(", ")),
              from = report.map(_.from),
              to = report.map(_.to),
              pieceIds = moves.map(_.id),
              fromXY = (primary.oldX, primary.oldY),
              toXY = (primary.newX, primary.newY),
              fromMap = primary.oldMapId,
              toMap = primary.newMapId
            )
          )
