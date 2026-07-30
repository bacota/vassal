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

import VASSAL.tools.SequenceEncoder;

/**
 * {@link CommandCodec} for {@link SetPersistentPropertyCommandADT}.
 *
 * <p>Wire format (fields separated by {@code '/'}):
 * {@code id/key/oldValue/newValue}.
 * Null values are encoded as empty strings.
 */
public class SetPersistentPropertyCommandCodec implements CommandCodec {

  private static final char SEP = '/';

  @Override
  public String getCommandType() {
    return SetPersistentPropertyCommandADT.COMMAND_TYPE;
  }

  @Override
  public String encode(CommandADT command) {
    final SetPersistentPropertyCommandADT c = (SetPersistentPropertyCommandADT) command;
    return new SequenceEncoder(SEP)
        .append(nullToEmpty(c.getId()))
        .append(nullToEmpty(c.getKey()))
        .append(nullToEmpty(c.getOldValue()))
        .append(nullToEmpty(c.getNewValue()))
        .getValue();
  }

  @Override
  public CommandADT decode(String encoded) {
    final SequenceEncoder.Decoder st = new SequenceEncoder.Decoder(encoded, SEP);
    final String id = emptyToNull(st.nextToken(""));
    final String key = emptyToNull(st.nextToken(""));
    final String oldValue = emptyToNull(st.nextToken(""));
    final String newValue = emptyToNull(st.nextToken(""));
    return new SetPersistentPropertyCommandADT(id, key, oldValue, newValue);
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }

  private static String emptyToNull(String s) {
    return (s == null || s.isEmpty()) ? null : s;
  }
}
