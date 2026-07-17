package vlog.format

/** Reimplementation of VASSAL's VASSAL.tools.SequenceEncoder /
  * SequenceEncoder.Decoder delimiter-separated field format.
  *
  * Encoding rule per field:
  *   - if the field contains the delimiter, a backslash, or a single quote,
  *     the delimiter is escaped as "\<delim>" and a literal backslash is
  *     escaped as "\\"; fields are then joined by the bare delimiter.
  *
  * Decoding rule: scan for the delimiter, treating "\<char>" as an escaped
  * literal <char> rather than a field terminator.
  */
object SequenceCodec:

  def encode(fields: Seq[String], delimiter: Char): String =
    fields.map(escape(_, delimiter)).mkString(delimiter.toString)

  private def escape(field: String, delimiter: Char): String =
    val sb = new StringBuilder
    for c <- field do
      if c == delimiter || c == '\\' then sb.append('\\')
      sb.append(c)
    sb.toString

  def decode(s: String, delimiter: Char): Vector[String] =
    val tokens = Vector.newBuilder[String]
    val current = new StringBuilder
    var i = 0
    while i < s.length do
      val c = s.charAt(i)
      if c == '\\' && i + 1 < s.length then
        current.append(s.charAt(i + 1))
        i += 2
      else if c == delimiter then
        tokens += current.toString
        current.clear()
        i += 1
      else
        current.append(c)
        i += 1
    tokens += current.toString
    tokens.result()
