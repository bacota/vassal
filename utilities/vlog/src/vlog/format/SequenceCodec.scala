package vlog.format

import java.lang.{StringBuilder => JStringBuilder}
import java.util.NoSuchElementException

/** Faithful reimplementation of VASSAL.tools.SequenceEncoder /
  * SequenceEncoder.Decoder — ported field-for-field from that class rather
  * than inferred, since the exact escaping rules matter byte-for-byte:
  *
  *   - Only the delimiter character itself is ever escaped, by inserting a
  *     literal backslash immediately before it. A literal backslash in the
  *     source data is NOT escaped on its own.
  *   - If a field's first character is a backslash, or the field is already
  *     wrapped in a matching pair of single quotes, the whole (delimiter-escaped)
  *     field is additionally wrapped in a single-quote pair so the decoder can
  *     tell it apart from an accidental leading backslash.
  *   - On decode, "\<delim>" is unescaped back to a literal delimiter
  *     character (the backslash is dropped, the delimiter char is kept); any
  *     other use of backslash is left untouched. The outer single-quote
  *     wrapping, if present, is then stripped.
  */
object SequenceCodec:

  def encode(fields: Seq[String], delimiter: Char): String =
    fields.map(escapeField(_, delimiter)).mkString(delimiter.toString)

  private def escapeField(field: String, delimiter: Char): String =
    if field.isEmpty then ""
    else
      val escaped = insertEscapes(field, delimiter)
      val needsQuote = field.charAt(0) == '\\' ||
        (field.charAt(0) == '\'' && field.charAt(field.length - 1) == '\'')
      if needsQuote then s"'$escaped'" else escaped

  private def insertEscapes(field: String, delimiter: Char): String =
    val sb = new JStringBuilder
    for c <- field do
      if c == delimiter then sb.append('\\')
      sb.append(c)
    sb.toString

  /** Splits a whole delimited string into all of its tokens, mirroring
    * repeated calls to SequenceEncoder.Decoder#nextToken() until exhausted.
    */
  def decode(s: String, delimiter: Char): Vector[String] =
    val decoder = new Decoder(s, delimiter)
    val tokens = Vector.newBuilder[String]
    while decoder.hasMoreTokens do
      tokens += decoder.nextToken()
    tokens.result()

  /** Direct port of VASSAL.tools.SequenceEncoder.Decoder, for callers (like
    * command decoding) that need to consume tokens one at a time, e.g. to
    * treat a trailing token as optional.
    */
  final class Decoder(initialValue: String, delimiter: Char):
    private var value: String = initialValue
    private var start = 0
    private val stop = if initialValue != null then initialValue.length else 0

    def hasMoreTokens: Boolean = value != null

    def nextToken(): String =
      if !hasMoreTokens then throw new NoSuchElementException()

      if start == stop then
        value = null
        return ""

      var buf: JStringBuilder = null
      var tok: String = null
      var i = start
      var foundDelim = false

      while i < stop && !foundDelim do
        if value.charAt(i) == delimiter then
          if i > 0 && value.charAt(i - 1) == '\\' then
            if buf == null then buf = new JStringBuilder()
            buf.append(value, start, i - 1)
            start = i
          else
            if buf == null || buf.length == 0 then
              tok = value.substring(start, i)
            else
              buf.append(value, start, i)
            start = i + 1
            foundDelim = true
        if !foundDelim then i += 1

      if !foundDelim then
        // reached the end without an unescaped delimiter
        if buf == null || buf.length == 0 then
          tok = value.substring(start)
        else
          buf.append(value, start, stop)
        value = null

      unquote(if tok != null then tok else buf.toString)

    private def unquote(s: String): String =
      if s.length > 1 && s.charAt(0) == '\'' && s.charAt(s.length - 1) == '\'' then
        s.substring(1, s.length - 1)
      else s
