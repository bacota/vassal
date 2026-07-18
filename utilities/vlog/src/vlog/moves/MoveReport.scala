package vlog.moves

/** Parses a VASSAL move-report chat line into (piece, from, to).
  *
  * The near-universal VASSAL movement report format is
  * `$pieceName$ moves $previousLocation$ <arrow> $location$`, usually wrapped
  * in `* ... *` and sometimes prefixed with the player's name in angle
  * brackets, e.g.:
  *
  *   `* Subervie, L'Heritier moves GG17 &rarr; FF18 *`
  *   `* <bacota> Kitakami, Oi moves Japanese Islands -> Marianas Islands *`
  *
  * Both the arrow form (`... moves GG17 &rarr; FF18`) and the prose form
  * (`... moves from GG17 to FF18`) are recognized. We locate the
  * `moves`/`moved` verb, read the piece to its left, and split the remainder
  * into origin and destination on either the arrow or the ` to ` separator.
  * Reports that don't fit (a different format, or a non-move chat line) yield
  * None, and the caller still reports the move from its structural data.
  */
object MoveReport:

  case class Parsed(piece: String, from: String, to: String)

  private val Verbs = Seq(" moves ", " moved ")
  // Arrow spellings VASSAL modules use between the two locations.
  private val Arrows = Seq("&rarr;", "&#8594;", "&#x2192;", "→", "-->", "->", "=>")

  def parse(rawText: String): Option[Parsed] =
    val text = decodeEntities(rawText)

    Verbs.iterator
      .map(v => (v, text.indexOf(v)))
      .find(_._2 >= 0)
      .flatMap { case (verb, vi) =>
        val piece = cleanPiece(text.substring(0, vi))
        splitFromTo(text.substring(vi + verb.length)).collect {
          case (from, to) if from.nonEmpty && to.nonEmpty => Parsed(piece, from, to)
        }
      }

  /** Split the text after the move verb into (from, to), handling both
    * `<from> <arrow> <to>` and `[from] <from> to <to>`. */
  private def splitFromTo(rest: String): Option[(String, String)] =
    Arrows.iterator.map(a => (a, rest.indexOf(a))).find(_._2 >= 0) match
      case Some((arrow, ai)) =>
        Some((stripLeadingFrom(rest.substring(0, ai)), cleanTail(rest.substring(ai + arrow.length))))
      case None =>
        val body = stripLeadingFrom(rest)
        val ti = body.indexOf(" to ")
        if ti >= 0 then Some((body.substring(0, ti).trim, cleanTail(body.substring(ti + 4))))
        else None

  private def stripLeadingFrom(s: String): String =
    val t = s.trim
    if t.startsWith("from ") then t.substring(5).trim else t

  /** Decode the handful of HTML entities that appear in reports, but leave the
    * arrow entities intact so [[parse]] can find them. */
  private def decodeEntities(s: String): String =
    s.replace("&amp;", "&")
      .replace("&lt;", "<")
      .replace("&gt;", ">")
      .replace("&quot;", "\"")
      .replace("&#39;", "'")

  /** Strip the leading `*`, an optional `<player>` prefix, and surrounding
    * whitespace from the piece portion of the report. */
  private def cleanPiece(s: String): String =
    var p = s.trim
    if p.startsWith("*") then p = p.substring(1).trim
    if p.startsWith("<") then
      val close = p.indexOf('>')
      if close >= 0 then p = p.substring(close + 1).trim
    p

  /** Strip a trailing `*` and surrounding whitespace from the destination. */
  private def cleanTail(s: String): String =
    var t = s.trim
    if t.endsWith("*") then t = t.substring(0, t.length - 1).trim
    t
