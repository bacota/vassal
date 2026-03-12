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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;

import VASSAL.build.module.BasicLogger;
import VASSAL.build.module.GameState;
import VASSAL.tools.io.DeobfuscatingInputStream;
import VASSAL.tools.io.ObfuscatingOutputStream;
import VASSAL.tools.io.ZipWriter;

/**
 * A command-line utility that reads an existing {@code .vlog} file and
 * re-writes its commands to a new {@code .vlog} file.
 *
 * <p>The tool works at the ZIP + obfuscation level: it deobfuscates the
 * "savedGame" entry from the input file and re-obfuscates it into the output
 * file.  No running game module is required.
 *
 * <p>In addition to copying the data verbatim the tool prints a human-readable
 * summary of the command structure it finds in the log (number of top-level
 * command tokens, number of {@link BasicLogger#LOG} entries, undo markers,
 * etc.) using the ADT command-type constants where available.
 *
 * <h3>Usage</h3>
 * <pre>
 *   java -cp vassal.jar VASSAL.command.adt.VlogRewriteCommand &lt;input.vlog&gt; &lt;output.vlog&gt;
 * </pre>
 *
 * <h3>Static API</h3>
 * The {@link #readVlog(File)} and {@link #writeVlog(File, String)} methods are
 * also useful as a reusable library API for tests and other tooling that needs
 * to inspect or produce vlog files without a running {@code GameModule}.
 */
public class VlogRewriteCommand {

  /**
   * Character used by {@code GameModule} to separate sub-commands inside a
   * serialised {@code Command} string.  This is {@code KeyEvent.VK_ESCAPE} (27).
   */
  static final char COMMAND_SEPARATOR = '\u001B';

  // Known sub-command prefixes found in VASSAL vlog files.
  // Ordered from most-specific to least-specific so the first match wins.
  // Note: "CHAT" has no trailing tab because Chatter.DisplayText.PREFIX = "CHAT" (no separator).
  // All others use "\t" as the separator after the prefix.
  private static final String[] KNOWN_PREFIXES = {
    BasicLogger.LOG,
    BasicLogger.UNDO,
    "SETUP\t",          // GameState.SetupCommand //NON-NLS
    "CHAT",             // Chatter.DisplayText.PREFIX — no tab by design //NON-NLS
    "NOTES\t",          // NotesWindow.SetScenarioNote //NON-NLS
    "PNOTES\t",         // NotesWindow.SetPublicNote //NON-NLS
    "PNOTE\t",          // SetPrivateTextCommand //NON-NLS
    "SNOTE\t",          // AddSecretNoteCommand //NON-NLS
    "UNMASK\t",         // ObscurableOptions.SetAllowed //NON-NLS
    "GlobalProperty\t", // GlobalProperty.SetGlobalProperty //NON-NLS
    "MutableProperty\t",// ChangePropertyCommand //NON-NLS
    "TURN",             // TurnTracker.SetTurn //NON-NLS
    "LOS_THREAD",       // LOS_Thread.LOSCommand //NON-NLS
    "SHOW\t",           // SpecialDiceButton.ShowResults //NON-NLS
    "DECK\t",           // Deck.LoadDeckCommand //NON-NLS
    "ROSTER\t",         // PlayerRoster.Add/Remove //NON-NLS
    "MODULE_EXT\t",     // ModuleExtension.RegCmd //NON-NLS
    "EVENT_LOG\t",      // EventLog.StoreEvents //NON-NLS
  };

  // -----------------------------------------------------------------------
  // CLI entry point
  // -----------------------------------------------------------------------

  /**
   * Reads commands from {@code args[0]} and writes them to {@code args[1]}.
   *
   * @param args {@code [input.vlog, output.vlog]}
   */
  public static void main(String[] args) throws IOException {
    if (args.length < 2) {
      System.err.println("Usage: VlogRewriteCommand <input.vlog> <output.vlog>"); //NON-NLS
      System.exit(1);
    }

    final File inputFile = new File(args[0]);
    final File outputFile = new File(args[1]);

    if (!inputFile.exists()) {
      System.err.println("Input file not found: " + inputFile); //NON-NLS
      System.exit(1);
    }

    System.out.println("Reading: " + inputFile); //NON-NLS
    final String commandString = readVlog(inputFile);
    System.out.println("Read " + commandString.length() + " command characters"); //NON-NLS

    final CommandSummary summary = summarise(commandString);
    System.out.println(summary);

    System.out.println("Writing: " + outputFile); //NON-NLS
    copyVlog(inputFile, outputFile, commandString);
    System.out.println("Done."); //NON-NLS
  }

  // -----------------------------------------------------------------------
  // Public library API
  // -----------------------------------------------------------------------

  /**
   * Reads and deobfuscates the command string stored in the {@code savedGame}
   * ZIP entry of the given {@code .vlog} file.
   *
   * @param vlogFile the {@code .vlog} file to read
   * @return the raw VASSAL-encoded command string
   * @throws IOException if the file cannot be read or is missing the
   *                     {@code savedGame} entry
   */
  public static String readVlog(File vlogFile) throws IOException {
    try (ZipInputStream zis = new ZipInputStream(
        new BufferedInputStream(Files.newInputStream(vlogFile.toPath())))) {
      for (ZipEntry entry = zis.getNextEntry();
           entry != null;
           entry = zis.getNextEntry()) {
        if (GameState.SAVEFILE_ZIP_ENTRY.equals(entry.getName())) {
          try (InputStream din = new DeobfuscatingInputStream(zis)) {
            return IOUtils.toString(din, StandardCharsets.UTF_8);
          }
        }
      }
    }
    throw new IOException(
        "Invalid vlog file: missing '" + GameState.SAVEFILE_ZIP_ENTRY //NON-NLS
        + "' entry in " + vlogFile);
  }

  /**
   * Writes a command string to a new {@code .vlog} file, using the standard
   * VASSAL obfuscation so the file can be opened by VASSAL.
   *
   * <p>This method creates a minimal vlog containing only the {@code savedGame}
   * entry.  When you are re-writing an existing vlog and want to preserve all
   * other metadata ({@code savedata}, {@code moduledata}, etc.), use
   * {@link #copyVlog(File, File, String)} instead.
   *
   * @param vlogFile      the output {@code .vlog} file to create (or overwrite)
   * @param commandString the raw VASSAL-encoded command string
   * @throws IOException if the file cannot be written
   */
  public static void writeVlog(File vlogFile, String commandString) throws IOException {
    try (ZipWriter zw = new ZipWriter(vlogFile)) {
      try (OutputStream out = new ObfuscatingOutputStream(
          new BufferedOutputStream(zw.write(GameState.SAVEFILE_ZIP_ENTRY)))) {
        out.write(commandString.getBytes(StandardCharsets.UTF_8));
      }
    }
  }

  /**
   * Copies an existing {@code .vlog} file to a new file, replacing only the
   * {@code savedGame} entry with the provided command string.
   *
   * <p>All other ZIP entries ({@code savedata}, {@code moduledata}, and any
   * other entries present in the source file) are copied verbatim so that the
   * output vlog is a fully valid VASSAL log file indistinguishable from one
   * written by the engine.
   *
   * @param inputVlog     the source {@code .vlog} file to copy from
   * @param outputVlog    the destination {@code .vlog} file to create (or overwrite)
   * @param commandString the raw VASSAL-encoded command string to write into
   *                      the {@code savedGame} entry
   * @throws IOException if either file cannot be read or written
   */
  public static void copyVlog(File inputVlog, File outputVlog, String commandString) throws IOException {
    try (ZipInputStream zis = new ZipInputStream(
             new BufferedInputStream(Files.newInputStream(inputVlog.toPath())));
         ZipWriter zw = new ZipWriter(outputVlog)) {

      boolean savedGameWritten = false;

      for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
        if (GameState.SAVEFILE_ZIP_ENTRY.equals(entry.getName())) {
          // Replace with the (possibly modified) command string
          try (OutputStream out = new ObfuscatingOutputStream(
              new BufferedOutputStream(zw.write(GameState.SAVEFILE_ZIP_ENTRY)))) {
            out.write(commandString.getBytes(StandardCharsets.UTF_8));
          }
          savedGameWritten = true;
        }
        else {
          // Stream every other entry (savedata, moduledata, …) verbatim
          zw.write(zis, entry.getName());
        }
      }

      if (!savedGameWritten) {
        // Input had no savedGame entry; write it anyway so the output is valid
        try (OutputStream out = new ObfuscatingOutputStream(
            new BufferedOutputStream(zw.write(GameState.SAVEFILE_ZIP_ENTRY)))) {
          out.write(commandString.getBytes(StandardCharsets.UTF_8));
        }
      }
    }
  }

  // -----------------------------------------------------------------------
  // Command analysis
  // -----------------------------------------------------------------------

  /**
   * Splits the raw VASSAL-encoded command string into individual sub-command
   * tokens (using {@link #COMMAND_SEPARATOR}) and returns a {@link CommandSummary}
   * describing the distribution of command types.
   *
   * @param commandString the raw VASSAL-encoded command string
   * @return a summary of the command types found
   */
  public static CommandSummary summarise(String commandString) {
    final List<String> tokens = split(commandString);
    final Map<String, Integer> counts = new LinkedHashMap<>();
    for (final String token : tokens) {
      final String label = classify(token);
      counts.merge(label, 1, Integer::sum);
    }
    return new CommandSummary(tokens.size(), counts);
  }

  /**
   * Splits a raw VASSAL-encoded command string into individual sub-command
   * tokens by splitting on {@link #COMMAND_SEPARATOR}.
   *
   * <p>Empty tokens (which can appear at the start or end of a compound
   * command string) are omitted.
   *
   * @param commandString the raw VASSAL-encoded command string
   * @return the list of non-empty sub-command strings
   */
  static List<String> split(String commandString) {
    final List<String> result = new ArrayList<>();
    int start = 0;
    for (int i = 0; i < commandString.length(); i++) {
      if (commandString.charAt(i) == COMMAND_SEPARATOR) {
        final String token = commandString.substring(start, i);
        if (!token.isEmpty()) {
          result.add(token);
        }
        start = i + 1;
      }
    }
    final String last = commandString.substring(start);
    if (!last.isEmpty()) {
      result.add(last);
    }
    return result;
  }

  /**
   * Returns a human-readable label for the given sub-command token by
   * matching its prefix against the list of known VASSAL command prefixes.
   * Returns {@code "UNKNOWN"} if no known prefix matches.
   *
   * @param token a single VASSAL sub-command string
   * @return a label such as {@code "LOG"}, {@code "SETUP"}, or {@code "UNKNOWN"}
   */
  static String classify(String token) {
    for (final String prefix : KNOWN_PREFIXES) {
      if (token.startsWith(prefix)) {
        // Trim trailing \t for readability
        return prefix.endsWith("\t") ? prefix.substring(0, prefix.length() - 1) : prefix;
      }
    }
    return "UNKNOWN"; //NON-NLS
  }

  // -----------------------------------------------------------------------
  // Summary record
  // -----------------------------------------------------------------------

  /**
   * A simple summary of the command types found in a vlog file.
   */
  public static final class CommandSummary {
    /** Total number of top-level sub-command tokens in the command string. */
    public final int totalTokens;
    /** Count per command-type label (ordered by first occurrence). */
    public final Map<String, Integer> countsByType;

    CommandSummary(int totalTokens, Map<String, Integer> countsByType) {
      this.totalTokens = totalTokens;
      this.countsByType = countsByType;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append("Command summary: ").append(totalTokens).append(" token(s)\n"); //NON-NLS
      for (final Map.Entry<String, Integer> e : countsByType.entrySet()) {
        sb.append("  ").append(e.getKey()).append(": ").append(e.getValue()).append('\n');
      }
      return sb.toString().stripTrailing();
    }
  }
}
