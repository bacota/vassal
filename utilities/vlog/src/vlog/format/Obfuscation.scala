package vlog.format

/** Codec for VASSAL's "obfuscated" byte stream format used inside .vlog/.vsav
  * zip entries (see VASSAL.tools.io.ObfuscatingOutputStream /
  * DeobfuscatingInputStream). Entries written this way begin with the
  * 5-byte ASCII header "!VCSK" followed by a 1-byte XOR key rendered as two
  * lowercase hex characters; every subsequent byte of the real payload is
  * XORed with that key and rendered as two lowercase hex characters.
  */
object Obfuscation:
  private val Header = "!VCSK"

  def isObfuscated(bytes: Array[Byte]): Boolean =
    bytes.length >= Header.length + 2 &&
      new String(bytes, 0, Header.length, "US-ASCII") == Header &&
      ((bytes.length - Header.length - 2) % 2 == 0)

  /** Decode an obfuscated entry back to its plaintext bytes.
    * If the header is absent, the bytes are returned unchanged, mirroring
    * DeobfuscatingInputStream's fallback behavior for older/plain entries.
    */
  def decode(bytes: Array[Byte]): Array[Byte] =
    if !isObfuscated(bytes) then bytes
    else
      val keyHex = new String(bytes, Header.length, 2, "US-ASCII")
      val key = Integer.parseInt(keyHex, 16).toByte
      val hexBody =
        new String(bytes, Header.length + 2, bytes.length - Header.length - 2, "US-ASCII")
      val out = new Array[Byte](hexBody.length / 2)
      var i = 0
      while i < out.length do
        val hi = Character.digit(hexBody.charAt(i * 2), 16)
        val lo = Character.digit(hexBody.charAt(i * 2 + 1), 16)
        if hi < 0 || lo < 0 then
          throw new IllegalArgumentException("Invalid hex in obfuscated body")
        out(i) = (((hi << 4) | lo) ^ key).toByte
        i += 1
      out

  /** Encode plaintext bytes into the obfuscated on-disk form, using the
    * given single-byte XOR key.
    */
  def encode(bytes: Array[Byte], key: Byte): Array[Byte] =
    val sb = new StringBuilder(Header)
    sb.append(f"${key & 0xff}%02x")
    for b <- bytes do
      sb.append(f"${(b ^ key) & 0xff}%02x")
    sb.toString.getBytes("US-ASCII")
