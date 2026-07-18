package vassal.core.io

/** Pure-Scala CRC-32 (ISO 3309 / ITU-T V.42, the same polynomial as
  * `java.util.zip.CRC32` and the CRC used by the ZIP format itself),
  * implemented without any JVM-only APIs so it cross-compiles to Scala.js.
  */
object Crc32 {
  private val table: Array[Int] = {
    val t = new Array[Int](256)
    var n = 0
    while (n < 256) {
      var c = n
      var k = 0
      while (k < 8) {
        c = if ((c & 1) != 0) 0xedb88320 ^ (c >>> 1) else c >>> 1
        k += 1
      }
      t(n) = c
      n += 1
    }
    t
  }

  /** @return the CRC-32 of `data`. */
  def compute(data: Array[Byte]): Long = compute(data, 0, data.length)

  def compute(data: Array[Byte], offset: Int, length: Int): Long = {
    var crc = 0xffffffff
    var i = offset
    val end = offset + length
    while (i < end) {
      crc = table((crc ^ data(i)) & 0xff) ^ (crc >>> 8)
      i += 1
    }
    (crc ^ 0xffffffff) & 0xffffffffL
  }
}
