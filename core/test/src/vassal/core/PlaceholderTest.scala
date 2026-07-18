package vassal.core

class PlaceholderTest extends munit.FunSuite {
  test("scaffold compiles and runs on the JVM") {
    assertEquals(Placeholder.phase, "1")
  }
}
