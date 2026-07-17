package vassal.core.io

import java.nio.charset.StandardCharsets

/** One decoded entry from a ZIP archive: its name and uncompressed bytes. */
final case class ZipEntry(name: String, data: Array[Byte])

/** Reads and writes ZIP archives (the container format for `.vmod`/`.vmdx`
  * modules and `.vsav`/`.vlog` saves/logs) without `java.util.zip`, which
  * doesn't exist under Scala.js. Parses the ZIP central directory directly
  * and decompresses DEFLATE-method entries with [[Inflater]]; STORED-method
  * entries are copied as-is.
  *
  * `ZipWriter` currently only emits STORED entries. That produces larger
  * files than the legacy app's DEFLATE-compressed output but is a fully
  * valid, standard ZIP archive that `java.util.zip` (and this reader) opens
  * without issue -- byte-identical output isn't required for compatibility,
  * only mutual openability (see ARCHITECTURE_NOTES.md / notes.txt risk #2).
  */
object Zip {

  private val LOCAL_FILE_HEADER_SIG = 0x04034b50
  private val CENTRAL_DIR_SIG = 0x02014b50
  private val EOCD_SIG = 0x06054b50

  private def u8(b: Array[Byte], off: Int): Int = b(off) & 0xff
  private def u16(b: Array[Byte], off: Int): Int = u8(b, off) | (u8(b, off + 1) << 8)
  private def u32(b: Array[Byte], off: Int): Long =
    (u8(b, off).toLong | (u8(b, off + 1).toLong << 8) |
      (u8(b, off + 2).toLong << 16) | (u8(b, off + 3).toLong << 24)) & 0xffffffffL

  /** Reads every entry out of a complete ZIP archive's bytes. */
  def read(bytes: Array[Byte]): Seq[ZipEntry] = {
    val eocdOffset = findEocd(bytes)
    val centralDirOffset = u32(bytes, eocdOffset + 16).toInt
    val entryCount = u16(bytes, eocdOffset + 10)

    val entries = new scala.collection.mutable.ArrayBuffer[ZipEntry](entryCount)
    var pos = centralDirOffset
    var i = 0
    while (i < entryCount) {
      require(
        u32(bytes, pos).toInt == CENTRAL_DIR_SIG,
        s"Expected central directory header at $pos"
      )
      val method = u16(bytes, pos + 10)
      val compressedSize = u32(bytes, pos + 20).toInt
      val uncompressedSize = u32(bytes, pos + 24).toInt
      val nameLen = u16(bytes, pos + 28)
      val extraLen = u16(bytes, pos + 30)
      val commentLen = u16(bytes, pos + 32)
      val localHeaderOffset = u32(bytes, pos + 42).toInt
      val name = new String(bytes, pos + 46, nameLen, StandardCharsets.UTF_8)

      entries += readEntry(bytes, localHeaderOffset, method, compressedSize, uncompressedSize, name)

      pos += 46 + nameLen + extraLen + commentLen
      i += 1
    }
    entries.toSeq
  }

  private def readEntry(
    bytes: Array[Byte],
    localHeaderOffset: Int,
    method: Int,
    compressedSize: Int,
    uncompressedSize: Int,
    name: String
  ): ZipEntry = {
    require(
      u32(bytes, localHeaderOffset).toInt == LOCAL_FILE_HEADER_SIG,
      s"Expected local file header at $localHeaderOffset for entry $name"
    )
    val lfhNameLen = u16(bytes, localHeaderOffset + 26)
    val lfhExtraLen = u16(bytes, localHeaderOffset + 28)
    val dataStart = localHeaderOffset + 30 + lfhNameLen + lfhExtraLen

    val compressed = java.util.Arrays.copyOfRange(bytes, dataStart, dataStart + compressedSize)

    val data = method match {
      case 0 => compressed // STORED
      case 8 => Inflater.inflate(compressed, uncompressedSize) // DEFLATE
      case other =>
        throw new UnsupportedOperationException(
          s"Unsupported ZIP compression method $other for entry $name"
        )
    }

    require(
      data.length == uncompressedSize,
      s"Decompressed size mismatch for $name: expected $uncompressedSize, got ${data.length}"
    )

    ZipEntry(name, data)
  }

  private def findEocd(bytes: Array[Byte]): Int = {
    // EOCD is at least 22 bytes, and can be followed by up to 65535 bytes of comment.
    val minPos = math.max(0, bytes.length - 22 - 65535)
    var pos = bytes.length - 22
    while (pos >= minPos) {
      if (u32(bytes, pos).toInt == EOCD_SIG) return pos
      pos -= 1
    }
    throw new IllegalArgumentException("Not a ZIP archive: no end-of-central-directory record found")
  }

  /** Builds a valid ZIP archive's bytes from `entries`, in order, all STORED (uncompressed). */
  def write(entries: Seq[ZipEntry]): Array[Byte] = {
    val out = new java.io.ByteArrayOutputStream()
    val centralDirectory = new java.io.ByteArrayOutputStream()
    val localHeaderOffsets = new Array[Int](entries.length)

    for ((entry, idx) <- entries.zipWithIndex) {
      localHeaderOffsets(idx) = out.size()
      val nameBytes = entry.name.getBytes(StandardCharsets.UTF_8)
      val crc = Crc32.compute(entry.data)

      writeLE32(out, LOCAL_FILE_HEADER_SIG)
      writeLE16(out, 20) // version needed
      writeLE16(out, 1 << 11) // general purpose flag: UTF-8 names
      writeLE16(out, 0) // method: stored
      writeLE16(out, 0) // mod time
      writeLE16(out, 0x21) // mod date (1980-01-01, the DOS epoch)
      writeLE32(out, crc)
      writeLE32(out, entry.data.length)
      writeLE32(out, entry.data.length)
      writeLE16(out, nameBytes.length)
      writeLE16(out, 0) // extra length
      out.write(nameBytes)
      out.write(entry.data)
    }

    for ((entry, idx) <- entries.zipWithIndex) {
      val nameBytes = entry.name.getBytes(StandardCharsets.UTF_8)
      val crc = Crc32.compute(entry.data)

      writeLE32(centralDirectory, CENTRAL_DIR_SIG)
      writeLE16(centralDirectory, 20) // version made by
      writeLE16(centralDirectory, 20) // version needed
      writeLE16(centralDirectory, 1 << 11)
      writeLE16(centralDirectory, 0) // method: stored
      writeLE16(centralDirectory, 0)
      writeLE16(centralDirectory, 0x21)
      writeLE32(centralDirectory, crc)
      writeLE32(centralDirectory, entry.data.length)
      writeLE32(centralDirectory, entry.data.length)
      writeLE16(centralDirectory, nameBytes.length)
      writeLE16(centralDirectory, 0) // extra length
      writeLE16(centralDirectory, 0) // comment length
      writeLE16(centralDirectory, 0) // disk number start
      writeLE16(centralDirectory, 0) // internal attributes
      writeLE32(centralDirectory, 0) // external attributes
      writeLE32(centralDirectory, localHeaderOffsets(idx))
      centralDirectory.write(nameBytes)
    }

    val centralDirOffset = out.size()
    val centralDirBytes = centralDirectory.toByteArray
    out.write(centralDirBytes)

    writeLE32(out, EOCD_SIG)
    writeLE16(out, 0) // disk number
    writeLE16(out, 0) // central dir start disk
    writeLE16(out, entries.length)
    writeLE16(out, entries.length)
    writeLE32(out, centralDirBytes.length)
    writeLE32(out, centralDirOffset)
    writeLE16(out, 0) // comment length

    out.toByteArray
  }

  private def writeLE16(out: java.io.ByteArrayOutputStream, v: Int): Unit = {
    out.write(v & 0xff)
    out.write((v >>> 8) & 0xff)
  }

  private def writeLE32(out: java.io.ByteArrayOutputStream, v: Long): Unit = {
    out.write((v & 0xff).toInt)
    out.write(((v >>> 8) & 0xff).toInt)
    out.write(((v >>> 16) & 0xff).toInt)
    out.write(((v >>> 24) & 0xff).toInt)
  }
}
