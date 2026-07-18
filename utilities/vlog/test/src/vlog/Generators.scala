package vlog

import org.scalacheck.Gen
import vlog.command.*

/** Shared ScalaCheck generators for the property-based tests. */
object Generators:

  /** Content characters that exercise the codecs' delimiter-escaping (the
    * field separators `/` and `\t`) and single-quote quoting. Excludes ESC
    * (0x1B), the command-tree separator that must not appear inside a leaf,
    * and backslash: VASSAL's SequenceEncoder never escapes a lone backslash,
    * so a field ending in one is genuinely not round-trippable — a faithful
    * limitation of the format, not something these tests should assert against. */
  private val contentChar: Gen[Char] =
    Gen.oneOf(('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++
      Seq('/', ' ', '\t', '\'', ';', ',', '-', '.'))

  /** A short string of content characters (possibly empty). */
  val text: Gen[String] =
    Gen.choose(0, 12).flatMap(n => Gen.listOfN(n, contentChar)).map(_.mkString)

  /** A string that is never the literal "null" (VASSAL's absent-value
    * sentinel), safe to place in a null-wrapped optional field. */
  private val nonSentinel: Gen[String] = text.map(s => if s == "null" then s + "." else s)
  private val optField: Gen[Option[String]] = Gen.option(nonSentinel)
  private val coord: Gen[Int] = Gen.choose(-100000, 100000)

  private val genAdd: Gen[Command] =
    for id <- text; pt <- text; st <- text yield AddPieceCmd(id, pt, st)
  private val genRemove: Gen[Command] = text.map(RemovePieceCmd(_))
  private val genChange: Gen[Command] =
    for id <- text; ns <- text; os <- Gen.option(text) yield ChangePieceCmd(id, ns, os)
  private val genMove: Gen[Command] =
    for
      id <- text
      nm <- optField; nx <- coord; ny <- coord; nu <- optField
      om <- optField; ox <- coord; oy <- coord; ou <- optField
      pl <- optField
    yield MovePieceCmd(id, nm, nx, ny, nu, om, ox, oy, ou, pl)
  private val genChat: Gen[Command] = text.map(DisplayTextCmd(_))
  private val genAudio: Gen[Command] = text.map(PlayAudioClipCmd(_))
  private val genUndo: Gen[Command] = Gen.oneOf(true, false).map(UndoCmd(_))

  /** A single leaf command that round-trips through CommandCodec. Excludes
    * UnknownCmd (whose raw form may re-parse as another type). */
  val leafCommand: Gen[Command] =
    Gen.oneOf(genAdd, genRemove, genChange, genMove, genChat, genAudio, genUndo, Gen.const(NullCmd))

  val leafCommands: Gen[List[Command]] =
    Gen.choose(1, 8).flatMap(n => Gen.listOfN(n, leafCommand))
