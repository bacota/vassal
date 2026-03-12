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

import java.awt.Point;

import VASSAL.tools.SequenceEncoder;

/**
 * {@link CommandCodec} for {@link MovePieceCommandADT}.
 *
 * <p>Wire format (fields separated by {@code '/'}):
 * {@code id/newMapId/newX/newY/newUnderneathId/oldMapId/oldX/oldY/oldUnderneathId/playerId}.
 * Null string fields are encoded as empty strings.
 */
public class MovePieceCommandCodec implements CommandCodec {

  private static final char SEP = '/';

  @Override
  public String getCommandType() {
    return MovePieceCommandADT.COMMAND_TYPE;
  }

  @Override
  public String encode(CommandADT command) {
    final MovePieceCommandADT c = (MovePieceCommandADT) command;
    return new SequenceEncoder(SEP)
        .append(c.getId())
        .append(nullToEmpty(c.getNewMapId()))
        .append(c.getNewPosition().x)
        .append(c.getNewPosition().y)
        .append(nullToEmpty(c.getNewUnderneathId()))
        .append(nullToEmpty(c.getOldMapId()))
        .append(c.getOldPosition().x)
        .append(c.getOldPosition().y)
        .append(nullToEmpty(c.getOldUnderneathId()))
        .append(nullToEmpty(c.getPlayerId()))
        .getValue();
  }

  @Override
  public CommandADT decode(String encoded) {
    final SequenceEncoder.Decoder st = new SequenceEncoder.Decoder(encoded, SEP);
    final String id = st.nextToken("");
    final String newMapId = emptyToNull(st.nextToken(""));
    final int newX = st.nextInt(0);
    final int newY = st.nextInt(0);
    final String newUnderneathId = emptyToNull(st.nextToken(""));
    final String oldMapId = emptyToNull(st.nextToken(""));
    final int oldX = st.nextInt(0);
    final int oldY = st.nextInt(0);
    final String oldUnderneathId = emptyToNull(st.nextToken(""));
    final String playerId = emptyToNull(st.nextToken(""));
    return new MovePieceCommandADT(id,
        newMapId, new Point(newX, newY), newUnderneathId,
        oldMapId, new Point(oldX, oldY), oldUnderneathId,
        playerId);
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }

  private static String emptyToNull(String s) {
    return (s == null || s.isEmpty()) ? null : s;
  }
}
