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
 * {@link CommandCodec} implementation for {@link NullCommandADT}.
 *
 * <p>Encodes a null command as an empty string and decodes any string
 * (typically empty) back to a {@link NullCommandADT}.
 */
public class NullCommandCodec implements CommandCodec {

  @Override
  public String getCommandType() {
    return NullCommandADT.COMMAND_TYPE;
  }

  /**
   * Encodes the {@link NullCommandADT} as an empty string.
   *
   * @param command the command to encode (expected to be a {@link NullCommandADT})
   * @return an empty string
   */
  @Override
  public String encode(CommandADT command) {
    return "";
  }

  /**
   * Decodes any string (typically empty) into a new {@link NullCommandADT}.
   *
   * @param encoded the encoded string
   * @return a new {@link NullCommandADT}
   */
  @Override
  public CommandADT decode(String encoded) {
    return new NullCommandADT();
  }
}
