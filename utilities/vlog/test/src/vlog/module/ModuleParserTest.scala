package vlog.module

import org.scalatest.funsuite.AnyFunSuite

class ModuleParserTest extends AnyFunSuite:

  // A minimal buildFile mirroring the real nesting: Map -> BoardPicker ->
  // Board -> ZonedGrid -> Zone -> SquareGrid -> SquareGridNumbering.
  private val buildFile =
    """<VASSAL.build.GameModule>
      |  <VASSAL.build.module.Map mapName="Main Map">
      |    <VASSAL.build.module.map.BoardPicker>
      |      <VASSAL.build.module.map.boardPicker.Board name="Board" image="b.png">
      |        <VASSAL.build.module.map.boardPicker.board.ZonedGrid>
      |          <VASSAL.build.module.map.boardPicker.board.mapgrid.Zone name="Play Area"
      |               path="0,0;100,0;100,100;0,100" locationFormat="$gridLocation$">
      |            <VASSAL.build.module.map.boardPicker.board.SquareGrid x0="0" y0="0" dx="50" dy="50">
      |              <VASSAL.build.module.map.boardPicker.board.mapgrid.SquareGridNumbering
      |                   first="H" hType="A" vType="N" hOff="0" vOff="0" hLeading="0" vLeading="0" sep=""/>
      |            </VASSAL.build.module.map.boardPicker.board.SquareGrid>
      |          </VASSAL.build.module.map.boardPicker.board.mapgrid.Zone>
      |          <VASSAL.build.module.map.boardPicker.board.mapgrid.Zone name="Dead Pile"
      |               path="200,0;300,0;300,100;200,100" locationFormat="$name$"/>
      |        </VASSAL.build.module.map.boardPicker.board.ZonedGrid>
      |      </VASSAL.build.module.map.boardPicker.Board>
      |    </VASSAL.build.module.map.BoardPicker>
      |  </VASSAL.build.module.Map>
      |</VASSAL.build.GameModule>""".stripMargin

  private val module = ModuleParser.parse(buildFile)
  private val resolver = new LocationResolver(module, None)

  test("parses the map, board and zoned grid") {
    val map = module.map("Main Map").get
    assert(map.boards.map(_.name) == Vector("Board"))
    assert(map.boards.head.grid.exists(_.isInstanceOf[ZonedGridDef]))
  }

  test("resolves a point inside the gridded zone") {
    assert(resolver.locationName("Main Map", (60, 80)) == Right("B2"))
  }

  test("resolves a point in a gridless zone to the zone name") {
    assert(resolver.locationName("Main Map", (250, 50)) == Right("Dead Pile"))
  }

  test("a point outside every zone is reported as unresolved") {
    assert(resolver.locationName("Main Map", (1000, 1000)).isLeft)
  }

  test("an unknown map name is reported") {
    assert(resolver.locationName("No Such Map", (0, 0)).isLeft)
  }

  test("a custom numbering class is recorded and not reproduced") {
    // A board whose grid is numbered by a module-supplied class we can't run.
    val bf =
      """<VASSAL.build.GameModule>
        |  <VASSAL.build.module.Map mapName="Main Map">
        |    <VASSAL.build.module.map.boardPicker.Board name="Board" image="b.png">
        |      <VASSAL.build.module.map.boardPicker.board.HexGrid x0="0" y0="0" dx="60" dy="70" sideways="true">
        |        <ak.ObliqueHexGridNumbering first="H" hType="A" vType="N"/>
        |      </VASSAL.build.module.map.boardPicker.board.HexGrid>
        |    </VASSAL.build.module.map.boardPicker.Board>
        |  </VASSAL.build.module.Map>
        |</VASSAL.build.GameModule>""".stripMargin
    val res = new LocationResolver(ModuleParser.parse(bf), None)
    val outcome = res.locationName("Main Map", (60, 80))
    assert(outcome.isLeft && outcome.swap.exists(_.contains("ObliqueHexGridNumbering")))
  }
