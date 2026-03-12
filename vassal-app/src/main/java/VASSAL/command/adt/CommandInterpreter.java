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

  private final Map<String, CommandCodec> codecs = new LinkedHashMap<>();

  /**
   * Creates a new interpreter pre-registered with a {@link NullCommandCodec}.
   */
  public CommandInterpreter() {
    registerCodec(NullCommandADT.COMMAND_TYPE, new NullCommandCodec());
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
   * <p>If the command has sub-commands, a compound encoding is produced:
   * the command itself and each sub-command are encoded individually and
   * joined with {@value #COMPOUND_SEPARATOR}, prefixed by
   * {@code COMPOUND:}.
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
      // Compound: encode this command + each sub-command
      final StringBuilder sb = new StringBuilder();
      sb.append(CompoundCommandADT.COMMAND_TYPE).append(TYPE_SEPARATOR);
      sb.append(encodeSingle(command));
      for (final CommandADT sub : subCommands) {
        sb.append(COMPOUND_SEPARATOR).append(encode(sub));
      }
      return sb.toString();
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
   * <p>If the string starts with {@code COMPOUND:}, each tab-delimited
   * token after the prefix is decoded recursively and appended as a
   * sub-command to the first decoded command.
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
   * Format: {@code COMPOUND:<first>\t<sub1>\t<sub2>...}
   */
  private CommandADT decodeCompound(String encoded) {
    // Strip "COMPOUND:" prefix
    final String body = encoded.substring(
        CompoundCommandADT.COMMAND_TYPE.length() + TYPE_SEPARATOR.length());

    final String[] parts = body.split(COMPOUND_SEPARATOR, -1);
    if (parts.length == 0) {
      return new NullCommandADT();
    }

    CommandADT result = decodeSingle(parts[0]);
    for (int i = 1; i < parts.length; i++) {
      result = result.append(decode(parts[i]));
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
