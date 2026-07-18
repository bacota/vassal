package vlog.module

import java.nio.file.Path

/** Resolves a map-coordinate point to its grid location name, using a module's
  * parsed geometry.
  *
  * Coordinate scope: a piece position in a .vlog is in MAP coordinates. This
  * resolver assumes the target board sits at the map origin (true for
  * single-board maps, and for the first board of a multi-board map). Placing
  * boards at other offsets is recorded in the saved game's board layout, not in
  * the module, and is out of scope here — for such maps only the origin board
  * resolves correctly.
  */
final class LocationResolver(val module: ModuleDef, moduleFile: Option[ModuleFile]):

  /** @return Right(locationName) or Left(reason it could not be computed). */
  def locationName(
      mapName: String,
      point: (Int, Int),
      boardName: Option[String] = None
  ): Either[String, String] =
    module.map(mapName) match
      case None => Left(s"map '$mapName' not found in module")
      case Some(m) =>
        boardName.flatMap(m.board).orElse(m.soleGriddedBoard) match
          case None => Left(s"map '$mapName' has no usable board")
          case Some(board) => locateOnBoard(board, point)

  private def locateOnBoard(board: BoardDef, point: (Int, Int)): Either[String, String] =
    board.grid match
      case None => Left(s"board '${board.name}' has no grid")
      case Some(u: UnsupportedGrid) => Left(s"board '${board.name}' uses an unsupported grid (${u.kind})")
      // A leaf grid with no recognized numbering can never produce a label.
      case Some(g @ (_: HexGridDef | _: SquareGridDef)) if g.numbering.isEmpty =>
        Left(g.customNumbering
          .map(c => s"board '${board.name}' uses custom numbering ($c) that can't be reproduced")
          .getOrElse(s"board '${board.name}' has no numbering"))
      case Some(grid) =>
        val boardSize = board.image.flatMap(img => moduleFile.flatMap(_.imageSize(img)))
        GridMath.locationName(grid, boardSize, point)
          .toRight(s"point $point on board '${board.name}' is outside all zones / uses numbering that can't be reproduced")

object LocationResolver:
  def read(modulePath: Path): LocationResolver =
    val file = ModuleFile.read(modulePath)
    new LocationResolver(ModuleParser.parse(file.buildFileXml), Some(file))
