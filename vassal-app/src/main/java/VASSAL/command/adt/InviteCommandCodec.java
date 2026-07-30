/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.tools.SequenceEncoder;

/** {@link CommandCodec} for {@link InviteCommandADT}. Wire: {@code player/playerId/room}. */
public class InviteCommandCodec implements CommandCodec {

  private static final char SEP = '/';

  @Override
  public String getCommandType() { return InviteCommandADT.COMMAND_TYPE; }

  @Override
  public String encode(CommandADT command) {
    final InviteCommandADT c = (InviteCommandADT) command;
    return new SequenceEncoder(SEP)
        .append(c.getPlayer())
        .append(c.getPlayerId())
        .append(c.getRoom())
        .getValue();
  }

  @Override
  public CommandADT decode(String encoded) {
    final SequenceEncoder.Decoder st = new SequenceEncoder.Decoder(encoded, SEP);
    return new InviteCommandADT(st.nextToken(""), st.nextToken(""), st.nextToken(""));
  }
}
