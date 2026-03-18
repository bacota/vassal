/*
 *
 * Copyright (c) 2000-2024 by Rodney Kinney
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License (LGPL) as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, copies are available
 * at http://www.opensource.org.
 */
package VASSAL.tools;

import VASSAL.tools.io.DeobfuscatingInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Standalone command-line tool that reads a VASSAL {@code .vlog} file and
 * prints the name of each top-level command contained in it, followed by a
 * summary count of commands by type.
 *
 * <p>Usage: {@code java VASSAL.tools.VlogCommandLister <file.vlog>}</p>
 *
 * <p>This tool does NOT require a running {@code GameModule} or any GUI
 * components.  It only uses {@link DeobfuscatingInputStream} from the VASSAL
 * low-level I/O layer and the standard Java library.</p>
 */
public class VlogCommandLister {

  /** ZIP entry name that holds the serialised command tree in a vlog/vsav file. */
  private static final String SAVEFILE_ZIP_ENTRY = "savedGame"; //$NON-NLS-1$

  /**
   * Command separator used by {@code GameModule.decode()}.
   * Value is {@code KeyEvent.VK_ESCAPE} (ASCII 27, ESC character).
   */
  private static final char COMMAND_SEPARATOR = '\u001b';

  // Command prefixes sourced directly from the VASSAL codebase:
  private static final String BEGIN_SAVE       = "begin_save";   //$NON-NLS-1$ GameState
  private static final String END_SAVE         = "end_save";     //$NON-NLS-1$ GameState
  private static final String ADD              = "+/";           //$NON-NLS-1$ BasicCommandEncoder.ADD
  private static final String REMOVE           = "-/";           //$NON-NLS-1$ BasicCommandEncoder.REMOVE
  private static final String CHANGE           = "D/";           //$NON-NLS-1$ BasicCommandEncoder.CHANGE
  private static final String MOVE             = "M/";           //$NON-NLS-1$ BasicCommandEncoder.MOVE
  private static final String LOG              = "LOG\t";        //$NON-NLS-1$ BasicLogger.LOG
  private static final String UNDO             = "UNDO\t";       //$NON-NLS-1$ BasicLogger.UNDO
  private static final String EVENT_LIST       = "Events";       //$NON-NLS-1$ EventLog.EVENT_LIST
  private static final String EXT_CMD          = "EXT\t";        //$NON-NLS-1$ ExtensionsLoader.COMMAND_PREFIX
  private static final String AUDIO            = "AUDIO\t";      //$NON-NLS-1$ PlayAudioClipCommand.COMMAND_PREFIX
  private static final String CHAT             = "CHAT";         //$NON-NLS-1$ Chatter.DisplayText.PREFIX

  /** Maximum characters of an unknown command token shown in the label. */
  private static final int UNKNOWN_PREVIEW_LEN = 40;

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Usage: VlogCommandLister <file.vlog>"); //$NON-NLS-1$
      System.exit(1);
    }

    final String filePath = args[0];
    final String content;

    try (InputStream fis = Files.newInputStream(Paths.get(filePath));
         ZipInputStream zip = new ZipInputStream(fis)) {

      String savedGame = null;
      for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
        if (SAVEFILE_ZIP_ENTRY.equals(entry.getName())) {
          try (InputStream din = new DeobfuscatingInputStream(zip)) {
            savedGame = new String(din.readAllBytes(), StandardCharsets.UTF_8);
          }
          break;
        }
      }

      if (savedGame == null) {
        System.err.println("No '" + SAVEFILE_ZIP_ENTRY + "' entry found in: " + filePath); //$NON-NLS-1$
        System.exit(2);
      }
      content = savedGame;
    }

    // Split by COMMAND_SEPARATOR and identify each token.
    final Map<String, Integer> counts = new LinkedHashMap<>();
    final String[] tokens = content.split(String.valueOf(COMMAND_SEPARATOR), -1);

    for (final String token : tokens) {
      final String name = identifyCommand(token);
      System.out.println(name);
      counts.merge(name, 1, Integer::sum);
    }

    // Summary
    System.out.println();
    System.out.println("=== Command summary ==="); //$NON-NLS-1$
    counts.forEach((type, count) ->
      System.out.printf("  %-40s %d%n", type, count)); //$NON-NLS-1$
  }

  /**
   * Returns a human-readable label for a single serialised command token.
   *
   * <p>For {@code LogCommand} tokens the label also includes the identity of
   * the wrapped inner command in parentheses, e.g.
   * {@code "LogCommand(Chatter.DisplayText)"}.</p>
   *
   * @param token a single command token (no {@code COMMAND_SEPARATOR} inside)
   * @return display label for the command type
   */
  static String identifyCommand(final String token) {
    if (token.isEmpty()) {
      return "NullCommand"; //$NON-NLS-1$
    }
    if (BEGIN_SAVE.equals(token)) {
      return "SetupCommand(false)"; //$NON-NLS-1$
    }
    if (END_SAVE.equals(token)) {
      return "SetupCommand(true)"; //$NON-NLS-1$
    }
    if (token.startsWith(LOG)) {
      final String inner = identifyCommand(token.substring(LOG.length()));
      return "LogCommand(" + inner + ")"; //$NON-NLS-1$
    }
    if (token.startsWith(UNDO)) {
      return "UndoCommand"; //$NON-NLS-1$
    }
    if (token.startsWith(ADD)) {
      return "AddPiece"; //$NON-NLS-1$
    }
    if (token.startsWith(REMOVE)) {
      return "RemovePiece"; //$NON-NLS-1$
    }
    if (token.startsWith(CHANGE)) {
      return "ChangePiece"; //$NON-NLS-1$
    }
    if (token.startsWith(MOVE)) {
      return "MovePiece"; //$NON-NLS-1$
    }
    if (token.startsWith(EVENT_LIST)) {
      return "StoreEvents"; //$NON-NLS-1$
    }
    if (token.startsWith(EXT_CMD)) {
      return "ExtensionCommand"; //$NON-NLS-1$
    }
    if (token.startsWith(AUDIO)) {
      return "PlayAudioClipCommand"; //$NON-NLS-1$
    }
    if (token.startsWith(CHAT)) {
      return "Chatter.DisplayText"; //$NON-NLS-1$
    }
    // Fall-through: show a truncated preview so the caller can investigate.
    final String preview = token.length() > UNKNOWN_PREVIEW_LEN
        ? token.substring(0, UNKNOWN_PREVIEW_LEN) + "..."  //$NON-NLS-1$
        : token;
    return "Unknown(" + preview + ")"; //$NON-NLS-1$
  }
}
