package vassal.core.build

import vassal.core.xml.{Xml, XmlElement}

private final class FakeDocumentation extends Buildable {
  var built: Boolean = false
  var parent: Buildable = null

  def build(e: XmlElement): Unit = built = true
  def addTo(p: Buildable): Unit = parent = p
  def add(child: Buildable): Unit = ()
  def buildElement: XmlElement = XmlElement("VASSAL.build.module.Documentation", Vector.empty, Vector.empty)
}

private final class FakeGameModule extends Buildable {
  var children: List[Buildable] = Nil

  def build(e: XmlElement): Unit = ()
  def addTo(p: Buildable): Unit = ()
  def add(child: Buildable): Unit = children ::= child
  def buildElement: XmlElement = XmlElement("VASSAL.build.GameModule", Vector.empty, Vector.empty)
}

class ComponentRegistryTest extends munit.FunSuite {

  test("create instantiates the registered factory") {
    ComponentRegistry.register(
      "vassal.core.build.ComponentRegistryTest.FakeDocumentation",
      () => new FakeDocumentation
    )
    val instance = ComponentRegistry.create("vassal.core.build.ComponentRegistryTest.FakeDocumentation")
    assert(instance.isInstanceOf[FakeDocumentation])
  }

  test("register returns a fresh instance each call, matching Class.forName().newInstance() semantics") {
    ComponentRegistry.register("test.Fresh", () => new FakeDocumentation)
    val a = ComponentRegistry.create("test.Fresh")
    val b = ComponentRegistry.create("test.Fresh")
    assert(a ne b)
  }

  test("create on an unregistered class name fails with a clear, actionable error") {
    val ex = intercept[NoSuchComponentException] {
      ComponentRegistry.create("VASSAL.build.module.SomeNotYetPortedComponent")
    }
    assert(ex.getMessage.contains("VASSAL.build.module.SomeNotYetPortedComponent"))
    assert(ex.getMessage.contains("hasn't been ported"))
  }

  test("isRegistered reflects registration state") {
    assertEquals(ComponentRegistry.isRegistered("VASSAL.build.module.TotallyUnknown"), false)
    ComponentRegistry.register("VASSAL.build.module.TotallyUnknown", () => new FakeDocumentation)
    assertEquals(ComponentRegistry.isRegistered("VASSAL.build.module.TotallyUnknown"), true)
  }

  test("a Buildable can build children discovered in its buildFile.xml element via the registry") {
    ComponentRegistry.register("VASSAL.build.module.Documentation", () => new FakeDocumentation)

    val moduleElement = Xml.parse(
      """<VASSAL.build.GameModule>
        |  <VASSAL.build.module.Documentation/>
        |</VASSAL.build.GameModule>""".stripMargin
    )

    val module = new FakeGameModule
    for (childElement <- moduleElement.childElements) {
      val child = ComponentRegistry.create(childElement.name)
      child.build(childElement)
      child.addTo(module)
      module.add(child)
    }

    assertEquals(module.children.length, 1)
    val doc = module.children.head.asInstanceOf[FakeDocumentation]
    assert(doc.built)
    assertEquals(doc.parent, module)
  }
}
