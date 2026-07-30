/*
 *
 * Copyright (c) 2000-2024 by VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License (LGPL) as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, copies are available
 * at http://www.opensource.org.
 */
package VASSAL.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Standalone command-line utility that reads a {@code .vlog} file and prints
 * a string representation of each command contained in the vlog file to stdout.
 *
 * <p>Usage: {@code java VASSAL.tools.VlogDumper <vlog-file>}</p>
 *
 * <p>This class has no dependencies on VASSAL module infrastructure.
 * All deobfuscation logic is implemented inline.</p>
 */
public class VlogDumper {

  /** Name of the ZIP entry inside a vlog/save file that holds the game state. */
  private static final String SAVEFILE_ZIP_ENTRY = "savedGame"; //NON-NLS

  /** Header that marks an obfuscated stream produced by ObfuscatingOutputStream. */
  private static final String OBFUSCATION_HEADER = "!VCSK"; //NON-NLS

  /** Separator between individual serialized commands in the save string. */
  private static final char COMMAND_SEPARATOR = '\u001B'; // escape char, decimal 27

  public static void main(String[] args) {
    if (args.length != 1) {
      System.err.println("Usage: java VASSAL.tools.VlogDumper <vlog-file>"); //NON-NLS
      System.exit(1);
    }

    final Path vlogPath = Path.of(args[0]);
    if (!Files.exists(vlogPath)) {
      System.err.println("Error: File not found: " + args[0]); //NON-NLS
      System.exit(1);
    }

    try {
      final String content = readSavedGame(vlogPath);
      final String[] commands = content.split(String.valueOf(COMMAND_SEPARATOR), -1);
      for (int i = 0; i < commands.length; i++) {
        System.out.println("Command " + (i + 1) + ": " + commands[i]); //NON-NLS
      }
    }
    catch (IOException e) {
      System.err.println("Error: " + e.getMessage()); //NON-NLS
      System.exit(1);
    }
  }

  /**
   * Opens the vlog file as a ZIP archive, locates the {@code savedGame} entry,
   * deobfuscates it if necessary, and returns the raw command string.
   *
   * @param vlogPath path to the vlog file
   * @return the deobfuscated command string
   * @throws IOException if the file cannot be read or contains no {@code savedGame} entry
   */
  static String readSavedGame(Path vlogPath) throws IOException {
    try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(vlogPath))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if (SAVEFILE_ZIP_ENTRY.equals(entry.getName())) {
          final byte[] raw = readAllBytes(zis);
          return deobfuscate(raw);
        }
      }
    }
    throw new IOException("No 'savedGame' entry found in vlog file"); //NON-NLS
  }

  /**
   * Reads all available bytes from the given stream without closing it
   * (safe to use on a {@link ZipInputStream} positioned at an entry).
   */
  private static byte[] readAllBytes(InputStream in) throws IOException {
    final ByteArrayOutputStream buf = new ByteArrayOutputStream();
    final byte[] tmp = new byte[8192];
    int n;
    while ((n = in.read(tmp)) != -1) {
      buf.write(tmp, 0, n);
    }
    return buf.toByteArray();
  }

  /**
   * Deobfuscates a byte array that may or may not be prefixed with the
   * {@code !VCSK} obfuscation header.
   *
   * <p>Obfuscation format (from {@code ObfuscatingOutputStream}):
   * <ol>
   *   <li>5-byte ASCII header {@code !VCSK}</li>
   *   <li>2 hex chars encoding the XOR key byte</li>
   *   <li>Remaining content as pairs of hex chars, each pair XOR'd with the key</li>
   * </ol>
   * If the header is absent the bytes are treated as plain UTF-8 text.</p>
   *
   * @param raw the raw bytes read from the ZIP entry
   * @return the deobfuscated string
   * @throws IOException if the obfuscated data is malformed
   */
  static String deobfuscate(byte[] raw) throws IOException {
    final String header = OBFUSCATION_HEADER;
    final int headerLen = header.length();

    // Check whether the header is present
    if (raw.length >= headerLen) {
      final String prefix = new String(raw, 0, headerLen, StandardCharsets.UTF_8);
      if (header.equals(prefix)) {
        // Next 2 bytes are the hex-encoded key
        if (raw.length < headerLen + 2) {
          throw new IOException("Obfuscated data is too short to contain key"); //NON-NLS
        }
        final byte key = (byte) ((unhex(raw[headerLen]) << 4) | unhex(raw[headerLen + 1]));

        // The remaining bytes are hex pairs XOR'd with the key
        final int dataStart = headerLen + 2;
        final int dataLen = raw.length - dataStart;
        if (dataLen % 2 != 0) {
          throw new IOException("Obfuscated data has odd number of hex characters"); //NON-NLS
        }
        final byte[] decoded = new byte[dataLen / 2];
        for (int i = 0; i < decoded.length; i++) {
          decoded[i] = (byte) (((unhex(raw[dataStart + i * 2]) << 4)
            | unhex(raw[dataStart + i * 2 + 1])) ^ key);
        }
        return new String(decoded, StandardCharsets.UTF_8);
      }
    }

    // No obfuscation header — treat as plain UTF-8
    return new String(raw, StandardCharsets.UTF_8);
  }

  /**
   * Converts a single ASCII hex character byte to its 0-15 integer value.
   *
   * @param b the byte (character) to convert
   * @return integer value 0-15
   * @throws IOException if the byte is not a valid hex character
   */
  private static int unhex(byte b) throws IOException {
    if (b >= 0x30 && b <= 0x39) return b - 0x30;       // '0'-'9'
    if (b >= 0x41 && b <= 0x46) return b - 0x37;       // 'A'-'F'
    if (b >= 0x61 && b <= 0x66) return b - 0x57;       // 'a'-'f'
    throw new IOException("Invalid hex character: " + (char) b); //NON-NLS
  }
}
