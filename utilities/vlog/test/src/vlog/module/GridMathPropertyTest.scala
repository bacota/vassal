package vlog.module

import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalacheck.Gen

class GridMathPropertyTest extends AnyFunSuite with ScalaCheckPropertyChecks:

  private val Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

  private def numbering(
      first: Char = 'H', hType: Char = 'N', vType: Char = 'N',
      hOff: Int = 0, vOff: Int = 0, locationFormat: String = "$gridLocation$"
  ): Numbering =
    Numbering(first, "", hType, vType, 0, 0, hOff, vOff, false, false, false, locationFormat)

  /** With a unit square grid at the origin and no offsets, an integer point
    * (col, row) is numbered exactly by its coordinates. */
  test("numeric numbering names an integer point by its coordinates") {
    val grid = SquareGridDef(0, 0, dx = 1, dy = 1, numbering = Some(numbering()))
    forAll(Gen.choose(0, 999), Gen.choose(0, 999)) { (x, y) =>
      assert(GridMath.locationName(grid, None, (x, y)).contains(s"$x$y"))
    }
  }

  test("alphabetic column numbering matches the A..Z, AA.. sequence") {
    // Isolate the column via a locationFormat of just "$column$".
    val grid = SquareGridDef(0, 0, dx = 1, dy = 1,
      numbering = Some(numbering(hType = 'A', locationFormat = "$column$")))
    forAll(Gen.choose(0, 25)) { col =>
      assert(GridMath.locationName(grid, None, (col, 0)).contains(Alphabet.charAt(col).toString))
    }
    // Past Z, VASSAL's scheme repeats the same letter rather than counting in
    // base 26: 26 -> AA, 27 -> BB, ... 51 -> ZZ, 52 -> AAA.
    assert(GridMath.locationName(grid, None, (26, 0)).contains("AA"))
    assert(GridMath.locationName(grid, None, (27, 0)).contains("BB"))
    assert(GridMath.locationName(grid, None, (51, 0)).contains("ZZ"))
    assert(GridMath.locationName(grid, None, (52, 0)).contains("AAA"))
  }

  // Arbitrary (but valid: positive cell sizes) grid + numbering + point.
  private val genNumbering: Gen[Numbering] =
    for
      first <- Gen.oneOf('H', 'V')
      hType <- Gen.oneOf('A', 'N'); vType <- Gen.oneOf('A', 'N')
      hLead <- Gen.choose(0, 3); vLead <- Gen.choose(0, 3)
      hOff <- Gen.choose(-30, 30); vOff <- Gen.choose(-30, 30)
      hDesc <- Gen.prob(0.3); vDesc <- Gen.prob(0.3); stag <- Gen.prob(0.3)
    yield Numbering(first, "", hType, vType, hLead, vLead, hOff, vOff, hDesc, vDesc, stag)

  private val genSize = Gen.choose(200, 5000)
  private val genCoord = Gen.choose(-5000, 5000)

  test("hex naming is total and never throws for any point and numbering") {
    val genGrid = for
      x0 <- genCoord; y0 <- genCoord
      dx <- Gen.choose(20.0, 300.0); dy <- Gen.choose(20.0, 300.0)
      sideways <- Gen.prob(0.5); n <- genNumbering
    yield HexGridDef(x0, y0, dx, dy, sideways, Some(n))

    forAll(genGrid, genSize, genSize, genCoord, genCoord) { (grid, w, h, x, y) =>
      // Must return a value (Some/None), never throw, for any input.
      val result = GridMath.locationName(grid, Some((w, h)), (x, y))
      assert(result.forall(_ != null))
    }
  }
