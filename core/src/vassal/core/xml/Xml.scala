package vassal.core.xml

/** A node in a parsed XML document: either an element or a run of text. */
sealed trait XmlNode

/** @param attributes in document order, since buildFile.xml diffing/round-tripping
  *                   benefits from stable ordering even though it isn't required
  *                   for compatibility (only well-formedness/openability is).
  */
final case class XmlElement(
  name: String,
  attributes: Vector[(String, String)],
  children: Vector[XmlNode]
) extends XmlNode {
  def attr(key: String): Option[String] = attributes.collectFirst { case (k, v) if k == key => v }

  /** Direct child elements, i.e. children filtered to ignore whitespace/text nodes --
    * this is the view VASSAL's Buildable/Builder tree-walking code actually consumes.
    */
  def childElements: Vector[XmlElement] = children.collect { case e: XmlElement => e }

  /** Concatenated text of direct XmlText children, e.g. the "British" in `<entry>British</entry>`. */
  def text: String = children.collect { case XmlText(t) => t }.mkString
}

final case class XmlText(value: String) extends XmlNode

/** Parses and serializes buildFile.xml-style documents without `javax.xml`/DOM,
  * which doesn't exist under Scala.js. Handles exactly what real buildFile.xml
  * content uses (elements, attributes, text content, standard entity refs) --
  * no DTDs, processing instructions beyond the XML declaration, comments, or
  * CDATA, none of which appear in any file in the Phase-0 corpus.
  */
object Xml {

  final case class ParseException(message: String, position: Int)
      extends RuntimeException(s"$message (at offset $position)")

  /** Parses a complete XML document, returning its root element. */
  def parse(source: String): XmlElement = {
    val p = new Parser(source)
    p.skipWhitespace()
    if (p.lookingAt("<?xml")) p.skipTo("?>")
    p.skipWhitespace()
    val root = p.parseElement()
    root
  }

  private final class Parser(s: String) {
    var pos: Int = 0

    def lookingAt(literal: String): Boolean =
      s.regionMatches(pos, literal, 0, literal.length)

    def skipTo(literal: String): Unit = {
      val i = s.indexOf(literal, pos)
      if (i < 0) throw ParseException(s"Expected '$literal'", pos)
      pos = i + literal.length
    }

    def skipWhitespace(): Unit =
      while (pos < s.length && s.charAt(pos).isWhitespace) pos += 1

    def parseElement(): XmlElement = {
      if (s.charAt(pos) != '<') throw ParseException("Expected '<'", pos)
      pos += 1
      val name = readName()

      val attrs = Vector.newBuilder[(String, String)]
      skipWhitespace()
      while (pos < s.length && s.charAt(pos) != '/' && s.charAt(pos) != '>') {
        val attrName = readName()
        skipWhitespace()
        if (s.charAt(pos) != '=') throw ParseException("Expected '=' in attribute", pos)
        pos += 1
        skipWhitespace()
        val quote = s.charAt(pos)
        if (quote != '"' && quote != '\'') throw ParseException("Expected quoted attribute value", pos)
        pos += 1
        val valStart = pos
        val valEnd = s.indexOf(quote, valStart)
        if (valEnd < 0) throw ParseException("Unterminated attribute value", pos)
        attrs += attrName -> unescape(s.substring(valStart, valEnd))
        pos = valEnd + 1
        skipWhitespace()
      }

      if (s.charAt(pos) == '/') {
        pos += 1
        if (s.charAt(pos) != '>') throw ParseException("Expected '>' after '/'", pos)
        pos += 1
        return XmlElement(name, attrs.result(), Vector.empty)
      }

      // s.charAt(pos) == '>'
      pos += 1
      val children = Vector.newBuilder[XmlNode]
      var done = false
      while (!done) {
        if (lookingAt("</")) {
          pos += 2
          val closeName = readName()
          if (closeName != name) {
            throw ParseException(s"Mismatched closing tag: expected '$name', got '$closeName'", pos)
          }
          skipWhitespace()
          if (s.charAt(pos) != '>') throw ParseException("Expected '>'", pos)
          pos += 1
          done = true
        }
        else if (lookingAt("<!--")) {
          skipTo("-->")
        }
        else if (lookingAt("<![CDATA[")) {
          pos += 9
          val end = s.indexOf("]]>", pos)
          if (end < 0) throw ParseException("Unterminated CDATA section", pos)
          children += XmlText(s.substring(pos, end))
          pos = end + 3
        }
        else if (s.charAt(pos) == '<') {
          children += parseElement()
        }
        else {
          val textStart = pos
          val nextLt = s.indexOf('<', pos)
          val textEnd = if (nextLt < 0) s.length else nextLt
          pos = textEnd
          val raw = s.substring(textStart, textEnd)
          if (raw.nonEmpty) children += XmlText(unescape(raw))
        }
      }

      XmlElement(name, attrs.result(), children.result())
    }

    private def readName(): String = {
      val start = pos
      while (pos < s.length && isNameChar(s.charAt(pos))) pos += 1
      if (pos == start) throw ParseException("Expected a name", pos)
      s.substring(start, pos)
    }

    private def isNameChar(c: Char): Boolean =
      !c.isWhitespace && c != '=' && c != '>' && c != '/' && c != '<'
  }

  private def unescape(s: String): String = {
    if (!s.contains('&')) return s
    val sb = new StringBuilder(s.length)
    var i = 0
    while (i < s.length) {
      val c = s.charAt(i)
      if (c == '&') {
        val semi = s.indexOf(';', i)
        if (semi < 0) { sb.append(c); i += 1 }
        else {
          val entity = s.substring(i + 1, semi)
          entity match {
            case "amp"  => sb.append('&'); i = semi + 1
            case "lt"   => sb.append('<'); i = semi + 1
            case "gt"   => sb.append('>'); i = semi + 1
            case "quot" => sb.append('"'); i = semi + 1
            case "apos" => sb.append('\''); i = semi + 1
            case _ if entity.startsWith("#x") || entity.startsWith("#X") =>
              sb.append(Integer.parseInt(entity.substring(2), 16).toChar); i = semi + 1
            case _ if entity.startsWith("#") =>
              sb.append(Integer.parseInt(entity.substring(1)).toChar); i = semi + 1
            case _ => sb.append(c); i += 1
          }
        }
      }
      else { sb.append(c); i += 1 }
    }
    sb.toString
  }

  private def escapeText(s: String): String = {
    val sb = new StringBuilder(s.length)
    for (c <- s) c match {
      case '&' => sb.append("&amp;")
      case '<' => sb.append("&lt;")
      case '>' => sb.append("&gt;")
      case _   => sb.append(c)
    }
    sb.toString
  }

  private def escapeAttr(s: String): String = {
    val sb = new StringBuilder(s.length)
    for (c <- s) c match {
      case '&'  => sb.append("&amp;")
      case '<'  => sb.append("&lt;")
      case '>'  => sb.append("&gt;")
      case '"'  => sb.append("&quot;")
      case '\t' => sb.append("&#9;")
      case '\n' => sb.append("&#10;")
      case '\r' => sb.append("&#13;")
      case _    => sb.append(c)
    }
    sb.toString
  }

  /** Serializes `root` back to a complete XML document string, with the same
    * `<?xml version="1.0" encoding="UTF-8" standalone="no"?>` declaration the
    * legacy app writes.
    */
  def write(root: XmlElement): String = {
    val sb = new StringBuilder
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n")
    writeElement(sb, root, 0)
    sb.append('\n')
    sb.toString
  }

  private def writeElement(sb: StringBuilder, e: XmlElement, depth: Int): Unit = {
    val indent = "    " * depth
    sb.append(indent).append('<').append(e.name)
    for ((k, v) <- e.attributes) sb.append(' ').append(k).append("=\"").append(escapeAttr(v)).append('"')

    val elementChildren = e.childElements
    if (e.children.isEmpty) {
      sb.append("/>")
    }
    else if (elementChildren.isEmpty) {
      // Text-only content: keep it inline, matching the legacy writer's style
      // (e.g. `<option name="x">value</option>`).
      sb.append('>').append(escapeText(e.text)).append("</").append(e.name).append('>')
    }
    else {
      sb.append(">\n")
      for (child <- elementChildren) {
        writeElement(sb, child, depth + 1)
        sb.append('\n')
      }
      sb.append(indent).append("</").append(e.name).append('>')
    }
  }
}
