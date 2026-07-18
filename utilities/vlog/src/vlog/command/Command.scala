package vlog.command

/** Model of the VASSAL `Command` hierarchy as it appears in the flattened
  * command stream of a .vlog file. Each case class corresponds to one
  * concrete Command subclass encoded by VASSAL.build.module.BasicCommandEncoder,
  * VASSAL.build.module.Chatter, or VASSAL.build.module.BasicLogger.
  */
sealed trait Command

/** VASSAL.command.AddPiece — prefix "+/" (BasicCommandEncoder.ADD).
  * Adds a piece to a map/deck.
  * @param id the GamePiece id
  * @param pieceType the piece type definition string (itself tab-delimited)
  * @param state the piece's encoded state string
  */
case class AddPieceCmd(id: String, pieceType: String, state: String) extends Command

/** VASSAL.command.RemovePiece — prefix "-/" (BasicCommandEncoder.REMOVE).
  * The remainder of the token is the raw piece id with no further delimiting.
  */
case class RemovePieceCmd(id: String) extends Command

/** VASSAL.command.ChangePiece — prefix "D/" (BasicCommandEncoder.CHANGE).
  * @param oldState absent when the command carries no undo state
  */
case class ChangePieceCmd(id: String, newState: String, oldState: Option[String]) extends Command

/** VASSAL.command.MovePiece — prefix "M/" (BasicCommandEncoder.MOVE).
  * Map/underneath-piece ids are optional (VASSAL's "null" sentinel).
  */
case class MovePieceCmd(
    id: String,
    newMapId: Option[String],
    newX: Int,
    newY: Int,
    newUnderId: Option[String],
    oldMapId: Option[String],
    oldX: Int,
    oldY: Int,
    oldUnderId: Option[String],
    playerId: Option[String]
) extends Command

/** VASSAL.command.NullCommand — encodes to the empty string. */
case object NullCmd extends Command

/** Chatter.DisplayText (chat/log message) — prefix "CHAT". */
case class DisplayTextCmd(message: String) extends Command

/** BasicLogger's LOG wrapper — prefix "LOG\t". Wraps whatever single
  * command was actually performed while logging was active.
  */
case class LogCmd(inner: Command) extends Command

/** BasicLogger's UNDO wrapper — prefix "UNDO\t". */
case class UndoCmd(inProgress: Boolean) extends Command

/** VASSAL.command.PlayAudioClipCommand — prefix "AUDIO\t". */
case class PlayAudioClipCmd(clip: String) extends Command

/** Any command token whose prefix isn't one of the ones modeled above
  * (e.g. commands registered by other modules/extensions). Preserves the
  * raw token so no information is lost.
  */
case class UnknownCmd(raw: String) extends Command
