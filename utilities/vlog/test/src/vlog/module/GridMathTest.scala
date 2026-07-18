package vlog.module

import org.scalatest.funsuite.AnyFunSuite

class GridMathTest extends AnyFunSuite:

  private def num(
      first: Char = 'H', sep: String = "", hType: Char = 'N', vType: Char = 'N',
      hLeading: Int = 1, vLeading: Int = 1, hOff: Int = 0, vOff: Int = 0,
      hDescend: Boolean = false, vDescend: Boolean = false, stagger: Boolean = false,
      locationFormat: String = "$gridLocation$"
  ): Numbering =
    Numbering(first, sep, hType, vType, hLeading, vLeading, hOff, vOff, hDescend, vDescend, stagger, locationFormat)

  test("hex matches VASSAL's recorded ground truth (Monmouth Main Map)") {
    // Board 'Monmouth Map' HexGrid + HexGridNumbering, verified against the
    // OldLocationName/OldX/OldY triples stored in the Monmouth vlogs.
    val hex = HexGridDef(x0 = 74, y0 = 71, dx = 112.6, dy = 129.6, sideways = false,
      numbering = Some(num(hLeading = 1, vLeading = 1, hOff = 8, vOff = 1)))
    assert(GridMath.locationName(hex, None, (2776, 2792)).contains("3222"))
    assert(GridMath.locationName(hex, None, (3001, 2403)).contains("3419"))
  }

  test("square grid with alphabetic columns") {
    val sq = SquareGridDef(0, 0, dx = 50, dy = 50,
      numbering = Some(num(first = 'H', hType = 'A', vType = 'N', hLeading = 0, vLeading = 0)))
    // (60,80): col floor(60/50+0.5)=1 -> 'B', row (int)(80/50+0.5)=2 -> "2"
    assert(GridMath.locationName(sq, None, (60, 80)).contains("B2"))
  }

  test("alphabetic column wraps past Z to AA") {
    val sq = SquareGridDef(0, 0, dx = 10, dy = 10,
      numbering = Some(num(first = 'H', hType = 'A', vType = 'N', hLeading = 0, vLeading = 0)))
    // col floor(260/10+0.5)=26 -> "AA"
    assert(GridMath.locationName(sq, None, (260, 55)).contains("AA6"))
  }

  test("a numbering locationFormat like 'Turn \\$column\\$' is applied") {
    val sq = SquareGridDef(100, 0, dx = 10, dy = 10,
      numbering = Some(num(first = 'V', hOff = 9, hLeading = 0, vLeading = 0, locationFormat = "Turn $column$")))
    // col floor((30-100)/10+0.5) = floor(-6.5) = -7; col+hOff = 2 -> "Turn 2"
    assert(GridMath.locationName(sq, None, (30, 55)).contains("Turn 2"))
  }

  test("zoned grid resolves to the containing zone's grid") {
    val inner = SquareGridDef(0, 0, dx = 50, dy = 50,
      numbering = Some(num(hType = 'A', hLeading = 0, vLeading = 0)))
    val square = new java.awt.Polygon(Array(0, 100, 100, 0), Array(0, 0, 100, 100), 4)
    val zoned = ZonedGridDef(
      zones = Vector(ZoneDef("Area", square, Some(inner), "$gridLocation$")),
      background = None
    )
    assert(GridMath.locationName(zoned, None, (60, 80)).contains("B2")) // inside zone
    assert(GridMath.locationName(zoned, None, (500, 500)).isEmpty) // outside, no background
  }

  test("a gridless zone is named by its zone name") {
    val square = new java.awt.Polygon(Array(0, 100, 100, 0), Array(0, 0, 100, 100), 4)
    val zoned = ZonedGridDef(
      zones = Vector(ZoneDef("Eliminated Pile", square, None, "$name$")),
      background = None
    )
    assert(GridMath.locationName(zoned, None, (10, 10)).contains("Eliminated Pile"))
  }
