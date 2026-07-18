package vlog.command

import org.scalatest.funsuite.AnyFunSuite

class CommandCodecTest extends AnyFunSuite:

  test("NullCommand round-trips as an empty token") {
    assert(CommandCodec.decode("") == NullCmd)
    assert(CommandCodec.encode(NullCmd) == "")
  }

  test("AddPiece round-trips") {
    val cmd = AddPieceCmd(id = "piece123", pieceType = "Marker\ttype", state = "state;data")
    val token = CommandCodec.encode(cmd)
    assert(token.startsWith("+/"))
    assert(CommandCodec.decode(token) == cmd)
  }

  test("RemovePiece round-trips") {
    val cmd = RemovePieceCmd(id = "piece123")
    val token = CommandCodec.encode(cmd)
    assert(token == "-/piece123")
    assert(CommandCodec.decode(token) == cmd)
  }

  test("ChangePiece round-trips with and without oldState") {
    val withOld = ChangePieceCmd(id = "p1", newState = "new", oldState = Some("old"))
    assert(CommandCodec.decode(CommandCodec.encode(withOld)) == withOld)

    val withoutOld = ChangePieceCmd(id = "p1", newState = "new", oldState = None)
    assert(CommandCodec.decode(CommandCodec.encode(withoutOld)) == withoutOld)
  }

  test("MovePiece round-trips with null map/under ids and no playerId") {
    val cmd = MovePieceCmd(
      id = "p1",
      newMapId = None,
      newX = 10,
      newY = 20,
      newUnderId = None,
      oldMapId = Some("map1"),
      oldX = 1,
      oldY = 2,
      oldUnderId = Some("p0"),
      playerId = None
    )
    assert(CommandCodec.decode(CommandCodec.encode(cmd)) == cmd)
  }

  test("MovePiece round-trips with playerId present") {
    val cmd = MovePieceCmd(
      id = "p1",
      newMapId = Some("map1"),
      newX = 10,
      newY = 20,
      newUnderId = Some("p2"),
      oldMapId = Some("map1"),
      oldX = 1,
      oldY = 2,
      oldUnderId = None,
      playerId = Some("user@example.com")
    )
    assert(CommandCodec.decode(CommandCodec.encode(cmd)) == cmd)
  }

  test("DisplayText round-trips") {
    val cmd = DisplayTextCmd(message = "* Alice moves a piece")
    assert(CommandCodec.decode(CommandCodec.encode(cmd)) == cmd)
  }

  test("Log wrapper round-trips around an inner command") {
    val cmd = LogCmd(RemovePieceCmd(id = "p1"))
    val token = CommandCodec.encode(cmd)
    assert(token.startsWith("LOG\t"))
    assert(CommandCodec.decode(token) == cmd)
  }

  test("Undo wrapper round-trips") {
    assert(CommandCodec.decode(CommandCodec.encode(UndoCmd(true))) == UndoCmd(true))
    assert(CommandCodec.decode(CommandCodec.encode(UndoCmd(false))) == UndoCmd(false))
  }

  test("PlayAudioClip round-trips") {
    val cmd = PlayAudioClipCmd(clip = "explosion.wav")
    assert(CommandCodec.decode(CommandCodec.encode(cmd)) == cmd)
  }

  test("Unknown prefixes are preserved verbatim") {
    val raw = "ZZZ/something/unrecognized"
    assert(CommandCodec.decode(raw) == UnknownCmd(raw))
    assert(CommandCodec.encode(UnknownCmd(raw)) == raw)
  }
