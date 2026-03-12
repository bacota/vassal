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

/**
 * Codec interface for a specific {@link CommandADT} type.
 *
 * <p>Each command type that needs to be serialized registers a
 * {@code CommandCodec} implementation with a {@link CommandInterpreter}.
 * The interpreter uses {@link #getCommandType()} as the registry key and
 * as the prefix written to encoded strings.
 */
public interface CommandCodec {

  /**
   * Encodes {@code command} into a String representation.
   *
   * @param command the command to encode; never {@code null}
   * @return the encoded string; never {@code null}
   */
  String encode(CommandADT command);

  /**
   * Decodes {@code encoded} back into a {@link CommandADT}.
   *
   * @param encoded the previously encoded string; never {@code null}
   * @return the decoded command; never {@code null}
   */
  CommandADT decode(String encoded);

  /**
   * Returns the command type identifier that this codec handles.
   * Must match the value returned by {@link CommandADT#getCommandType()}
   * for the commands this codec processes.
   *
   * @return the command type string; never {@code null}
   */
  String getCommandType();
}
