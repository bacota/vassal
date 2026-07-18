package vassal.jvmapp

import vassal.core.Placeholder

/** Phase 1 scaffold entry point. Will grow into the fixture-conformance
  * runner: load vassal-core's JVM build against the Phase-0 corpus and
  * diff against the golden fixtures in vassal-app/src/test/resources/fixtures.
  */
object Main {
  def main(args: Array[String]): Unit =
    println(s"vassal-core scaffold, phase ${Placeholder.phase}")
}
