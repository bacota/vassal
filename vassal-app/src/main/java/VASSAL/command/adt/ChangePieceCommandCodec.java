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
 * {@link CommandCodec} for {@link ChangePieceCommandADT}.
 *
 * <p>Wire format: {@code id/oldState/newState} using {@link SequenceEncoder}
 * with {@code '/'} as the separator.  A missing/null token is encoded as the
 * empty string.
 */
public class ChangePieceCommandCodec implements CommandCodec {

  private static final char SEP = '/';

  @Override
  public String getCommandType() {
    return ChangePieceCommandADT.COMMAND_TYPE;
  }

  @Override
  public String encode(CommandADT command) {
    final ChangePieceCommandADT c = (ChangePieceCommandADT) command;
    return new SequenceEncoder(SEP)
        .append(c.getId())
        .append(nullToEmpty(c.getOldState()))
        .append(c.getNewState())
        .getValue();
  }

  @Override
  public CommandADT decode(String encoded) {
    final SequenceEncoder.Decoder st = new SequenceEncoder.Decoder(encoded, SEP);
    final String id = st.nextToken("");
    final String oldState = emptyToNull(st.nextToken(""));
    final String newState = st.nextToken("");
    return new ChangePieceCommandADT(id, oldState, newState);
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }

  private static String emptyToNull(String s) {
    return s.isEmpty() ? null : s;
  }
}
