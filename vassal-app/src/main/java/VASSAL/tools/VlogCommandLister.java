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

import VASSAL.command.ChangePiece;
import VASSAL.command.MovePiece;
import VASSAL.tools.io.DeobfuscatingInputStream;
import VASSAL.tools.io.ObfuscatingOutputStream;
import VASSAL.tools.io.ZipWriter;

import java.awt.Point;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Standalone command-line tool that reads a VASSAL {@code .vlog} file and
 * prints the decoded {@link VASSAL.command.Command#toString()} of each top-level
 * command contained in it, followed by a summary count of commands by type.
 * It can also recreate a {@code .vlog} file from a previously extracted token stream.
 *
 * <p>List commands in a vlog:</p>
 * <pre>  java VASSAL.tools.VlogCommandLister &lt;file.vlog&gt;</pre>
 *
 * <p>Recreate a vlog from its tokens (round-trip):</p>
 * <pre>  java VASSAL.tools.VlogCommandLister &lt;file.vlog&gt; --recreate &lt;output.vlog&gt;</pre>
 *
 * <p>This tool does NOT require a running {@code GameModule} or any GUI
 * components.  It only uses {@link DeobfuscatingInputStream}/
 * {@link ObfuscatingOutputStream} from the VASSAL low-level I/O layer,
 * {@link VASSAL.command.ChangePiece}/{@link MovePiece} (which are decodable
 * without a module), and the standard Java library.</p>
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
  private static final String BEGIN_SAVE  = "begin_save";  //$NON-NLS-1$ GameState
  private static final String END_SAVE    = "end_save";    //$NON-NLS-1$ GameState
  private static final String ADD         = "+/";          //$NON-NLS-1$ BasicCommandEncoder.ADD
  private static final String REMOVE      = "-/";          //$NON-NLS-1$ BasicCommandEncoder.REMOVE
  private static final String CHANGE      = "D/";          //$NON-NLS-1$ BasicCommandEncoder.CHANGE
  private static final String MOVE        = "M/";          //$NON-NLS-1$ BasicCommandEncoder.MOVE
  private static final String LOG         = "LOG\t";       //$NON-NLS-1$ BasicLogger.LOG
  private static final String UNDO        = "UNDO\t";      //$NON-NLS-1$ BasicLogger.UNDO
  private static final String EVENT_LIST  = "Events";      //$NON-NLS-1$ EventLog.EVENT_LIST
  private static final String EXT_CMD     = "EXT\t";       //$NON-NLS-1$ ExtensionsLoader.COMMAND_PREFIX
  private static final String AUDIO       = "AUDIO\t";     //$NON-NLS-1$ PlayAudioClipCommand.COMMAND_PREFIX
  private static final String CHAT        = "CHAT";        //$NON-NLS-1$ Chatter.DisplayText.PREFIX

  /** Delimiter used by {@code BasicCommandEncoder} between fields in a token. */
  private static final char PARAM_SEPARATOR = '/';

  /** Maximum characters of an unknown or large field shown in the output. */
  private static final int PREVIEW_LEN = 40;

  public static void main(String[] args) throws IOException {
    if (args.length != 1 && args.length != 3) {
      System.err.println("Usage: VlogCommandLister <file.vlog> [--recreate <output.vlog>]"); //$NON-NLS-1$
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

    // Split by COMMAND_SEPARATOR, decode each token, and print.
    final Map<String, Integer> counts = new LinkedHashMap<>();
    final String[] tokens = content.split(String.valueOf(COMMAND_SEPARATOR), -1);

    for (final String token : tokens) {
      final String typeName = identifyCommand(token);
      final String description = describeCommand(token);
      System.out.println(description);
      counts.merge(typeName, 1, Integer::sum);
    }

    // Summary
    System.out.println();
    System.out.println("=== Command summary ==="); //$NON-NLS-1$
    counts.forEach((type, count) ->
      System.out.printf("  %-40s %d%n", type, count)); //$NON-NLS-1$

    // Optionally recreate the vlog from the extracted tokens.
    if (args.length == 3 && "--recreate".equals(args[1])) { //$NON-NLS-1$
      final String outputPath = args[2];
      recreateVlog(tokens, new File(outputPath));
      System.out.println("Recreated vlog written to: " + outputPath); //$NON-NLS-1$
    }
  }

  /**
   * Recreates a {@code .vlog} file from an array of command tokens.
   *
   * <p>The tokens are joined with {@link #COMMAND_SEPARATOR}, then written
   * through {@link ObfuscatingOutputStream} into the {@code savedGame} entry
   * of a new ZIP file, exactly mirroring how {@code BasicLogger.write()} and
   * {@code GameState.saveGame()} produce vlog/vsav files.</p>
   *
   * <p>Only the {@code savedGame} ZIP entry is written; optional metadata
   * entries (e.g. {@code moduleData}) that a full VASSAL save would include
   * are not reproduced because they are not available without a running
   * {@code GameModule}.</p>
   *
   * @param tokens the command tokens to write; each element must not contain
   *               the {@link #COMMAND_SEPARATOR} character
   * @param outputFile destination file; created or overwritten as necessary
   * @throws IOException if writing the file fails
   */
  public static void recreateVlog(final String[] tokens, final File outputFile)
                                                             throws IOException {
    final String content = String.join(String.valueOf(COMMAND_SEPARATOR), tokens);
    try (ZipWriter zw = new ZipWriter(outputFile)) {
      try (OutputStream out = new ObfuscatingOutputStream(
               new BufferedOutputStream(zw.write(SAVEFILE_ZIP_ENTRY)))) {
        out.write(content.getBytes(StandardCharsets.UTF_8));
      }
    }
  }

  /**
   * Returns the short type name of the command represented by the given token.
   * Used for summary counting.
   *
   * @param token a single command token (no {@code COMMAND_SEPARATOR} inside)
   * @return short type name, e.g. {@code "ChangePiece"}
   */
  static String identifyCommand(final String token) {
    if (token.isEmpty())              return "NullCommand";            //$NON-NLS-1$
    if (BEGIN_SAVE.equals(token))     return "SetupCommand";           //$NON-NLS-1$
    if (END_SAVE.equals(token))       return "SetupCommand";           //$NON-NLS-1$
    if (token.startsWith(LOG))        return "LogCommand";             //$NON-NLS-1$
    if (token.startsWith(UNDO))       return "UndoCommand";            //$NON-NLS-1$
    if (token.startsWith(ADD))        return "AddPiece";               //$NON-NLS-1$
    if (token.startsWith(REMOVE))     return "RemovePiece";            //$NON-NLS-1$
    if (token.startsWith(CHANGE))     return "ChangePiece";            //$NON-NLS-1$
    if (token.startsWith(MOVE))       return "MovePiece";              //$NON-NLS-1$
    if (token.startsWith(EVENT_LIST)) return "StoreEvents";            //$NON-NLS-1$
    if (token.startsWith(EXT_CMD))    return "RegCmd";                 //$NON-NLS-1$
    if (token.startsWith(AUDIO))      return "PlayAudioClipCommand";   //$NON-NLS-1$
    if (token.startsWith(CHAT))       return "DisplayText";            //$NON-NLS-1$
    return "Unknown";                                                   //$NON-NLS-1$
  }

  /**
   * Decodes a serialised command token and returns a string matching the style
   * of {@link VASSAL.command.Command#toString()}: {@code ClassName[details]}.
   *
   * <p>For command types whose objects can be constructed without a running
   * {@code GameModule} ({@link ChangePiece}, {@link MovePiece}), the actual
   * {@code Command} object is created and {@code toString()} is called on it.
   * For all other types the relevant fields are extracted from the token and
   * formatted in the same {@code ClassName[field=value,...]} style.</p>
   *
   * <p>{@code LogCommand} tokens are handled recursively: the description of
   * the wrapped inner command is included in the output.</p>
   *
   * @param token a single command token (no {@code COMMAND_SEPARATOR} inside)
   * @return decoded description string, never {@code null}
   */
  static String describeCommand(final String token) {
    if (token.isEmpty()) {
      return "NullCommand"; //$NON-NLS-1$
    }

    if (BEGIN_SAVE.equals(token)) {
      return "SetupCommand[gameStarting=false]"; //$NON-NLS-1$
    }

    if (END_SAVE.equals(token)) {
      return "SetupCommand[gameStarting=true]"; //$NON-NLS-1$
    }

    if (token.startsWith(LOG)) {
      final String innerDesc = describeCommand(token.substring(LOG.length()));
      return "LogCommand[inner=" + innerDesc + "]"; //$NON-NLS-1$
    }

    if (token.startsWith(UNDO)) {
      final String inProgress = token.substring(UNDO.length());
      return "UndoCommand[inProgress=" + inProgress + "]"; //$NON-NLS-1$
    }

    if (token.startsWith(ADD)) {
      // Format: +/<id>/<type>/<state>
      final SequenceEncoder.Decoder st =
        new SequenceEncoder.Decoder(token.substring(ADD.length()), PARAM_SEPARATOR);
      final String id    = st.hasMoreTokens() ? unwrapNull(st.nextToken()) : null;
      final String type  = st.hasMoreTokens() ? truncate(st.nextToken()) : null;
      final String state = st.hasMoreTokens() ? truncate(st.nextToken()) : null;
      return "AddPiece[id=" + id + ",type=" + type + ",state=" + state + "]"; //$NON-NLS-1$
    }

    if (token.startsWith(REMOVE)) {
      // Format: -/<id>
      final String id = token.substring(REMOVE.length());
      return "RemovePiece[id=" + unwrapNull(id) + "]"; //$NON-NLS-1$
    }

    if (token.startsWith(CHANGE)) {
      // Format: D/<id>/<newState>[/<oldState>]
      // ChangePiece has a useful getDetails() — create the object and call toString().
      final SequenceEncoder.Decoder st =
        new SequenceEncoder.Decoder(token.substring(CHANGE.length()), PARAM_SEPARATOR);
      final String id       = st.hasMoreTokens() ? st.nextToken() : ""; //$NON-NLS-1$
      final String newState = st.hasMoreTokens() ? st.nextToken() : ""; //$NON-NLS-1$
      final String oldState = st.hasMoreTokens() ? st.nextToken() : null;
      return new ChangePiece(id, oldState, newState).toString();
    }

    if (token.startsWith(MOVE)) {
      // Format: M/<id>/<newMapId>/<newX>/<newY>/<newUnderId>/<oldMapId>/<oldX>/<oldY>/<oldUnderId>[/<playerId>]
      // MovePiece has a useful getDetails() — create the object and call toString().
      try {
        final SequenceEncoder.Decoder st =
          new SequenceEncoder.Decoder(token.substring(MOVE.length()), PARAM_SEPARATOR);
        final String id          = unwrapNull(st.nextToken());
        final String newMapId    = unwrapNull(st.nextToken());
        final int    newX        = Integer.parseInt(st.nextToken());
        final int    newY        = Integer.parseInt(st.nextToken());
        final String newUnderId  = unwrapNull(st.nextToken());
        final String oldMapId    = unwrapNull(st.nextToken());
        final int    oldX        = Integer.parseInt(st.nextToken());
        final int    oldY        = Integer.parseInt(st.nextToken());
        final String oldUnderId  = unwrapNull(st.nextToken());
        final String playerId    = st.hasMoreTokens() ? st.nextToken() : null;
        return new MovePiece(id, newMapId, new Point(newX, newY), newUnderId,
                             oldMapId, new Point(oldX, oldY), oldUnderId,
                             playerId).toString();
      }
      catch (RuntimeException e) {
        return "MovePiece[malformed token: " + e.getMessage() + "]"; //$NON-NLS-1$
      }
    }

    if (token.startsWith(EVENT_LIST)) {
      final String events = truncate(token.substring(EVENT_LIST.length()));
      return "StoreEvents[events=" + events + "]"; //$NON-NLS-1$
    }

    if (token.startsWith(EXT_CMD)) {
      // Format: EXT\t<name>\t<version>
      final SequenceEncoder.Decoder st =
        new SequenceEncoder.Decoder(token.substring(EXT_CMD.length()), '\t');
      final String name    = st.hasMoreTokens() ? st.nextToken() : ""; //$NON-NLS-1$
      final String version = st.hasMoreTokens() ? st.nextToken() : ""; //$NON-NLS-1$
      return "RegCmd[name=" + name + ",version=" + version + "]"; //$NON-NLS-1$
    }

    if (token.startsWith(AUDIO)) {
      final String clip = token.substring(AUDIO.length());
      return "PlayAudioClipCommand[clip=" + clip + "]"; //$NON-NLS-1$
    }

    if (token.startsWith(CHAT)) {
      // Chatter.DisplayText: getDetails() returns the message
      final String msg = truncate(token.substring(CHAT.length()));
      return "DisplayText[" + msg + "]"; //$NON-NLS-1$
    }

    // Unknown token — show truncated preview
    return "Unknown[" + truncate(token) + "]"; //$NON-NLS-1$
  }

  /**
   * Maps the string {@code "null"} to Java {@code null}; passes other values
   * through unchanged. Mirrors {@code BasicCommandEncoder.unwrapNull()}.
   *
   * @param s the string to check
   * @return {@code null} if {@code s} equals {@code "null"}, otherwise {@code s} unchanged
   */
  private static String unwrapNull(final String s) {
    return "null".equals(s) ? null : s; //$NON-NLS-1$
  }

  /**
   * Truncates {@code s} to {@value #PREVIEW_LEN} characters, appending
   * {@code "..."} when truncation occurs.
   *
   * @param s the string to truncate, may be {@code null}
   * @return truncated string with {@code "..."} suffix if longer than {@value #PREVIEW_LEN},
   *         or {@code null} if input is {@code null}
   */
  private static String truncate(final String s) {
    if (s == null) return null;
    return s.length() > PREVIEW_LEN ? s.substring(0, PREVIEW_LEN) + "..." : s; //$NON-NLS-1$
  }
}
