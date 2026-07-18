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
      // Mirror GameModule.decode exactly: pull off the first token and treat
      // the string as a genuine leaf only when that token IS the whole string.
      // If it isn't, the string is a nested subtree — extracting `first`
      // unescaped its inner ESC delimiters, which must now be flattened
      // recursively. (Testing `first == raw` rather than the token count
      // matters when a leaf's data contained an escaped ESC but no unescaped
      // one: that decodes to a single token that is still not the raw string.)
      val decoder = new SequenceCodec.Decoder(raw, CommandSeparator)
      val first = decoder.nextToken()
      if first == raw then Vector(CommandCodec.decode(raw))
      else
        val tokens = Vector.newBuilder[String]
        tokens += first
        while decoder.hasMoreTokens do tokens += decoder.nextToken()
        tokens.result().flatMap(flatten)
