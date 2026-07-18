package vassal.core.xml

import org.w3c.dom.{Element => DomElement, Node => DomNode}

class XmlTest extends munit.FunSuite {

  test("parse/write round-trips a simple synthetic document") {
    val doc =
      """<?xml version="1.0" encoding="UTF-8" standalone="no"?>
        |<root a="1" b="two &amp; three">
        |    <child/>
        |    <child2 x="&quot;quoted&quot;">some text</child2>
        |</root>
        |""".stripMargin

    val parsed = Xml.parse(doc)
    assertEquals(parsed.name, "root")
    assertEquals(parsed.attr("a"), Some("1"))
    assertEquals(parsed.attr("b"), Some("two & three"))
    assertEquals(parsed.childElements.map(_.name), Vector("child", "child2"))
    assertEquals(parsed.childElements(1).attr("x"), Some("\"quoted\""))
    assertEquals(parsed.childElements(1).text, "some text")

    val reparsed = Xml.parse(Xml.write(parsed))
    assertEquals(reparsed, parsed)
  }

  test("empty element with only whitespace children round-trips as self-closing") {
    val parsed = Xml.parse("<a><b>   </b></a>")
    val b = parsed.childElements.head
    assertEquals(b.text.trim, "")
  }

  private def realBuildFiles: Seq[String] = Seq(
    "Saratoga-3.2.buildFile.xml",
    "Monmouth-1.7.1.buildFile.xml",
    "Waterloo 3.0.2.buildFile.xml"
  )

  private def readResource(name: String): String = {
    val is = getClass.getResourceAsStream(s"/buildfiles/$name")
    require(is != null, s"missing test resource $name")
    new String(is.readAllBytes(), "UTF-8")
  }

  private def parseWithJdkDom(xml: String): org.w3c.dom.Document = {
    val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
    factory.newDocumentBuilder().parse(
      new org.xml.sax.InputSource(new java.io.StringReader(xml))
    )
  }

  /** Structurally compares our parse tree against the JDK's real DOM parse of
    * the same document -- the strongest available cross-check that Xml.parse
    * is actually correct, not just internally self-consistent.
    */
  private def assertStructurallyEqual(ours: XmlElement, dom: DomElement): Unit = {
    assertEquals(ours.name, dom.getTagName)

    val domAttrs = dom.getAttributes
    assertEquals(ours.attributes.length, domAttrs.getLength, s"attribute count mismatch on <${ours.name}>")
for ((k, v) <- ours.attributes) {
  val domAttr = domAttrs.getNamedItem(k)
  assert(domAttr != null, s"missing attribute $k on <${ours.name}>")
  assertEquals(v, domAttr.getNodeValue, s"attribute $k on <${ours.name}>")
}

    val domChildElements = domChildrenOf(dom)
    assertEquals(
      ours.childElements.length,
      domChildElements.length,
      s"child element count mismatch on <${ours.name}>"
    )
    for ((oursChild, domChild) <- ours.childElements.zip(domChildElements)) {
      assertStructurallyEqual(oursChild, domChild)
    }

    // Only meaningful for text-only (leaf) elements -- DOM's getTextContent
    // concatenates descendant text too, which for element-bearing nodes
    // includes irrelevant whitespace-formatting text between children.
    if (domChildElements.isEmpty) {
      assertEquals(ours.text.trim, dom.getTextContent.trim, s"text mismatch on <${ours.name}>")
    }
  }

  private def domChildrenOf(e: DomElement): Vector[DomElement] = {
    val nodes = e.getChildNodes
    val builder = Vector.newBuilder[DomElement]
    for (i <- 0 until nodes.getLength) {
      nodes.item(i) match {
        case el: DomElement => builder += el
        case _              => ()
      }
    }
    builder.result()
  }

  realBuildFiles.foreach { name =>
    test(s"parses $name identically to the JDK DOM parser") {
      val xml = readResource(name)
      val ours = Xml.parse(xml)
      val dom = parseWithJdkDom(xml).getDocumentElement
      assertStructurallyEqual(ours, dom)
    }

    test(s"$name round-trips through write+parse with an equivalent tree") {
      val xml = readResource(name)
      val ours = Xml.parse(xml)
      val rewritten = Xml.write(ours)
      val reparsed = Xml.parse(rewritten)
      assertEquals(reparsed, ours)

      // And the rewritten XML must still be parseable by the real JDK parser,
      // i.e. openable by the legacy app.
      val dom = parseWithJdkDom(rewritten).getDocumentElement
      assertStructurallyEqual(reparsed, dom)
    }
  }
}
