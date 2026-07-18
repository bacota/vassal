package vlog.format

import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalacheck.Gen
import vlog.Generators

class CodecPropertyTest extends AnyFunSuite with ScalaCheckPropertyChecks:

  // Realistic delimiters; excludes '\\' and '\'' which the escaping scheme
  // reserves (a delimiter that is itself an escape or quote char is degenerate
  // and never used by VASSAL).
  private val delimiter: Gen[Char] = Gen.oneOf('\u001b', '/', '\t', ',', ';', '|', ':', '#', '~', ' ')

  // Field content excludes backslash (see Generators): a trailing backslash is
  // ambiguous with the delimiter escape and not round-trippable by design.
  private def fieldOf(delim: Char): Gen[String] =
    Gen.listOf(Gen.oneOf('a', 'B', '7', delim, '\'', ' ', '/', ';')).map(_.mkString)

  private def fields(delim: Char): Gen[List[String]] =
    Gen.choose(1, 8).flatMap(n => Gen.listOfN(n, fieldOf(delim)))

  test("SequenceCodec.decode is the inverse of encode") {
    forAll(delimiter) { delim =>
      forAll(fields(delim)) { fs =>
        whenever(fs.nonEmpty) {
          assert(SequenceCodec.decode(SequenceCodec.encode(fs, delim), delim).toList == fs)
        }
      }
    }
  }

  test("rawSplit joined by the delimiter reconstructs the original string") {
    forAll(delimiter) { delim =>
      forAll(Gen.listOf(Gen.oneOf('a', delim, '\\', ' ', ';')).map(_.mkString)) { s =>
        assert(SequenceCodec.rawSplit(s, delim).mkString(delim.toString) == s)
      }
    }
  }

  test("rawSplit and decode agree on token count") {
    forAll(delimiter) { delim =>
      forAll(Gen.listOf(Gen.oneOf('a', delim, '\\', ' ')).map(_.mkString)) { s =>
        assert(SequenceCodec.rawSplit(s, delim).size == SequenceCodec.decode(s, delim).size)
      }
    }
  }

  test("Obfuscation.decode is the inverse of encode for any bytes and key") {
    forAll { (bytes: Array[Byte], key: Byte) =>
      assert(Obfuscation.decode(Obfuscation.encode(bytes, key)).sameElements(bytes))
    }
  }

  test("an obfuscated payload is recognized as obfuscated") {
    forAll { (bytes: Array[Byte], key: Byte) =>
      assert(Obfuscation.isObfuscated(Obfuscation.encode(bytes, key)))
    }
  }
