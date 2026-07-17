package vlog.format

import org.scalatest.funsuite.AnyFunSuite

class SequenceCodecTest extends AnyFunSuite:

  private val Esc: Char = 0x1B.toChar

  test("round-trips fields containing the delimiter and backslashes") {
    val fields = Seq("plain", s"has${Esc}delim", "backslash\\here", "quote'ish")
    val encoded = SequenceCodec.encode(fields, Esc)
    assert(SequenceCodec.decode(encoded, Esc) == fields.toVector)
  }

  test("decodes a simple tab-delimited example") {
    assert(SequenceCodec.decode("a\tb\tc", '\t') == Vector("a", "b", "c"))
  }

  test("empty string decodes to a single empty field") {
    assert(SequenceCodec.decode("", '\t') == Vector(""))
  }
