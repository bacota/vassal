/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.tools.SequenceEncoder;

/**
 * {@link CommandCodec} for {@link LockScenarioOptionsTabADT}.
 *
 * <p>Wire format (fields separated by {@code '/'}):
 * {@code tabName/lockedBy/lockedPw/lockedDt/oldLockedBy/oldLockedPw/oldLockedDt}
 */
public class LockScenarioOptionsTabCodec implements CommandCodec {

  private static final char SEP = '/';

  @Override
  public String getCommandType() { return LockScenarioOptionsTabADT.COMMAND_TYPE; }

  @Override
  public String encode(CommandADT command) {
    final LockScenarioOptionsTabADT c = (LockScenarioOptionsTabADT) command;
    return new SequenceEncoder(SEP)
        .append(nullToEmpty(c.getTabName()))
        .append(nullToEmpty(c.getLockedBy()))
        .append(nullToEmpty(c.getLockedPw()))
        .append(nullToEmpty(c.getLockedDt()))
        .append(nullToEmpty(c.getOldLockedBy()))
        .append(nullToEmpty(c.getOldLockedPw()))
        .append(nullToEmpty(c.getOldLockedDt()))
        .getValue();
  }

  @Override
  public CommandADT decode(String encoded) {
    final SequenceEncoder.Decoder st = new SequenceEncoder.Decoder(encoded, SEP);
    return new LockScenarioOptionsTabADT(
        st.nextToken(""), st.nextToken(""), st.nextToken(""), st.nextToken(""),
        st.nextToken(""), st.nextToken(""), st.nextToken(""));
  }

  private static String nullToEmpty(String s) { return s == null ? "" : s; }
}
