package vassal.core.command

class SequenceEncoderTest extends munit.FunSuite {

  test("simple round trip, no escaping needed") {
    val se = new SequenceEncoder(',')
    se.append("A").append("B").append("C")
    assertEquals(se.getValue, "A,B,C")

    val dec = new SequenceEncoder.Decoder(se.getValue, ',')
    assertEquals(dec.nextToken(), "A")
    assertEquals(dec.nextToken(), "B")
    assertEquals(dec.nextToken(), "C")
    assertEquals(dec.hasMoreTokens, false)
  }

  test("delimiter inside an element is escaped and recovered") {
    val se = new SequenceEncoder(',')
    se.append("A").append("B,C")
    assertEquals(se.getValue, "A,B\\,C")

    val dec = new SequenceEncoder.Decoder(se.getValue, ',')
    assertEquals(dec.nextToken(), "A")
    assertEquals(dec.nextToken(), "B,C")
    assertEquals(dec.hasMoreTokens, false)
  }

  test("nested SequenceEncoders, matching the class doc's own example") {
    val inner = new SequenceEncoder("B", ',').append("C").getValue
    val outer = new SequenceEncoder("A", ',').append(inner).getValue
    assertEquals(outer, "A,B\\,C")
  }

  test("a value starting with a backslash is quote-wrapped") {
    val se = new SequenceEncoder(',')
    se.append("\\weird")
    val encoded = se.getValue
    val dec = new SequenceEncoder.Decoder(encoded, ',')
    assertEquals(dec.nextToken(), "\\weird")
  }

  test("a value already wrapped in single quotes round-trips unchanged") {
    val se = new SequenceEncoder(',')
    se.append("'quoted'")
    val dec = new SequenceEncoder.Decoder(se.getValue, ',')
    assertEquals(dec.nextToken(), "'quoted'")
  }

  test("empty and null-ish tokens") {
    // Consecutive delimiters decode to an empty-string token.
    val dec = new SequenceEncoder.Decoder("A,,B", ',')
    assertEquals(dec.nextToken(), "A")
    assertEquals(dec.nextToken(), "")
    assertEquals(dec.nextToken(), "B")
    assertEquals(dec.hasMoreTokens, false)
  }

  test("decoding an empty string yields a single empty token") {
    val dec = new SequenceEncoder.Decoder("", ',')
    assertEquals(dec.hasMoreTokens, true)
    assertEquals(dec.nextToken(), "")
    assertEquals(dec.hasMoreTokens, false)
  }

  test("getRemaining returns the unconsumed tail") {
    val dec = new SequenceEncoder.Decoder("A,B,C", ',')
    dec.nextToken()
    assertEquals(dec.getRemaining, "B,C")
  }

  test("appended primitives encode via their string forms") {
    val se = new SequenceEncoder(',')
    se.append(42).append(true).append(3.5)
    assertEquals(se.getValue, "42,true,3.5")
  }

  for (
    resource <- Seq("BritishTurn2.reencoded.txt", "BritishTurn7Combat.reencoded.txt")
  ) {
    test(s"tokenizing and rejoining real production data ($resource) is lossless") {
      val is = getClass.getResourceAsStream(s"/commands/$resource")
      require(is != null, s"missing test resource $resource")
      val original = new String(is.readAllBytes(), "UTF-8")

      val commandSeparator = 27.toChar // ASCII ESC, matches GameModule.COMMAND_SEPARATOR
      val dec = new SequenceEncoder.Decoder(original, commandSeparator)
      val tokens = collection.mutable.ArrayBuffer.empty[String]
      while (dec.hasMoreTokens) tokens += dec.nextToken()

      assert(tokens.length > 1, "expected a real multi-command save/log to split into >1 top-level tokens")

      val se = new SequenceEncoder(commandSeparator)
      for (t <- tokens) se.append(t)

      assertEquals(se.getValue, original)
    }
  }
}
