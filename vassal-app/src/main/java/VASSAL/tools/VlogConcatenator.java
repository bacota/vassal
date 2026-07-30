/*
 * Copyright (c) 2024 by VASSAL developers
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

import VASSAL.tools.io.DeobfuscatingInputStream;
import VASSAL.tools.io.ObfuscatingOutputStream;
import VASSAL.tools.io.ZipWriter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Headless command-line utility that concatenates two VASSAL log files (.vlog)
 * into a new one. The output vlog contains the beginning state from the first
 * vlog followed by all log commands from both vlogs in sequence.
 *
 * <p>Usage: VlogConcatenator &lt;module&gt; &lt;vlog1&gt; &lt;vlog2&gt; &lt;output&gt;</p>
 *
 * <p>The module argument is accepted for compatibility but is not required for
 * the raw log concatenation, which operates directly on the encoded command
 * strings without game-state execution.</p>
 */
public class VlogConcatenator {

  /** The ZIP entry name for the saved-game data (matches {@code GameState.SAVEFILE_ZIP_ENTRY}). */
  private static final String SAVEFILE_ZIP_ENTRY = "savedGame"; //NON-NLS

  /** The ZIP entry name for save-file metadata (matches {@code SaveMetaData.ZIP_ENTRY_NAME}). */
  private static final String SAVEDATA_ZIP_ENTRY = "savedata"; //NON-NLS

  /** The ZIP entry name for module metadata (matches {@code ModuleMetaData.ZIP_ENTRY_NAME}). */
  private static final String MODULEDATA_ZIP_ENTRY = "moduledata"; //NON-NLS

  /**
   * The command separator character used by {@code GameModule.encode/decode}
   * (matches {@code KeyEvent.VK_ESCAPE}).
   */
  private static final char COMMAND_SEPARATOR = '\u001B';

  /** Prefix for log commands as encoded by {@code BasicLogger}. */
  private static final String LOG_PREFIX = "LOG\t"; //NON-NLS

  public static void main(String[] args) {
    System.setProperty("java.awt.headless", "true"); //NON-NLS

    if (args.length != 4) {
      System.err.println("Usage: VlogConcatenator <module> <vlog1> <vlog2> <output>"); //NON-NLS
      System.err.println("  module  - path to the .vmod module file"); //NON-NLS
      System.err.println("  vlog1   - path to the first .vlog file"); //NON-NLS
      System.err.println("  vlog2   - path to the second .vlog file"); //NON-NLS
      System.err.println("  output  - path for the concatenated output .vlog file"); //NON-NLS
      System.exit(1);
    }

    final File moduleFile = new File(args[0]);
    final File vlog1File  = new File(args[1]);
    final File vlog2File  = new File(args[2]);
    final File outputFile = new File(args[3]);

    if (!moduleFile.isFile()) {
      System.err.println("Error: Module file not found: " + moduleFile.getAbsolutePath()); //NON-NLS
      System.exit(1);
    }
    if (!vlog1File.isFile()) {
      System.err.println("Error: First vlog file not found: " + vlog1File.getAbsolutePath()); //NON-NLS
      System.exit(1);
    }
    if (!vlog2File.isFile()) {
      System.err.println("Error: Second vlog file not found: " + vlog2File.getAbsolutePath()); //NON-NLS
      System.exit(1);
    }

    try {
      System.out.println("Loading first vlog..."); //NON-NLS
      final String vlog1Encoded = readSavedGameEntry(vlog1File);

      System.out.println("Loading second vlog..."); //NON-NLS
      final String vlog2Encoded = readSavedGameEntry(vlog2File);

      System.out.println("Writing output vlog..."); //NON-NLS
      writeOutput(vlog1Encoded, vlog2Encoded, vlog1File, outputFile);

      System.out.println("Done."); //NON-NLS
      System.exit(0);
    }
    catch (IOException e) {
      System.err.println("Error: " + e.getMessage()); //NON-NLS
      System.exit(1);
    }
  }

  /**
   * Reads and deobfuscates the {@value #SAVEFILE_ZIP_ENTRY} entry from the
   * given vlog/vsav ZIP file.
   *
   * @param vlogFile the vlog ZIP file to read
   * @return the decoded command-sequence string
   * @throws IOException if the file cannot be read or has no savedGame entry
   */
  static String readSavedGameEntry(File vlogFile) throws IOException {
    try (InputStream fin = new BufferedInputStream(Files.newInputStream(vlogFile.toPath()));
         ZipInputStream zin = new ZipInputStream(fin)) {
      for (ZipEntry entry = zin.getNextEntry(); entry != null; entry = zin.getNextEntry()) {
        if (SAVEFILE_ZIP_ENTRY.equals(entry.getName())) {
          try (DeobfuscatingInputStream din = new DeobfuscatingInputStream(zin)) {
            final byte[] bytes = din.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
          }
        }
      }
    }
    throw new IOException("Invalid vlog file (no '" + SAVEFILE_ZIP_ENTRY + "' entry): " + vlogFile); //NON-NLS
  }

  /**
   * Extracts the log-command tokens (those prefixed with {@value #LOG_PREFIX})
   * from an encoded command-sequence string.
   *
   * <p>Tokens are separated by {@link #COMMAND_SEPARATOR}. A separator preceded
   * by an odd number of backslashes is escaped (part of the token); a separator
   * preceded by an even number of backslashes (including zero) is a real boundary.
   *
   * @param encoded the full encoded command-sequence string from a vlog file
   * @return a list of raw (still-escaped) log-command token strings
   */
  static List<String> extractLogTokens(String encoded) {
    final List<String> logTokens = new ArrayList<>();
    int tokenStart = 0;
    for (int i = 0; i <= encoded.length(); i++) {
      final boolean atEnd = (i == encoded.length());
      if (atEnd || encoded.charAt(i) == COMMAND_SEPARATOR) {
        // Count consecutive backslashes immediately before position i
        int backslashCount = 0;
        for (int j = i - 1; j >= tokenStart && encoded.charAt(j) == '\\'; j--) {
          backslashCount++;
        }
        // An odd number of preceding backslashes means the separator is escaped
        final boolean escaped = !atEnd && (backslashCount % 2 == 1);
        if (!escaped) {
          final String token = encoded.substring(tokenStart, i);
          if (token.startsWith(LOG_PREFIX)) {
            logTokens.add(token);
          }
          tokenStart = i + 1;
        }
      }
    }
    return logTokens;
  }

  /**
   * Writes the concatenated output vlog ZIP file.
   *
   * <p>The output contains:
   * <ul>
   *   <li>A {@value #SAVEFILE_ZIP_ENTRY} entry with vlog1's full encoded data
   *       followed by any log-command tokens extracted from vlog2.</li>
   *   <li>The {@value #SAVEDATA_ZIP_ENTRY} and {@value #MODULEDATA_ZIP_ENTRY}
   *       metadata entries copied from vlog1.</li>
   * </ul>
   *
   * @param vlog1Encoded  the decoded saved-game string from the first vlog
   * @param vlog2Encoded  the decoded saved-game string from the second vlog
   * @param vlog1File     the first vlog file (used to copy metadata entries)
   * @param outputFile    destination file for the concatenated vlog
   * @throws IOException if any I/O error occurs
   */
  static void writeOutput(String vlog1Encoded,
                          String vlog2Encoded,
                          File vlog1File,
                          File outputFile) throws IOException {
    // Build the combined encoded string: vlog1 + vlog2 log tokens
    final List<String> vlog2LogTokens = extractLogTokens(vlog2Encoded);
    final StringBuilder combined = new StringBuilder(vlog1Encoded);
    for (final String token : vlog2LogTokens) {
      combined.append(COMMAND_SEPARATOR);
      combined.append(token);
    }
    final byte[] combinedBytes = combined.toString().getBytes(StandardCharsets.UTF_8);

    try (ZipWriter zw = new ZipWriter(outputFile)) {
      // Write the combined savedGame entry
      try (OutputStream out = new ObfuscatingOutputStream(
             new BufferedOutputStream(zw.write(SAVEFILE_ZIP_ENTRY)))) {
        out.write(combinedBytes);
      }

      // Copy metadata entries from vlog1
      copyMetadataEntries(vlog1File, zw);
    }
  }

  /**
   * Copies the {@value #SAVEDATA_ZIP_ENTRY} and {@value #MODULEDATA_ZIP_ENTRY}
   * entries from {@code sourceVlog} into {@code zw}.
   *
   * @param sourceVlog the vlog file to copy metadata from
   * @param zw         the {@link ZipWriter} to write into
   * @throws IOException if any I/O error occurs
   */
  private static void copyMetadataEntries(File sourceVlog, ZipWriter zw) throws IOException {
    try (ZipFile zf = new ZipFile(sourceVlog)) {
      final ZipEntry savedataEntry = zf.getEntry(SAVEDATA_ZIP_ENTRY);
      if (savedataEntry != null) {
        try (InputStream in = zf.getInputStream(savedataEntry)) {
          zw.write(in, SAVEDATA_ZIP_ENTRY);
        }
      }
      final ZipEntry moduledataEntry = zf.getEntry(MODULEDATA_ZIP_ENTRY);
      if (moduledataEntry != null) {
        try (InputStream in = zf.getInputStream(moduledataEntry)) {
          zw.write(in, MODULEDATA_ZIP_ENTRY);
        }
      }
    }
  }
}
