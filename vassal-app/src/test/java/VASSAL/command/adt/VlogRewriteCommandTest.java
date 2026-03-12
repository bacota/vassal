/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
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
package VASSAL.command.adt;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link VlogRewriteCommand}.
 */
class VlogRewriteCommandTest {

  @TempDir
  File tempDir;

  // -----------------------------------------------------------------------
  // split() tests
  // -----------------------------------------------------------------------

  @Test
  void split_emptyString_returnsEmptyList() {
    assertTrue(VlogRewriteCommand.split("").isEmpty());
  }

  @Test
  void split_singleToken_noSeparator() {
    final List<String> result = VlogRewriteCommand.split("SETUP\ttrue");
    assertEquals(1, result.size());
    assertEquals("SETUP\ttrue", result.get(0));
  }

  @Test
  void split_twoTokens_separatedByEscape() {
    final String sep = String.valueOf(VlogRewriteCommand.COMMAND_SEPARATOR);
    final List<String> result = VlogRewriteCommand.split("SETUP\ttrue" + sep + "LOG\tSOMETHING");
    assertEquals(2, result.size());
    assertEquals("SETUP\ttrue", result.get(0));
    assertEquals("LOG\tSOMETHING", result.get(1));
  }

  @Test
  void split_emptyTokensAreSkipped() {
    // Leading / trailing separators should not produce empty tokens
    final String sep = String.valueOf(VlogRewriteCommand.COMMAND_SEPARATOR);
    final List<String> result = VlogRewriteCommand.split(sep + "TOKEN" + sep);
    assertEquals(1, result.size());
    assertEquals("TOKEN", result.get(0));
  }

  // -----------------------------------------------------------------------
  // classify() tests
  // -----------------------------------------------------------------------

  @Test
  void classify_logToken() {
    assertEquals("LOG", VlogRewriteCommand.classify("LOG\tanything"));
  }

  @Test
  void classify_undoToken() {
    assertEquals("UNDO", VlogRewriteCommand.classify("UNDO\ttrue"));
  }

  @Test
  void classify_setupToken() {
    assertEquals("SETUP", VlogRewriteCommand.classify("SETUP\ttrue"));
  }

  @Test
  void classify_unknownToken() {
    assertEquals("UNKNOWN", VlogRewriteCommand.classify("ZZZNOTHINGKNOWN\t123"));
  }

  // -----------------------------------------------------------------------
  // summarise() tests
  // -----------------------------------------------------------------------

  @Test
  void summarise_emptyString() {
    final VlogRewriteCommand.CommandSummary summary = VlogRewriteCommand.summarise("");
    assertEquals(0, summary.totalTokens);
    assertTrue(summary.countsByType.isEmpty());
  }

  @Test
  void summarise_mixedTokens() {
    final String sep = String.valueOf(VlogRewriteCommand.COMMAND_SEPARATOR);
    final String input = "SETUP\ttrue" + sep + "LOG\tfoo" + sep + "LOG\tbar" + sep + "UNDO\tfalse";
    final VlogRewriteCommand.CommandSummary summary = VlogRewriteCommand.summarise(input);
    assertEquals(4, summary.totalTokens);
    assertEquals(2, (int) summary.countsByType.get("LOG"));
    assertEquals(1, (int) summary.countsByType.get("SETUP"));
    assertEquals(1, (int) summary.countsByType.get("UNDO"));
  }

  @Test
  void summarise_toString_containsSummary() {
    final String sep = String.valueOf(VlogRewriteCommand.COMMAND_SEPARATOR);
    final String input = "SETUP\ttrue" + sep + "LOG\tfoo";
    final String str = VlogRewriteCommand.summarise(input).toString();
    assertTrue(str.contains("2"), "Should mention 2 tokens");
    assertTrue(str.contains("LOG"), "Should list LOG");
    assertTrue(str.contains("SETUP"), "Should list SETUP");
  }

  // -----------------------------------------------------------------------
  // readVlog / writeVlog round-trip
  // -----------------------------------------------------------------------

  @Test
  void readWriteVlog_roundTrip(@TempDir File tmp) throws IOException {
    final String commandString = "SETUP\ttrue\u001BLOG\tsome command data\u001BUNDO\tfalse";

    // Write to a vlog file
    final File vlogFile = new File(tmp, "test.vlog");
    VlogRewriteCommand.writeVlog(vlogFile, commandString);

    assertTrue(vlogFile.exists(), "vlog file should be created");
    assertTrue(vlogFile.length() > 0, "vlog file should not be empty");

    // Read it back
    final String read = VlogRewriteCommand.readVlog(vlogFile);
    assertEquals(commandString, read);
  }

  @Test
  void readWriteVlog_copyPreservesContent(@TempDir File tmp) throws IOException {
    final String content = "LOG\thello world\u001BLOG\t";

    final File src = new File(tmp, "src.vlog");
    final File dst = new File(tmp, "dst.vlog");

    VlogRewriteCommand.writeVlog(src, content);

    // Copy via read+write
    VlogRewriteCommand.writeVlog(dst, VlogRewriteCommand.readVlog(src));

    assertEquals(content, VlogRewriteCommand.readVlog(dst));
  }

  @Test
  void readVlog_missingEntry_throwsIOException(@TempDir File tmp) throws IOException {
    // Create a ZIP file that has no "savedGame" entry
    final File bad = new File(tmp, "bad.vlog");
    try (java.util.zip.ZipOutputStream zos =
        new java.util.zip.ZipOutputStream(Files.newOutputStream(bad.toPath()))) {
      zos.putNextEntry(new java.util.zip.ZipEntry("wrong-entry"));
      zos.write("data".getBytes(StandardCharsets.UTF_8));
    }
    assertThrows(IOException.class, () -> VlogRewriteCommand.readVlog(bad));
  }

  // -----------------------------------------------------------------------
  // copyVlog — metadata preservation
  // -----------------------------------------------------------------------

  @Test
  void copyVlog_preservesExtraEntries(@TempDir File tmp) throws IOException {
    final String commandString = "SETUP\ttrue\u001BLOG\tsome data";
    final byte[] savedataBytes = "<savedata><version>1</version></savedata>".getBytes(StandardCharsets.UTF_8);
    final byte[] moduledataBytes = "<moduledata><name>Test</name></moduledata>".getBytes(StandardCharsets.UTF_8);

    // Build a source vlog with savedGame + savedata + moduledata
    final File src = new File(tmp, "src.vlog");
    try (java.util.zip.ZipOutputStream zos =
             new java.util.zip.ZipOutputStream(Files.newOutputStream(src.toPath()))) {
      // savedGame (obfuscated)
      zos.putNextEntry(new java.util.zip.ZipEntry(VASSAL.build.module.GameState.SAVEFILE_ZIP_ENTRY));
      // Write raw obfuscated bytes using ObfuscatingOutputStream
      final java.io.ByteArrayOutputStream obfBuf = new java.io.ByteArrayOutputStream();
      try (VASSAL.tools.io.ObfuscatingOutputStream oos = new VASSAL.tools.io.ObfuscatingOutputStream(obfBuf)) {
        oos.write(commandString.getBytes(StandardCharsets.UTF_8));
      }
      zos.write(obfBuf.toByteArray());

      // savedata (plain)
      zos.putNextEntry(new java.util.zip.ZipEntry("savedata"));
      zos.write(savedataBytes);

      // moduledata (plain)
      zos.putNextEntry(new java.util.zip.ZipEntry("moduledata"));
      zos.write(moduledataBytes);
    }

    // Copy it
    final File dst = new File(tmp, "dst.vlog");
    VlogRewriteCommand.copyVlog(src, dst, commandString);

    // Verify: savedGame round-trips correctly
    assertEquals(commandString, VlogRewriteCommand.readVlog(dst));

    // Verify: savedata and moduledata are preserved verbatim
    final java.util.Map<String, byte[]> entries = readAllEntries(dst);
    assertTrue(entries.containsKey("savedata"), "savedata should be preserved");
    assertTrue(entries.containsKey("moduledata"), "moduledata should be preserved");
    assertArrayEquals(savedataBytes, entries.get("savedata"));
    assertArrayEquals(moduledataBytes, entries.get("moduledata"));
  }

  @Test
  void copyVlog_replacesCommandString(@TempDir File tmp) throws IOException {
    final String original = "SETUP\ttrue\u001BLOG\toriginal";
    final String replacement = "SETUP\ttrue\u001BLOG\treplaced";

    final File src = new File(tmp, "src.vlog");
    VlogRewriteCommand.writeVlog(src, original);

    final File dst = new File(tmp, "dst.vlog");
    VlogRewriteCommand.copyVlog(src, dst, replacement);

    assertEquals(replacement, VlogRewriteCommand.readVlog(dst));
  }

  /** Helper: reads all raw (non-deobfuscated) bytes for each ZIP entry. */
  private static java.util.Map<String, byte[]> readAllEntries(File zipFile) throws IOException {
    final java.util.Map<String, byte[]> result = new java.util.LinkedHashMap<>();
    try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
             Files.newInputStream(zipFile.toPath()))) {
      for (java.util.zip.ZipEntry e = zis.getNextEntry(); e != null; e = zis.getNextEntry()) {
        final java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        final byte[] buffer = new byte[4096];
        int n;
        while ((n = zis.read(buffer)) != -1) buf.write(buffer, 0, n);
        result.put(e.getName(), buf.toByteArray());
      }
    }
    return result;
  }
}
