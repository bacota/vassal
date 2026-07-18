package vassal.core.command

class CommandDispatcherTest extends munit.FunSuite {

  test("NullCommand round-trips through the dispatcher") {
    val dispatcher = new CommandDispatcher(Vector(NullCommandEncoder))
    val encoded = dispatcher.encode(new NullCommand)
    assertEquals(encoded, Some(""))

    val decoded = dispatcher.decode(encoded.get)
    assert(decoded.isDefined)
    assert(decoded.get.isNull)
  }

  test("decode(null) and encode(null) both yield None") {
    val dispatcher = new CommandDispatcher(Vector(NullCommandEncoder))
    assertEquals(dispatcher.decode(null), None)
    assertEquals(dispatcher.encode(null), None)
  }

  test("an unrecognized command string decodes to None") {
    val dispatcher = new CommandDispatcher(Vector(NullCommandEncoder))
    assertEquals(dispatcher.decode("not empty and not recognized"), None)
  }

  test("multiple top-level commands joined by the separator all decode") {
    val recording = new RecordingCommandEncoder
    val dispatcher = new CommandDispatcher(Vector(NullCommandEncoder, recording), commandSeparator = ',')

    val joined = "" + ',' + "tag:hello" + ',' + "tag:world"
    val decoded = dispatcher.decode(joined)

    assert(decoded.isDefined)
    // root is the NullCommand (promoted away since it's null), so the first
    // real command becomes the root, with the rest appended as subcommands.
    assertEquals(recording.decodedTags.toVector, Vector("hello", "world"))
  }

  test("encoding a compound command re-joins subcommand encodings with the separator") {
    val recording = new RecordingCommandEncoder
    val dispatcher = new CommandDispatcher(Vector(recording), commandSeparator = ',')

    val root = new TaggedCommand("a")
    root.append(new TaggedCommand("b"))
    root.append(new TaggedCommand("c"))

    val encoded = dispatcher.encode(root)
    assertEquals(encoded, Some("tag:a,tag:b,tag:c"))

    val decoded = dispatcher.decode(encoded.get)
    assertEquals(recording.decodedTags.toVector, Vector("a", "b", "c"))
  }
}

private final class TaggedCommand(val tag: String) extends Command

/** A minimal stand-in for a real per-trait CommandEncoder like BasicCommandEncoder,
  * using a "tag:" prefix instead of any real VASSAL wire format.
  */
private final class RecordingCommandEncoder extends CommandEncoder {
  val decodedTags = collection.mutable.ArrayBuffer.empty[String]

  def decode(command: String): Option[Command] =
    if (command.startsWith("tag:")) {
      val tag = command.stripPrefix("tag:")
      decodedTags += tag
      Some(new TaggedCommand(tag))
    }
    else None

  def encode(c: Command): Option[String] =
    c match {
      case t: TaggedCommand => Some(s"tag:${t.tag}")
      case _                => None
    }
}
