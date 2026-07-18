package vlog.module

import scala.xml.{Elem, Node, XML}

/** Parses a module's buildFile.xml into a [[ModuleDef]]: its maps, their
  * boards, and each board's (background) grid and numbering.
  *
  * Only the standard VASSAL grid/numbering classes are reproduced. A grid with
  * a custom numbering class (e.g. `ak.ObliqueHexGridNumbering`) is kept with
  * its geometry but no numbering, and the custom class name is recorded so the
  * resolver can explain why it can't produce a label.
  */
object ModuleParser:

  private val MapLabel = "VASSAL.build.module.Map"
  private val BoardLabel = "VASSAL.build.module.map.boardPicker.Board"
  private val HexLabel = "VASSAL.build.module.map.boardPicker.board.HexGrid"
  private val SquareLabel = "VASSAL.build.module.map.boardPicker.board.SquareGrid"
  private val RegionLabel = "VASSAL.build.module.map.boardPicker.board.RegionGrid"
  private val ZoneLabel = "VASSAL.build.module.map.boardPicker.board.mapgrid.Zone"
  private val HexNumbering = "VASSAL.build.module.map.boardPicker.board.mapgrid.HexGridNumbering"
  private val SquareNumbering = "VASSAL.build.module.map.boardPicker.board.mapgrid.SquareGridNumbering"

  def parse(buildFileXml: String): ModuleDef =
    val root = XML.loadString(buildFileXml)
    val maps = elems(root).filter(_.label == MapLabel).map(parseMap)
    ModuleDef(maps.toVector)

  private def parseMap(mapElem: Elem): MapDef =
    val boards = descendants(mapElem).filter(_.label == BoardLabel).map(parseBoard)
    MapDef(attr(mapElem, "mapName"), boards.toVector)

  private val ZonedLabel = "VASSAL.build.module.map.boardPicker.board.ZonedGrid"

  private def parseBoard(boardElem: Elem): BoardDef =
    val image = Option(attr(boardElem, "image")).filter(_.nonEmpty)
    BoardDef(attr(boardElem, "name"), image, topGrid(boardElem).map(parseGrid))

  /** The board's outermost grid element: a ZonedGrid or a bare Hex/Square/Region
    * grid (whichever is found first, top-down). */
  private def topGrid(node: Node): Option[Elem] =
    val kids = elems(node)
    kids
      .find(k => Set(ZonedLabel, HexLabel, SquareLabel, RegionLabel).contains(k.label))
      .orElse(kids.iterator.flatMap(topGrid).nextOption())

  /** First direct-child Hex/Square/Region grid of a container (ZonedGrid or
    * Zone), not descending further. */
  private def directGrid(node: Elem): Option[Elem] =
    elems(node).find(k => k.label == HexLabel || k.label == SquareLabel || k.label == RegionLabel)

  private def parseGrid(g: Elem): GridDef = g.label match
    case ZonedLabel =>
      val zones = elems(g).filter(_.label == ZoneLabel).map(parseZone).toVector
      val background = directGrid(g).map(parseGrid)
      ZonedGridDef(zones, background)
    case HexLabel =>
      val (num, custom) = parseNumbering(g, HexNumbering)
      HexGridDef(
        x0 = attrInt(g, "x0"),
        y0 = attrInt(g, "y0"),
        dx = attrDouble(g, "dx"),
        dy = attrDouble(g, "dy"),
        sideways = attrBool(g, "sideways"),
        numbering = num,
        customNumbering = custom
      )
    case SquareLabel =>
      val (num, custom) = parseNumbering(g, SquareNumbering)
      SquareGridDef(
        x0 = attrInt(g, "x0"),
        y0 = attrInt(g, "y0"),
        dx = attrDouble(g, "dx"),
        dy = attrDouble(g, "dy"),
        numbering = num,
        customNumbering = custom
      )
    case RegionLabel => UnsupportedGrid("RegionGrid")
    case other => UnsupportedGrid(other)

  private def parseZone(z: Elem): ZoneDef =
    ZoneDef(
      name = attr(z, "name"),
      polygon = parsePolygon(attr(z, "path")),
      grid = directGrid(z).map(parseGrid),
      locationFormat = attr(z, "locationFormat") match
        case "" => "$name$"
        case f => f
    )

  /** Parse a Zone `path` ("x1,y1;x2,y2;...") into a polygon, mirroring
    * VASSAL.build.module.map.boardPicker.board.mapgrid.PolygonEditor. */
  private def parsePolygon(path: String): java.awt.Polygon =
    val poly = new java.awt.Polygon()
    if path != null then
      path.split(";").foreach { pt =>
        pt.split(",") match
          case Array(xs, ys) =>
            try poly.addPoint(xs.trim.toInt, ys.trim.toInt)
            catch case _: NumberFormatException => ()
          case _ => ()
      }
    poly

  /** Returns (parsed numbering, custom-class-name). A numbering child with the
    * expected standard label is parsed; any other numbering-looking child is
    * reported as custom and left unparsed. */
  private def parseNumbering(grid: Elem, standardLabel: String): (Option[Numbering], Option[String]) =
    val numberingElems = elems(grid).filter(_.label.toLowerCase.endsWith("numbering"))
    numberingElems.find(_.label == standardLabel) match
      case Some(n) => (Some(readNumbering(n)), None)
      case None =>
        (None, numberingElems.headOption.map(_.label))

  private def readNumbering(n: Elem): Numbering =
    Numbering(
      first = attr(n, "first").headOption.getOrElse('H'),
      sep = attr(n, "sep"),
      hType = attr(n, "hType").headOption.getOrElse('N'),
      vType = attr(n, "vType").headOption.getOrElse('N'),
      hLeading = attrInt(n, "hLeading"),
      vLeading = attrInt(n, "vLeading"),
      hOff = attrInt(n, "hOff"),
      vOff = attrInt(n, "vOff"),
      hDescend = attrBool(n, "hDescend"),
      vDescend = attrBool(n, "vDescend"),
      stagger = attrBool(n, "stagger"),
      locationFormat = attr(n, "locationFormat") match
        case "" => "$gridLocation$"
        case f => f
    )

  // --- XML helpers ---

  private def elems(n: Node): Seq[Elem] = n.child.collect { case e: Elem => e }.toSeq

  private def descendants(n: Node): Seq[Elem] =
    elems(n).flatMap(e => e +: descendants(e))

  private def attr(e: Elem, name: String): String = (e \@ name)

  private def attrInt(e: Elem, name: String): Int =
    attr(e, name) match
      case "" => 0
      case s => math.round(s.toDouble).toInt

  private def attrDouble(e: Elem, name: String): Double =
    attr(e, name) match
      case "" => 0.0
      case s => s.toDouble

  private def attrBool(e: Elem, name: String): Boolean = attr(e, name) == "true"
