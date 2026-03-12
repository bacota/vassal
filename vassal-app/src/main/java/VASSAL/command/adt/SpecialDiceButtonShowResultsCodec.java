/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.tools.SequenceEncoder;

/**
 * {@link CommandCodec} for {@link SpecialDiceButtonShowResultsADT}.
 *
 * <p>Wire format: {@code buttonId/roll0/roll1/.../rollN} (SequenceEncoder with '/').
 */
public class SpecialDiceButtonShowResultsCodec implements CommandCodec {

  private static final char SEP = '/';

  @Override
  public String getCommandType() { return SpecialDiceButtonShowResultsADT.COMMAND_TYPE; }

  @Override
  public String encode(CommandADT command) {
    final SpecialDiceButtonShowResultsADT c = (SpecialDiceButtonShowResultsADT) command;
    final SequenceEncoder se = new SequenceEncoder(SEP);
    se.append(c.getButtonId());
    for (final int r : c.getRolls()) {
      se.append(r);
    }
    return se.getValue();
  }

  @Override
  public CommandADT decode(String encoded) {
    final SequenceEncoder.Decoder st = new SequenceEncoder.Decoder(encoded, SEP);
    final String buttonId = st.nextToken("");
    final java.util.List<Integer> rollList = new java.util.ArrayList<>();
    while (st.hasMoreTokens()) {
      rollList.add(st.nextInt(0));
    }
    final int[] rolls = new int[rollList.size()];
    for (int i = 0; i < rolls.length; i++) rolls[i] = rollList.get(i);
    return new SpecialDiceButtonShowResultsADT(buttonId, rolls);
  }
}
