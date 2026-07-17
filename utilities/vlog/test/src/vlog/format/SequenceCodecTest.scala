package vlog.format

import org.scalatest.funsuite.AnyFunSuite

class SequenceCodecTest extends AnyFunSuite:

  private val Esc: Char = 0x1B.toChar

  test("round-trips plain fields and fields containing the delimiter") {
    val fields = Seq("plain", s"has${Esc}delim", "unrelated")
    val encoded = SequenceCodec.encode(fields, Esc)
    assert(SequenceCodec.decode(encoded, Esc) == fields.toVector)
  }

  test("a field starting with a backslash round-trips via quote-wrapping") {
    val fields = Seq("\\leadingBackslash", "plain")
    val encoded = SequenceCodec.encode(fields, ',')
    assert(SequenceCodec.decode(encoded, ',') == fields.toVector)
  }

  test("a field already wrapped in single quotes round-trips") {
    val fields = Seq("'quoted'", "plain")
    val encoded = SequenceCodec.encode(fields, ',')
    assert(SequenceCodec.decode(encoded, ',') == fields.toVector)
  }

  test("a mid-field backslash not adjacent to the delimiter is left untouched") {
    val fields = Seq("back\\slashmid", "plain")
    val encoded = SequenceCodec.encode(fields, ',')
    assert(SequenceCodec.decode(encoded, ',') == fields.toVector)
  }

  test("matches the SequenceEncoder javadoc example: A,B\\,C") {
    // new SequenceEncoder("A",',').append(new SequenceEncoder("B",',').append("C").getValue()).getValue()
    val inner = SequenceCodec.encode(Seq("B", "C"), ',')
    val outer = SequenceCodec.encode(Seq("A", inner), ',')
    assert(outer == "A,B\\,C")
  }

  test("decodes a simple tab-delimited example") {
    assert(SequenceCodec.decode("a\tb\tc", '\t') == Vector("a", "b", "c"))
  }

  test("empty string decodes to a single empty field") {
    assert(SequenceCodec.decode("", '\t') == Vector(""))
  }
