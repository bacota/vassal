/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.tools.SequenceEncoder;

/**
 * {@link CommandCodec} for {@link TurnTrackerSetTurnADT}.
 * Wire format: {@code trackerId/newState/oldState}.
 */
public class TurnTrackerSetTurnCodec implements CommandCodec {

  private static final char SEP = '/';

  @Override
  public String getCommandType() { return TurnTrackerSetTurnADT.COMMAND_TYPE; }

  @Override
  public String encode(CommandADT command) {
    final TurnTrackerSetTurnADT c = (TurnTrackerSetTurnADT) command;
    return new SequenceEncoder(SEP)
        .append(c.getTrackerId())
        .append(c.getNewState())
        .append(c.getOldState())
        .getValue();
  }

  @Override
  public CommandADT decode(String encoded) {
    final SequenceEncoder.Decoder st = new SequenceEncoder.Decoder(encoded, SEP);
    return new TurnTrackerSetTurnADT(st.nextToken(""), st.nextToken(""), st.nextToken(""));
  }
}
