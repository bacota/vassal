package vlog.module

import vlog.command.*
import vlog.format.VLogFile
import java.nio.file.Path
import scala.util.matching.Regex

/** Checks [[GridMath]] against ground truth a .vlog carries about itself.
  *
  * Wherever a piece stops, VASSAL stores the location name it computed
  * (`OldLocationName`) beside the pixel coordinates (`OldX`/`OldY`), board and
  * map in the piece's state. Reproducing the same name from the same point is
  * an exact, module-supplied test of the geometry port — no external oracle
  * needed. Only samples recorded on `mapName` are checked.
  */
object GridValidation:

  final case class Result(
      samples: Int,
      matched: Int,
      mismatched: Int,
      unresolved: Int,
      examples: Vector[String]
  ):
    def ok: Boolean = mismatched == 0

  private val Pattern: Regex =
    raw"OldLocationName;([^;]*);.*?OldX;(-?\d+);OldY;(-?\d+);OldBoard;([^;]*);OldMap;([^,)\t]*)".r

  def validate(resolver: LocationResolver, vlogPath: Path, mapName: String): Result =
    val commandString = VLogFile.read(vlogPath).commandString
    val samples = CommandTree
      .flatten(commandString)
      .collect { case LogCmd(ChangePieceCmd(_, s, _)) => s; case ChangePieceCmd(_, s, _) => s }
      .flatMap(state => Pattern.findAllMatchIn(state))
      .map(m => (m.group(1), m.group(2).toInt, m.group(3).toInt, m.group(4), m.group(5)))
      .distinct
      .filter(_._5 == mapName)

    var matched, mismatched, unresolved = 0
    val examples = Vector.newBuilder[String]
    samples.foreach { case (expected, x, y, board, _) =>
      resolver.locationName(mapName, (x, y), Some(board).filter(_.nonEmpty)) match
        case Right(name) if name == expected => matched += 1
        case Right(name) =>
          mismatched += 1
          if mismatched <= 10 then examples += s"($x,$y) expected '$expected' got '$name'"
        case Left(_) => unresolved += 1
    }
    Result(samples.size, matched, mismatched, unresolved, examples.result())
