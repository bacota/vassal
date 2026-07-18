package vassal.core.command

class CommandTest extends munit.FunSuite {

  test("NullCommand is null and loggable is false") {
    val c = new NullCommand
    assertEquals(c.isNull, true)
    assertEquals(c.isLoggable, false)
  }

  test("appending a null-ish command is a true no-op, not even recorded") {
    val c = new NullCommand
    val result = c.append(new NullCommand)
    assertEquals(result, c)
    assertEquals(c.subCommands.length, 0)
  }

  test("appending a non-null command to a null root promotes the child") {
    val root = new NullCommand
    val child = new RecordingCommand("child")
    val result = root.append(child)
    assertEquals(result, child)
    assertEquals(root.subCommands, Vector(child))
  }

  test("appending to a non-null command keeps it as the root") {
    val root = new RecordingCommand("root")
    val child = new RecordingCommand("child")
    val result = root.append(child)
    assertEquals(result, root)
    assertEquals(root.subCommands, Vector(child))
  }

  test("appending two non-null children to a null root only promotes on the first") {
    // Regression test: isNull must be evaluated *before* the subcommand is
    // recorded, since NullCommand.isNull depends on the (about to change)
    // subcommand list. Getting this backwards makes the second append below
    // wrongly return `root` instead of `first`.
    val root = new NullCommand
    val first = new RecordingCommand("first")
    val afterFirst = root.append(first)
    assertEquals(afterFirst, first)

    val second = new RecordingCommand("second")
    val afterSecond = root.append(second)
    assertEquals(afterSecond, root, "root is no longer null-ish after the first append, so it keeps ownership")
    assertEquals(root.subCommands, Vector(first, second))
  }

  test("toString includes details and subcommands") {
    val root = new RecordingCommand("root")
    root.append(new RecordingCommand("child"))
    assertEquals(root.toString, "RecordingCommand[root]+RecordingCommand[child]")
  }
}

private final class RecordingCommand(label: String) extends Command {
  override def details: Option[String] = Some(label)
}
