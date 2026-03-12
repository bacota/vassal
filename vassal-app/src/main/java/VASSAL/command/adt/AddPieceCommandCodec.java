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
 * {@link CommandCodec} for {@link AddPieceCommandADT}.
 *
 * <p>Wire format (fields separated by {@code '/'}):
 * {@code pieceId/pieceType/state}.
 */
public class AddPieceCommandCodec implements CommandCodec {

  private static final char SEP = '/';

  @Override
  public String getCommandType() {
    return AddPieceCommandADT.COMMAND_TYPE;
  }

  @Override
  public String encode(CommandADT command) {
    final AddPieceCommandADT c = (AddPieceCommandADT) command;
    return new SequenceEncoder(SEP)
        .append(c.getPieceId())
        .append(c.getPieceType())
        .append(c.getState())
        .getValue();
  }

  @Override
  public CommandADT decode(String encoded) {
    final SequenceEncoder.Decoder st = new SequenceEncoder.Decoder(encoded, SEP);
    final String pieceId = st.nextToken("");
    final String pieceType = st.nextToken("");
    final String state = st.nextToken("");
    return new AddPieceCommandADT(pieceId, pieceType, state);
  }
}
