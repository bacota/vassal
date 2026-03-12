/*
 *
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

import java.util.LinkedHashMap;
import java.util.Map;

import VASSAL.tools.SequenceEncoder;

/**
 * Interpreter for {@link CommandADT} instances that manages encoding,
 * decoding, and execution.
 *
 * <p>The interpreter maintains a registry of {@link CommandCodec} instances
 * keyed by command-type string.  It handles compound commands by joining
 * encoded sub-commands with a delimiter and writing a
 * {@link CompoundCommandADT#COMMAND_TYPE} prefix.
 *
 * <p>A {@link NullCommandCodec} is pre-registered automatically.
 *
 * <h3>Wire format</h3>
 * <ul>
 *   <li>Single command: {@code <type>:<payload>}</li>
 *   <li>Compound command: {@code COMPOUND:<sub1>\t<sub2>\t...}</li>
 * </ul>
 * where {@code <sub_n>} is itself a fully encoded single or compound command
 * string (with {@code :} separating type from payload).
 */
public class CommandInterpreter {

  /** Separator between the command-type prefix and the payload. */
  static final String TYPE_SEPARATOR = ":";

  /** Delimiter used to separate sub-commands in a compound encoding. */
  static final String COMPOUND_SEPARATOR = "\t";

  /** Character form of {@link #COMPOUND_SEPARATOR} used with {@link SequenceEncoder}. */
  static final char COMPOUND_SEP_CHAR = '\t';

  private final Map<String, CommandCodec> codecs = new LinkedHashMap<>();

  /**
   * Creates a new interpreter pre-registered with codecs for all built-in
   * command types corresponding to the direct {@link VASSAL.command.Command}
   * subclasses in the VASSAL engine.
   */
  public CommandInterpreter() {
    // --- VASSAL.command ---
    registerCodec(NullCommandADT.COMMAND_TYPE, new NullCommandCodec());
    registerCodec(ChangePieceCommandADT.COMMAND_TYPE, new ChangePieceCommandCodec());
    registerCodec(MovePieceCommandADT.COMMAND_TYPE, new MovePieceCommandCodec());
    registerCodec(AddPieceCommandADT.COMMAND_TYPE, new AddPieceCommandCodec());
    registerCodec(RemovePieceCommandADT.COMMAND_TYPE, new RemovePieceCommandCodec());
    registerCodec(AlertCommandADT.COMMAND_TYPE, new AlertCommandCodec());
    registerCodec(PlayAudioClipCommandADT.COMMAND_TYPE, new PlayAudioClipCommandCodec());
    registerCodec(SetPersistentPropertyCommandADT.COMMAND_TYPE, new SetPersistentPropertyCommandCodec());
    registerCodec(FlareCommandADT.COMMAND_TYPE, new FlareCommandCodec());
    registerCodec(ConditionalCommandADT.COMMAND_TYPE, new ConditionalCommandCodec(this));
    // --- VASSAL.build.module / GameState ---
    registerCodec(SetupCommandADT.COMMAND_TYPE, new SetupCommandCodec());
    registerCodec(BasicLoggerUndoCommandADT.COMMAND_TYPE, new BasicLoggerUndoCommandCodec());
    registerCodec(BasicLoggerLogCommandADT.COMMAND_TYPE, new BasicLoggerLogCommandCodec(this));
    registerCodec(ModuleExtensionRegCmdADT.COMMAND_TYPE, new ModuleExtensionRegCmdCodec());
    registerCodec(ObscurableOptionsSetAllowedADT.COMMAND_TYPE, new ObscurableOptionsSetAllowedCodec());
    registerCodec(NewGameIndicatorMarkGameNotNewADT.COMMAND_TYPE, new NewGameIndicatorMarkGameNotNewCodec());
    registerCodec(ChatterDisplayTextADT.COMMAND_TYPE, new ChatterDisplayTextCodec());
    registerCodec(NotesWindowSetScenarioNoteADT.COMMAND_TYPE, new NotesWindowSetScenarioNoteCodec());
    registerCodec(NotesWindowSetPublicNoteADT.COMMAND_TYPE, new NotesWindowSetPublicNoteCodec());
    registerCodec(PlayerRosterAddADT.COMMAND_TYPE, new PlayerRosterAddCodec());
    registerCodec(PlayerRosterRemoveADT.COMMAND_TYPE, new PlayerRosterRemoveCodec());
    registerCodec(EventLogStoreEventsADT.COMMAND_TYPE, new EventLogStoreEventsCodec());
    // --- Component-identified ---
    registerCodec(TurnTrackerSetTurnADT.COMMAND_TYPE, new TurnTrackerSetTurnCodec());
    registerCodec(GlobalPropertySetGlobalPropertyADT.COMMAND_TYPE, new GlobalPropertySetGlobalPropertyCodec());
    registerCodec(LOSThreadLOSCommandADT.COMMAND_TYPE, new LOSThreadLOSCommandCodec());
    registerCodec(SpecialDiceButtonShowResultsADT.COMMAND_TYPE, new SpecialDiceButtonShowResultsCodec());
    registerCodec(BoardPickerSetBoardsADT.COMMAND_TYPE, new BoardPickerSetBoardsCodec());
    registerCodec(DeckLoadDeckCommandADT.COMMAND_TYPE, new DeckLoadDeckCommandCodec());
    registerCodec(LockScenarioOptionsTabADT.COMMAND_TYPE, new LockScenarioOptionsTabCodec());
    // --- Notes / Secret ---
    registerCodec(AddSecretNoteCommandADT.COMMAND_TYPE, new AddSecretNoteCommandCodec());
    registerCodec(SetPrivateTextCommandADT.COMMAND_TYPE, new SetPrivateTextCommandCodec());
    registerCodec(ChangePropertyCommandADT.COMMAND_TYPE, new ChangePropertyCommandCodec());
    // --- Chat (not loggable) ---
    registerCodec(InviteCommandADT.COMMAND_TYPE, new InviteCommandCodec());
    registerCodec(PrivMsgCommandADT.COMMAND_TYPE, new PrivMsgCommandCodec());
    registerCodec(SynchCommandADT.COMMAND_TYPE, new SynchCommandCodec());
    registerCodec(SoundEncoderCmdADT.COMMAND_TYPE, new SoundEncoderCmdCodec());
    // --- TextClient ---
    registerCodec(TextClientShowTextADT.COMMAND_TYPE, new TextClientShowTextCodec());
  }

  // -----------------------------------------------------------------------
  // Codec registry
  // -----------------------------------------------------------------------

  /**
   * Registers a {@link CommandCodec} for the given command type.
   * Replaces any previously registered codec for that type.
   *
   * @param commandType the command type string; must not be {@code null}
   * @param codec       the codec to register; must not be {@code null}
   * @throws IllegalArgumentException if {@code commandType} or {@code codec} is {@code null}
   */
  public void registerCodec(String commandType, CommandCodec codec) {
    if (commandType == null) {
      throw new IllegalArgumentException("commandType must not be null");
    }
    if (codec == null) {
      throw new IllegalArgumentException("codec must not be null");
    }
    codecs.put(commandType, codec);
  }

  /**
   * Removes the codec registered for the given command type.
   * Does nothing if no codec is registered for that type.
   *
   * @param commandType the command type to unregister; must not be {@code null}
   */
  public void unregisterCodec(String commandType) {
    if (commandType == null) {
      throw new IllegalArgumentException("commandType must not be null");
    }
    codecs.remove(commandType);
  }

  // -----------------------------------------------------------------------
  // Encode
  // -----------------------------------------------------------------------

  /**
   * Encodes {@code command} into a String.
   *
   * <p>If the command has sub-commands, a compound encoding is produced using
   * {@link SequenceEncoder} with a tab delimiter so that any tab characters
   * inside individual payloads are properly escaped:
   * {@code COMPOUND:<first>\t<sub1>\t<sub2>...}.
   *
   * @param command the command to encode; must not be {@code null}
   * @return the encoded string
   * @throws IllegalArgumentException if no codec is registered for the command type
   */
  public String encode(CommandADT command) {
    if (command == null) {
      throw new IllegalArgumentException("command must not be null");
    }

    final CommandADT[] subCommands = command.getSubCommands();
    if (subCommands.length > 0) {
      // Use SequenceEncoder so \t inside payloads is properly escaped
      final SequenceEncoder se = new SequenceEncoder(encodeSingle(command), COMPOUND_SEP_CHAR);
      for (final CommandADT sub : subCommands) {
        se.append(encode(sub));
      }
      return CompoundCommandADT.COMMAND_TYPE + TYPE_SEPARATOR + se.getValue();
    }

    return encodeSingle(command);
  }

  /**
   * Encodes a single (non-compound) command without its sub-commands.
   *
   * @param command the command to encode
   * @return {@code <type>:<payload>}
   * @throws IllegalArgumentException if no codec is registered for the command type
   */
  private String encodeSingle(CommandADT command) {
    final String type = command.getCommandType();
    final CommandCodec codec = codecs.get(type);
    if (codec == null) {
      throw new IllegalArgumentException(
          "No codec registered for command type: " + type);
    }
    return type + TYPE_SEPARATOR + codec.encode(command);
  }

  // -----------------------------------------------------------------------
  // Decode
  // -----------------------------------------------------------------------

  /**
   * Decodes an encoded string back into a {@link CommandADT}.
   *
   * <p>If the string starts with {@code COMPOUND:}, the body is parsed with
   * {@link SequenceEncoder.Decoder} (which handles escape sequences) and each
   * token is decoded recursively.
   *
   * @param encoded the encoded string; must not be {@code null}
   * @return the decoded command
   * @throws IllegalArgumentException if no codec is registered for the detected type,
   *                                   or if the encoded string is malformed
   */
  public CommandADT decode(String encoded) {
    if (encoded == null) {
      throw new IllegalArgumentException("encoded must not be null");
    }

    if (encoded.startsWith(CompoundCommandADT.COMMAND_TYPE + TYPE_SEPARATOR)) {
      return decodeCompound(encoded);
    }

    return decodeSingle(encoded);
  }

  /**
   * Decodes a compound-encoded string.
   * Format: {@code COMPOUND:<SequenceEncoder-value-with-tab-separator>}
   */
  private CommandADT decodeCompound(String encoded) {
    // Strip "COMPOUND:" prefix
    final String body = encoded.substring(
        CompoundCommandADT.COMMAND_TYPE.length() + TYPE_SEPARATOR.length());

    final SequenceEncoder.Decoder st = new SequenceEncoder.Decoder(body, COMPOUND_SEP_CHAR);
    if (!st.hasMoreTokens()) {
      return new NullCommandADT();
    }

    CommandADT result = decodeSingle(st.nextToken());
    while (st.hasMoreTokens()) {
      result = result.append(decode(st.nextToken()));
    }
    return result;
  }

  /**
   * Decodes a single (non-compound) encoded string.
   * Format: {@code <type>:<payload>}
   */
  private CommandADT decodeSingle(String encoded) {
    final int sep = encoded.indexOf(TYPE_SEPARATOR);
    if (sep < 0) {
      throw new IllegalArgumentException(
          "Malformed encoded command (missing type separator): " + encoded);
    }
    final String type = encoded.substring(0, sep);
    final String payload = encoded.substring(sep + TYPE_SEPARATOR.length());

    final CommandCodec codec = codecs.get(type);
    if (codec == null) {
      throw new IllegalArgumentException(
          "No codec registered for command type: " + type);
    }
    return codec.decode(payload);
  }

  // -----------------------------------------------------------------------
  // Execute
  // -----------------------------------------------------------------------

  /**
   * Executes {@code command}.
   *
   * <p>This is a convenience/central point for execution that may be used
   * for interception or logging.
   *
   * @param command the command to execute; must not be {@code null}
   */
  public void execute(CommandADT command) {
    if (command == null) {
      throw new IllegalArgumentException("command must not be null");
    }
    command.execute();
  }
}
