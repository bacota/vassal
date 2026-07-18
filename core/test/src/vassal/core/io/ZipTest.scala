package vassal.core.io

class ZipTest extends munit.FunSuite {

  test("Crc32 matches known value for empty and simple inputs") {
    assertEquals(Crc32.compute(Array.emptyByteArray), 0L)
    // "123456789" is the standard CRC-32 check value test vector.
    assertEquals(Crc32.compute("123456789".getBytes("UTF-8")), 0xcbf43926L)
  }

  test("Zip.write then Zip.read round-trips entries") {
    val entries = Seq(
      ZipEntry("buildFile.xml", "<hello>world</hello>".getBytes("UTF-8")),
      ZipEntry("images/foo.png", Array.tabulate(500)(i => (i % 256).toByte)),
      ZipEntry("empty.txt", Array.emptyByteArray)
    )

    val bytes = Zip.write(entries)
    val readBack = Zip.read(bytes)

    assertEquals(readBack.map(_.name), entries.map(_.name))
    for ((expected, actual) <- entries.zip(readBack)) {
      assertEquals(actual.data.toSeq, expected.data.toSeq)
    }
  }

  test("Inflater decompresses real DEFLATE data produced by java.util.zip") {
    // Only java.util.zip is available on the JVM test backend; this validates
    // our from-scratch Inflater against the reference implementation the
    // legacy app actually uses to write .vmod/.vsav/.vlog files.
    val original =
      ("The quick brown fox jumps over the lazy dog. " * 50).getBytes("UTF-8")

    val deflater = new java.util.zip.Deflater(java.util.zip.Deflater.DEFAULT_COMPRESSION, true)
    deflater.setInput(original)
    deflater.finish()
    val buf = new Array[Byte](original.length * 2 + 64)
    val compressedLen = deflater.deflate(buf)
    val compressed = java.util.Arrays.copyOf(buf, compressedLen)

    val decompressed = Inflater.inflate(compressed, original.length)
    assertEquals(decompressed.toSeq, original.toSeq)
  }

  test("Zip.read parses a real DEFLATE-compressed zip written by java.util.zip") {
    val name = "buildFile.xml"
    val content = ("<VASSAL.build.GameModule>" + ("x" * 5000) + "</VASSAL.build.GameModule>")
      .getBytes("UTF-8")

    val baos = new java.io.ByteArrayOutputStream()
    val zos = new java.util.zip.ZipOutputStream(baos)
    zos.putNextEntry(new java.util.zip.ZipEntry(name))
    zos.write(content)
    zos.closeEntry()
    zos.close()

    val entries = Zip.read(baos.toByteArray)
    assertEquals(entries.length, 1)
    assertEquals(entries.head.name, name)
    assertEquals(entries.head.data.toSeq, content.toSeq)
  }
}
