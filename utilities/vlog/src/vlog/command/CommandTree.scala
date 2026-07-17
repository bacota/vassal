package vlog.command

import vlog.format.SequenceCodec

/** Reconstructs the ordered list of leaf [[Command]]s from the raw,
  * recursively-encoded command string found in a .vlog file's savedGame
  * entry.
  *
  * VASSAL.build.GameModule.encode(Command) (GameModule.java:1565-1583)
  * flattens a Command tree by ESC-joining a node's own encoded string with
  * the (recursively encoded) strings of its subcommands via
  * `SequenceEncoder.append`. Critically, `append` always escapes any ESC
  * characters already present in what it's appending — so a child that
  * itself had further subcommands (and so already contains literal,
  * unescaped ESC delimiters from its own recursive encoding) gets those
  * delimiters escaped *again* by the parent. The result is that splitting
  * the whole string on ESC just once is not enough to recover the leaves:
  * a resulting token can still contain further (now-unescaped) literal ESC
  * characters, meaning it is itself a nested subtree that must be split
  * again.
  *
  * `GameModule.decode(String)` (GameModule.java:1520-1540) mirrors this: it
  * only treats a string as a genuine single leaf command (dispatching to
  * the registered CommandEncoders) when splitting it finds just one token
  * identical to the whole string; otherwise it recurses on every token.
  */
object CommandTree:

  /** VASSAL's GameModule.COMMAND_SEPARATOR: java.awt.event.KeyEvent.VK_ESCAPE. */
  val CommandSeparator: Char = 0x1B.toChar

  def flatten(raw: String): Vector[Command] =
    if raw == null then Vector.empty
    else
      val tokens = SequenceCodec.decode(raw, CommandSeparator)
      if tokens.size <= 1 then Vector(CommandCodec.decode(raw))
      else tokens.flatMap(flatten)
