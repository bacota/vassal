package vlog.moves

import org.scalatest.funsuite.AnyFunSuite

class MoveReportTest extends AnyFunSuite:

  test("parses the &rarr; hex format") {
    val r = MoveReport.parse("* Subervie, L'Heritier moves GG17 &rarr; FF18 *")
    assert(r.contains(MoveReport.Parsed("Subervie, L'Heritier", "GG17", "FF18")))
  }

  test("parses the -> area format with a player prefix") {
    val r = MoveReport.parse("* <bacota> Kitakami, Oi moves Japanese Islands -> Marianas Islands *")
    assert(r.contains(MoveReport.Parsed("Kitakami, Oi", "Japanese Islands", "Marianas Islands")))
  }

  test("parses a unicode arrow") {
    val r = MoveReport.parse("Tank moves A1 → B2")
    assert(r.contains(MoveReport.Parsed("Tank", "A1", "B2")))
  }

  test("decodes HTML entities in names but keeps the arrow") {
    val r = MoveReport.parse("* A &amp; B moves H&amp;1 &rarr; H&amp;2 *")
    assert(r.contains(MoveReport.Parsed("A & B", "H&1", "H&2")))
  }

  test("parses the arrowless 'moves from A to B' prose format") {
    val r = MoveReport.parse("* 17th Foot moves from H12 to H13 *")
    assert(r.contains(MoveReport.Parsed("17th Foot", "H12", "H13")))
  }

  test("parses 'moves from A -> B' (leading from plus arrow)") {
    val r = MoveReport.parse("* Unit moves from A1 -&gt; B2 *".replace("&gt;", ">"))
    assert(r.contains(MoveReport.Parsed("Unit", "A1", "B2")))
  }

  test("returns None for a non-move chat line") {
    assert(MoveReport.parse("* Alice rolls 2d6 = 7 *").isEmpty)
  }

  test("returns None when there is a verb but no arrow") {
    assert(MoveReport.parse("* Tank moved off the board *").isEmpty)
  }
