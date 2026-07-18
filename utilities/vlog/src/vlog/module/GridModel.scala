package vlog.module

/** Parsed geometry of a module's maps/boards/grids, enough to reproduce
  * VASSAL's pixel -> location-name computation for the standard grid types.
  * Ported field-for-field from the module's buildFile so the same attributes
  * feed the same math VASSAL runs at record time.
  */

/** Numbering scheme of a RegularGridNumbering (Hex or Square), matching the
  * attributes of VASSAL.build.module.map.boardPicker.board.mapgrid.* . */
final case class Numbering(
    first: Char, // 'H' => column shown first, 'V' => row first
    sep: String,
    hType: Char, // 'A' alphabetic, 'N' numeric
    vType: Char,
    hLeading: Int,
    vLeading: Int,
    hOff: Int,
    vOff: Int,
    hDescend: Boolean,
    vDescend: Boolean,
    stagger: Boolean,
    locationFormat: String = "$gridLocation$"
)

sealed trait GridDef:
  /** Present only for a recognized standard numbering; None means the grid has
    * a custom/absent numbering we cannot label. */
  def numbering: Option[Numbering]

  /** The class name of a numbering we could not reproduce (e.g. a module's own
    * `ak.ObliqueHexGridNumbering`), for reporting. */
  def customNumbering: Option[String]

final case class HexGridDef(
    x0: Int,
    y0: Int,
    dx: Double, // hex width  (getHexWidth)
    dy: Double, // hex size   (getHexSize)
    sideways: Boolean,
    numbering: Option[Numbering],
    customNumbering: Option[String] = None
) extends GridDef

final case class SquareGridDef(
    x0: Int,
    y0: Int,
    dx: Double,
    dy: Double,
    numbering: Option[Numbering],
    customNumbering: Option[String] = None
) extends GridDef

/** A ZonedGrid: an ordered list of zones (each a polygon region with its own
  * grid) over an optional background grid. VASSAL resolves a point to the first
  * zone whose polygon contains it, else the background grid. */
final case class ZonedGridDef(zones: Vector[ZoneDef], background: Option[GridDef]) extends GridDef:
  def numbering: Option[Numbering] = None
  def customNumbering: Option[String] = None

/** One zone of a ZonedGrid.
  * @param polygon in board coordinates (java.awt.Polygon for exact
  *   VASSAL-matching point containment)
  * @param locationFormat VASSAL Zone location format, referencing `$name$`
  *   and/or `$gridLocation$` (default `$name$`)
  */
final case class ZoneDef(
    name: String,
    polygon: java.awt.Polygon,
    grid: Option[GridDef],
    locationFormat: String
)

/** A grid type we do not compute (e.g. RegionGrid), preserved for reporting. */
final case class UnsupportedGrid(kind: String) extends GridDef:
  def numbering: Option[Numbering] = None
  def customNumbering: Option[String] = None

final case class BoardDef(name: String, image: Option[String], grid: Option[GridDef])

final case class MapDef(name: String, boards: Vector[BoardDef]):
  def board(boardName: String): Option[BoardDef] = boards.find(_.name == boardName)
  /** The board to use when the caller doesn't name one: the only board, if
    * there is exactly one that has a grid. */
  def soleGriddedBoard: Option[BoardDef] = boards.filter(_.grid.isDefined) match
    case Vector(only) => Some(only)
    case _ => boards.headOption

final case class ModuleDef(maps: Vector[MapDef]):
  def map(mapName: String): Option[MapDef] = maps.find(_.name == mapName)
