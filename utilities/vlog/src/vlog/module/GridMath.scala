package vlog.module

/** Faithful port of VASSAL's pixel -> location-name math for the standard grid
  * types, mirroring HexGrid + HexGridNumbering, SquareGrid + SquareGridNumbering
  * and RegularGridNumbering. All arithmetic (floor vs. truncate, the sideways
  * axis swap, stagger, descending flips, the leading-zero rule) is copied from
  * those classes so results match VASSAL byte-for-byte.
  *
  * @param boardSize (width, height) of the board image, needed only by grids
  *   whose numbering descends (which reference the map size); pass None when
  *   unknown — a descending grid then yields None.
  */
object GridMath:

  def locationName(grid: GridDef, boardSize: Option[(Int, Int)], p: (Int, Int)): Option[String] =
    grid match
      case h: HexGridDef => h.numbering.flatMap(n => hexName(h, n, boardSize, p))
      case s: SquareGridDef => s.numbering.flatMap(n => squareName(s, n, boardSize, p))
      case z: ZonedGridDef => zonedName(z, boardSize, p)
      case _: UnsupportedGrid => None

  // --- Zoned ---

  private def zonedName(z: ZonedGridDef, boardSize: Option[(Int, Int)], p: (Int, Int)): Option[String] =
    z.zones.find(_.polygon.contains(p._1, p._2)) match
      case Some(zone) =>
        val gridLoc = zone.grid.flatMap(g => locationName(g, boardSize, p))
        val text = applyZoneFormat(zone.locationFormat, zone.name, gridLoc)
        Option.when(text.nonEmpty)(text)
      case None =>
        z.background.flatMap(g => locationName(g, boardSize, p))

  /** Substitute a Zone's location format (VASSAL exposes `$name$` and
    * `$gridLocation$`). */
  private def applyZoneFormat(format: String, name: String, gridLocation: Option[String]): String =
    format
      .replace("$name$", name)
      .replace("$gridLocation$", gridLocation.getOrElse(""))

  // --- Hex ---

  private def hexName(g: HexGridDef, n: Numbering, boardSize: Option[(Int, Int)], p: (Int, Int)): Option[String] =
    // grid.getRawColumn / getRawRow both rotate the point first when sideways.
    val (rx, ry) = if g.sideways then (p._2.toDouble, p._1.toDouble) else (p._1.toDouble, p._2.toDouble)
    val rawColumn = math.floor((rx - g.x0) / g.dx + 0.5).toInt
    val nx = math.round((rx - g.x0) / g.dx).toInt
    val rawRow =
      if nx % 2 == 0 then math.round((ry - g.y0) / g.dy).toInt
      else math.round((ry - g.y0 - g.dy / 2) / g.dy).toInt

    // maxRows/maxColumns use a deliberately swapped axis/size pairing in VASSAL.
    def maxRows = boardSize.map { case (_, h) => math.floor(h / g.dx + 0.5).toInt }
    def maxColumns = boardSize.map { case (w, _) => math.floor(w / g.dy + 0.5).toInt }

    for
      col <- hexColumn(n, g.sideways, rawColumn, maxRows, maxColumns)
      row <- hexRow(n, g.sideways, rawColumn, rawRow, maxRows, maxColumns)
    yield format(n, col, row)

  private def hexColumn(n: Numbering, sideways: Boolean, rawColumn: Int,
      maxRows: => Option[Int], maxColumns: => Option[Int]): Option[Int] =
    if n.vDescend && sideways then maxRows.map(_ - rawColumn)
    else if n.hDescend && !sideways then maxColumns.map(_ - rawColumn)
    else Some(rawColumn)

  private def hexRow(n: Numbering, sideways: Boolean, rawColumn: Int, rawRow: Int,
      maxRows: => Option[Int], maxColumns: => Option[Int]): Option[Int] =
    val base =
      if n.vDescend && !sideways then maxRows.map(_ - rawRow)
      else if n.hDescend && sideways then maxColumns.map(_ - rawRow)
      else Some(rawRow)
    base.map { r =>
      if n.stagger && rawColumn % 2 != 0 then
        val descend = if sideways then n.hDescend else n.vDescend
        if descend then r - 1 else r + 1
      else r
    }

  // --- Square ---

  private def squareName(g: SquareGridDef, n: Numbering, boardSize: Option[(Int, Int)], p: (Int, Int)): Option[String] =
    val rawCol = math.floor((p._1 - g.x0) / g.dx + 0.5).toInt
    val rawRow = ((p._2 - g.y0) / g.dy + 0.5).toInt // (int) cast: truncate toward zero
    def maxRows = boardSize.map { case (_, h) => (h / g.dy + 0.5).toInt }
    def maxColumns = boardSize.map { case (w, _) => (w / g.dx + 0.5).toInt }

    val colOpt = if n.hDescend then maxColumns.map(_ - rawCol) else Some(rawCol)
    val rowOpt = if n.vDescend then maxRows.map(_ - rawRow) else Some(rawRow)
    for col <- colOpt; row <- rowOpt yield format(n, col, row)

  // --- Shared formatting (RegularGridNumbering.getName) ---

  private def format(n: Numbering, col: Int, row: Int): String =
    // Mirrors RegularGridNumbering.locationName: the $column$/$row$ properties
    // are the offset-adjusted, formatted names, and $gridLocation$ is them
    // joined in the configured order. The numbering's locationFormat (usually
    // "$gridLocation$", but e.g. "Turn $column$" on a track) wraps them.
    val colName = name(col + n.hOff, n.hType, n.hLeading)
    val rowName = name(row + n.vOff, n.vType, n.vLeading)
    val gridLocation = if n.first == 'H' then colName + n.sep + rowName else rowName + n.sep + colName
    n.locationFormat
      .replace("$gridLocation$", gridLocation)
      .replace("$row$", rowName)
      .replace("$column$", colName)

  private val Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

  /** RegularGridNumbering.getName(int, type, leading). */
  private def name(rowOrColumn: Int, kind: Char, leading: Int): String =
    val sign = if rowOrColumn < 0 then "-" else ""
    var v = math.abs(rowOrColumn)
    if kind == 'A' then
      val sb = new StringBuilder(sign)
      var continue = true
      while continue do
        sb.append(Alphabet.charAt(v % 26))
        v -= 26
        continue = v >= 0
      sb.toString
    else
      val sb = new StringBuilder(sign)
      var lead = leading
      while lead > 0 && v < math.pow(10.0, lead) do
        sb.append('0')
        lead -= 1
      sb.append(v)
      sb.toString
