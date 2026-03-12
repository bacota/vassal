/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.tools.SequenceEncoder;

/**
 * {@link CommandCodec} for {@link PlayerRosterAddADT}.
 * Wire format: {@code playerId/playerName/side}.
 */
public class PlayerRosterAddCodec implements CommandCodec {

  private static final char SEP = '/';

  @Override
  public String getCommandType() { return PlayerRosterAddADT.COMMAND_TYPE; }

  @Override
  public String encode(CommandADT command) {
    final PlayerRosterAddADT c = (PlayerRosterAddADT) command;
    return new SequenceEncoder(SEP)
        .append(c.getPlayerId())
        .append(c.getPlayerName())
        .append(c.getSide())
        .getValue();
  }

  @Override
  public CommandADT decode(String encoded) {
    final SequenceEncoder.Decoder st = new SequenceEncoder.Decoder(encoded, SEP);
    return new PlayerRosterAddADT(st.nextToken(""), st.nextToken(""), st.nextToken(""));
  }
}
