package vassal.core.command

/** Cross-platform port of `VASSAL.tools.SequenceEncoder`: encodes a sequence
  * of strings into one delimiter-joined string, escaping the delimiter (and
  * quote-wrapping strings that start with a backslash or are already
  * quote-wrapped) so it can be told apart from delimiter characters that
  * occur inside an element. This is the low-level wire format underlying
  * essentially every `Command`/`CommandEncoder` in VASSAL, so any
  * discrepancy here breaks save/log/module compatibility.
  *
  * The legacy class has a separate "ugly delimiter" fast path for numeric
  * appends (`append(Int)` etc.) that skips escaping when the delimiter can't
  * possibly appear in a numeric literal's textual form. That's a
  * performance-only optimization -- the *output* is identical either way --
  * so it's omitted here: every append routes through the same string-append
  * logic for one, easier-to-verify code path.
  */
final class SequenceEncoder(delim: Char) {
  private var buffer: StringBuilder = null

  def this(value: String, delimiter: Char) = {
    this(delimiter)
    append(value)
  }

  private def startBufferOrAddDelimiter(): Unit =
    if (buffer == null) buffer = new StringBuilder()
    else buffer.append(delim)

  def append(s: String): SequenceEncoder = {
    startBufferOrAddDelimiter()

    if (s == null || s.isEmpty) return this

    if (s.charAt(0) == '\\' || (s.charAt(0) == '\'' && s.charAt(s.length - 1) == '\'')) {
      buffer.append('\'')
      appendEscapedString(s)
      buffer.append('\'')
    }
    else {
      appendEscapedString(s)
    }

    this
  }

  private def appendEscapedChar(c: Char): Unit = {
    if (c == delim) buffer.append('\\')
    buffer.append(c)
  }

  def append(c: Char): SequenceEncoder = {
    startBufferOrAddDelimiter()

    if (c == '\\' || c == '\'') {
      buffer.append('\'')
      appendEscapedChar(c)
      buffer.append('\'')
    }
    else {
      appendEscapedChar(c)
    }

    this
  }

  def append(i: Int): SequenceEncoder = append(String.valueOf(i))
  def append(l: Long): SequenceEncoder = append(String.valueOf(l))
  def append(d: Double): SequenceEncoder = append(String.valueOf(d))
  def append(b: Boolean): SequenceEncoder = append(String.valueOf(b))

  /** @return the encoded string, or `null` if nothing has been appended --
    *         matching the legacy nullable-return contract, since callers
    *         (e.g. the Command dispatcher) rely on distinguishing "empty
    *         sequence" from "sequence of one empty element."
    */
  def getValue: String = if (buffer != null) buffer.toString else null

  private def appendEscapedString(s: String): Unit = {
    var begin = 0
    var end = s.indexOf(delim)
    while (begin <= end) {
      buffer.append(s.substring(begin, end)).append('\\')
      begin = end
      end = s.indexOf(delim, end + 1)
    }
    buffer.append(s.substring(begin))
  }
}

object SequenceEncoder {

  /** Splits a string produced by [[SequenceEncoder]] back into its elements. */
  final class Decoder(value: String, private val delim: Char) extends Iterator[String] {
    private var v: String = value
    private var buf: StringBuilder = null
    private var start: Int = 0
    private val stop: Int = if (value != null) value.length else 0

    def this(d: Decoder) = {
      this(d.v, d.delim)
      start = d.start
    }

    def hasMoreTokens: Boolean = v != null

    def getRemaining: String = if (!hasMoreTokens) "" else v.substring(start, stop)

    def nextToken(): String = {
      if (!hasMoreTokens) throw new NoSuchElementException()

      if (start == stop) {
        // token for "null" is the empty string
        v = null
        return ""
      }

      if (buf != null) buf.setLength(0)

      var tok: String = null
      var i = start
      var foundDelim = false
      while (i < stop && !foundDelim) {
        if (v.charAt(i) == delim) {
          if (i > 0 && v.charAt(i - 1) == '\\') {
            // escaped delimiter; piece together the token, dropping just the backslash
            if (buf == null) buf = new StringBuilder()
            buf.append(v.substring(start, i - 1))
            start = i
            i += 1
          }
          else {
            // real delimiter: this token ends here
            if (buf == null || buf.isEmpty) tok = v.substring(start, i)
            else buf.append(v.substring(start, i))
            start = i + 1
            foundDelim = true
          }
        }
        else {
          i += 1
        }
      }

      if (!foundDelim) {
        // reached the end of the string without a(nother) real delimiter
        if (buf == null || buf.isEmpty) tok = v.substring(start)
        else buf.append(v.substring(start, stop))
        v = null
      }

      unquote(if (tok != null) tok else buf.toString)
    }

    private def unquote(s: String): String = {
      val len = s.length
      if (len > 1 && s.charAt(0) == '\'' && s.charAt(len - 1) == '\'') s.substring(1, len - 1)
      else s
    }

    def hasNext: Boolean = hasMoreTokens
    def next(): String = nextToken()

    def copy(): Decoder = new Decoder(this)
  }
}
