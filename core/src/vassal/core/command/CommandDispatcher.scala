package vassal.core.command

/** Cross-platform port of the encode/decode dispatch logic in
  * `VASSAL.build.GameModule` (which itself implements `CommandEncoder` as a
  * dispatcher over every [[CommandEncoder]] registered with the module).
  * Splits/joins compound command strings on `commandSeparator` (ASCII ESC in
  * the legacy app) and tries each registered encoder in turn for each
  * single sub-command.
  *
  * @param encoders tried in order; the first to recognize a sub-command wins,
  *                 exactly as `GameModule.commandEncoders` is walked in order.
  */
final class CommandDispatcher(encoders: Vector[CommandEncoder], commandSeparator: Char = 27.toChar) {

  private def decodeSubCommand(subCommand: String): Option[Command] =
    encoders.iterator.map(_.decode(subCommand)).collectFirst { case Some(c) => c }

  /** Mirrors `GameModule.decode(String)`. */
  def decode(command: String): Option[Command] = {
    if (command == null) return None

    val st = new SequenceEncoder.Decoder(command, commandSeparator)
    val first = st.nextToken()

    if (command == first) {
      decodeSubCommand(first)
    }
    else {
      var c = decode(first)
      while (st.hasMoreTokens) {
        val next = decode(st.nextToken())
        c = (c, next) match {
          case (None, n)          => n
          case (Some(cmd), None)  => Some(cmd)
          case (Some(cmd), Some(n)) => Some(cmd.append(n))
        }
      }
      c
    }
  }

  private def encodeSubCommand(c: Command): Option[String] =
    encoders.iterator.map(_.encode(c)).collectFirst { case Some(s) => s }

  /** Mirrors `GameModule.encode(Command)`. */
  def encode(c: Command): Option[String] = {
    if (c == null) return None

    val s = encodeSubCommand(c)
    val sub = c.subCommands
    if (sub.isEmpty) {
      s
    }
    else {
      val se = new SequenceEncoder(s.orNull, commandSeparator)
      for (command <- sub) {
        encode(command).foreach(se.append)
      }
      Option(se.getValue)
    }
  }
}
