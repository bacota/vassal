package vassal.core.command

/** Cross-platform port of `VASSAL.command.CommandEncoder`: translates a
  * single (non-compound) [[Command]] to and from its ascii wire-format
  * string. Each encoder only needs to recognize its own command type(s);
  * see [[CommandDispatcher]] for how a module's full set of encoders is
  * tried in turn.
  */
trait CommandEncoder {
  /** Attempts to decode `command` as this encoder's command type.
    * @return `None` if this encoder doesn't recognize `command`.
    */
  def decode(command: String): Option[Command]

  /** Attempts to encode `c` as this encoder's command type.
    * @return `None` if this encoder doesn't handle `c`'s type.
    */
  def encode(c: Command): Option[String]
}

/** `NullCommand` <-> `""`, per `VASSAL.build.module.BasicCommandEncoder`. */
object NullCommandEncoder extends CommandEncoder {
  def decode(command: String): Option[Command] =
    if (command.isEmpty) Some(new NullCommand) else None

  def encode(c: Command): Option[String] =
    c match {
      case _: NullCommand => Some("")
      case _              => None
    }
}
