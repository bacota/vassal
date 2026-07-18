package vassal.core.io

/** Pure-Scala implementation of raw DEFLATE decompression (RFC 1951), the
  * compression method ("method 8") ZIP entries use. No JVM-only APIs, so
  * this cross-compiles to Scala.js -- browsers have no `java.util.zip`.
  *
  * Only decompression is implemented for now; ZipWriter (see Zip.scala)
  * currently writes STORED (uncompressed) entries, which are equally valid
  * ZIP entries and openable by any reader, including the legacy JVM app.
  */
object Inflater {

  private final class BitReader(data: Array[Byte]) {
    var bytePos: Int = 0
    var bitPos: Int = 0

    def readBit(): Int = {
      val b = (data(bytePos) >>> bitPos) & 1
      bitPos += 1
      if (bitPos == 8) { bitPos = 0; bytePos += 1 }
      b
    }

    /** DEFLATE packs most multi-bit values LSB-first. */
    def readBits(n: Int): Int = {
      var v = 0
      var i = 0
      while (i < n) {
        v |= readBit() << i
        i += 1
      }
      v
    }

    /** Huffman codes are packed MSB-first (RFC 1951 section 3.1.1). */
    def readHuffmanBit(): Int = readBit()

    def alignToByte(): Unit =
      if (bitPos != 0) { bitPos = 0; bytePos += 1 }

    def readByte(): Int = {
      val v = data(bytePos) & 0xff
      bytePos += 1
      v
    }
  }

  /** Canonical Huffman decode table built from per-symbol code lengths,
    * per RFC 1951 section 3.2.2.
    */
  private final class HuffmanTable(codeLengths: Array[Int]) {
    private val maxBits = if (codeLengths.isEmpty) 0 else codeLengths.max
    private val codeToSymbol = scala.collection.mutable.Map.empty[(Int, Int), Int]

    {
      val blCount = new Array[Int](maxBits + 1)
      for (l <- codeLengths if l > 0) blCount(l) += 1

      val nextCode = new Array[Int](maxBits + 1)
      var code = 0
      var bits = 1
      while (bits <= maxBits) {
        code = (code + blCount(bits - 1)) << 1
        nextCode(bits) = code
        bits += 1
      }

      for (sym <- codeLengths.indices) {
        val len = codeLengths(sym)
        if (len > 0) {
          codeToSymbol((len, nextCode(len))) = sym
          nextCode(len) += 1
        }
      }
    }

    def decode(br: BitReader): Int = {
      var code = 0
      var len = 0
      while (len < 32) {
        code = (code << 1) | br.readHuffmanBit()
        len += 1
        codeToSymbol.get((len, code)) match {
          case Some(sym) => return sym
          case None      => ()
        }
      }
      throw new IllegalStateException("Invalid Huffman code in DEFLATE stream")
    }
  }

  private val lengthBase = Array(
    3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31,
    35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258
  )
  private val lengthExtraBits = Array(
    0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2,
    3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0
  )
  private val distBase = Array(
    1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
    257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145,
    8193, 12289, 16385, 24577
  )
  private val distExtraBits = Array(
    0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6,
    7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13
  )
  private val codeLengthOrder =
    Array(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15)

  private val fixedLitLenTable: HuffmanTable = {
    val lens = new Array[Int](288)
    for (i <- 0 until 144) lens(i) = 8
    for (i <- 144 until 256) lens(i) = 9
    for (i <- 256 until 280) lens(i) = 7
    for (i <- 280 until 288) lens(i) = 8
    new HuffmanTable(lens)
  }

  private val fixedDistTable: HuffmanTable =
    new HuffmanTable(Array.fill(30)(5))

  /** A growable byte buffer supporting the random-access reads back-references need. */
  private final class OutBuffer(initialCapacity: Int) {
    private var buf = new Array[Byte](math.max(64, initialCapacity))
    private var len = 0

    private def ensure(extra: Int): Unit =
      if (len + extra > buf.length) {
        var newCap = buf.length * 2
        while (newCap < len + extra) newCap *= 2
        val nb = new Array[Byte](newCap)
        System.arraycopy(buf, 0, nb, 0, len)
        buf = nb
      }

    def writeByte(b: Byte): Unit = {
      ensure(1)
      buf(len) = b
      len += 1
    }

    def copyBack(distance: Int, length: Int): Unit = {
      ensure(length)
      var i = 0
      while (i < length) {
        buf(len) = buf(len - distance)
        len += 1
        i += 1
      }
    }

    def toArray: Array[Byte] = java.util.Arrays.copyOf(buf, len)
  }

  /** Inflates a raw (headerless) DEFLATE stream, e.g. a ZIP entry's payload.
    * @param expectedSize hint for output-buffer sizing (the uncompressed
    *                      size from the ZIP entry header); not load-bearing.
    */
  def inflate(data: Array[Byte], expectedSize: Int = 0): Array[Byte] = {
    val br = new BitReader(data)
    val out = new OutBuffer(expectedSize)

    var finalBlock = false
    while (!finalBlock) {
      finalBlock = br.readBits(1) == 1
      val blockType = br.readBits(2)

      blockType match {
        case 0 => // stored
          br.alignToByte()
          val len = br.readByte() | (br.readByte() << 8)
          br.readByte(); br.readByte() // NLEN, ignored
          var i = 0
          while (i < len) { out.writeByte(br.readByte().toByte); i += 1 }

        case 1 => // fixed Huffman
          inflateBlock(br, out, fixedLitLenTable, fixedDistTable)

        case 2 => // dynamic Huffman
          val (litLenTable, distTable) = readDynamicTables(br)
          inflateBlock(br, out, litLenTable, distTable)

        case _ =>
          throw new IllegalStateException(s"Invalid DEFLATE block type $blockType")
      }
    }

    out.toArray
  }

  private def readDynamicTables(br: BitReader): (HuffmanTable, HuffmanTable) = {
    val hlit = br.readBits(5) + 257
    val hdist = br.readBits(5) + 1
    val hclen = br.readBits(4) + 4

    val clCodeLengths = new Array[Int](19)
    for (i <- 0 until hclen) clCodeLengths(codeLengthOrder(i)) = br.readBits(3)
    val clTable = new HuffmanTable(clCodeLengths)

    val allLengths = new Array[Int](hlit + hdist)
    var i = 0
    while (i < allLengths.length) {
      val sym = clTable.decode(br)
      if (sym < 16) {
        allLengths(i) = sym
        i += 1
      }
      else if (sym == 16) {
        val repeat = br.readBits(2) + 3
        val prev = allLengths(i - 1)
        var r = 0
        while (r < repeat) { allLengths(i) = prev; i += 1; r += 1 }
      }
      else if (sym == 17) {
        val repeat = br.readBits(3) + 3
        var r = 0
        while (r < repeat) { allLengths(i) = 0; i += 1; r += 1 }
      }
      else { // 18
        val repeat = br.readBits(7) + 11
        var r = 0
        while (r < repeat) { allLengths(i) = 0; i += 1; r += 1 }
      }
    }

    val litLenLengths = allLengths.slice(0, hlit)
    val distLengths = allLengths.slice(hlit, hlit + hdist)
    (new HuffmanTable(litLenLengths), new HuffmanTable(distLengths))
  }

  private def inflateBlock(
    br: BitReader,
    out: OutBuffer,
    litLenTable: HuffmanTable,
    distTable: HuffmanTable
  ): Unit = {
    var done = false
    while (!done) {
      val sym = litLenTable.decode(br)
      if (sym < 256) {
        out.writeByte(sym.toByte)
      }
      else if (sym == 256) {
        done = true
      }
      else {
        val idx = sym - 257
        val length = lengthBase(idx) + br.readBits(lengthExtraBits(idx))
        val distSym = distTable.decode(br)
        val distance = distBase(distSym) + br.readBits(distExtraBits(distSym))
        out.copyBack(distance, length)
      }
    }
  }
}
