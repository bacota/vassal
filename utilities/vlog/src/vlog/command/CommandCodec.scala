package vlog.command

import vlog.format.SequenceCodec

/** Encodes/decodes single command tokens (as split from the ESC-delimited
  * savedGame command stream) into/from [[Command]] values, mirroring
  * VASSAL.build.module.BasicCommandEncoder, Chatter, and BasicLogger.
  */
object CommandCodec:

  /** Field separator used within a BasicCommandEncoder command
    * (BasicCommandEncoder.PARAM_SEPARATOR).
    */
  private val FieldSep = '/'

  private val LogPrefix = "LOG\t"
  private val UndoPrefix = "UNDO\t"
  private val ChatPrefix = "CHAT"
  private val AddPrefix = "+/"
  private val RemovePrefix = "-/"
  private val ChangePrefix = "D/"
  private val MovePrefix = "M/"
  private val AudioPrefix = "AUDIO\t"

  private val NullSentinel = "null"

  private def wrapNull(value: Option[String]): String = value.getOrElse(NullSentinel)
  private def unwrapNull(value: String): Option[String] =
    if value == NullSentinel then None else Some(value)

  def decode(token: String): Command =
    if token.isEmpty then NullCmd
    else if token.startsWith(LogPrefix) then
      LogCmd(decode(token.substring(LogPrefix.length)))
    else if token.startsWith(UndoPrefix) then
      UndoCmd(token.substring(UndoPrefix.length) == "true")
    else if token.startsWith(ChatPrefix) then
      DisplayTextCmd(token.substring(ChatPrefix.length))
    else if token.startsWith(AudioPrefix) then
      PlayAudioClipCmd(token.substring(AudioPrefix.length))
    else if token.startsWith(AddPrefix) then
      SequenceCodec.decode(token.substring(AddPrefix.length), FieldSep) match
        case Vector(id, pieceType, state) =>
          AddPieceCmd(id = id, pieceType = pieceType, state = state)
        case _ =>
          UnknownCmd(token)
    else if token.startsWith(RemovePrefix) then
      RemovePieceCmd(id = token.substring(RemovePrefix.length))
    else if token.startsWith(ChangePrefix) then
      val fields = SequenceCodec.decode(token.substring(ChangePrefix.length), FieldSep)
      if fields.length < 2 then UnknownCmd(token)
      else
        ChangePieceCmd(
          id = fields(0),
          newState = fields(1),
          oldState = fields.lift(2)
        )
    else if token.startsWith(MovePrefix) then
      val fields = SequenceCodec.decode(token.substring(MovePrefix.length), FieldSep)
      MovePieceCmd(
        id = fields(0),
        newMapId = unwrapNull(fields(1)),
        newX = fields(2).toInt,
        newY = fields(3).toInt,
        newUnderId = unwrapNull(fields(4)),
        oldMapId = unwrapNull(fields(5)),
        oldX = fields(6).toInt,
        oldY = fields(7).toInt,
        oldUnderId = unwrapNull(fields(8)),
        playerId = fields.lift(9).flatMap(unwrapNull)
      )
    else
      UnknownCmd(token)

  def encode(command: Command): String = command match
    case NullCmd => ""
    case LogCmd(inner) => LogPrefix + encode(inner)
    case UndoCmd(inProgress) => UndoPrefix + inProgress.toString
    case DisplayTextCmd(message) => ChatPrefix + message
    case PlayAudioClipCmd(clip) => AudioPrefix + clip
    case AddPieceCmd(id, pieceType, state) =>
      AddPrefix + SequenceCodec.encode(Seq(id, pieceType, state), FieldSep)
    case RemovePieceCmd(id) =>
      RemovePrefix + id
    case ChangePieceCmd(id, newState, oldState) =>
      val fields = Seq(id, newState) ++ oldState.toSeq
      ChangePrefix + SequenceCodec.encode(fields, FieldSep)
    case MovePieceCmd(id, newMapId, newX, newY, newUnderId, oldMapId, oldX, oldY, oldUnderId, playerId) =>
      val fields = Seq(
        id,
        wrapNull(newMapId),
        newX.toString,
        newY.toString,
        wrapNull(newUnderId),
        wrapNull(oldMapId),
        oldX.toString,
        oldY.toString,
        wrapNull(oldUnderId)
      ) ++ playerId.toSeq
      MovePrefix + SequenceCodec.encode(fields, FieldSep)
    case UnknownCmd(raw) => raw
