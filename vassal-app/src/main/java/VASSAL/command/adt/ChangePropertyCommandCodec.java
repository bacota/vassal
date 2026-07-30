/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.tools.SequenceEncoder;

/**
 * {@link CommandCodec} for {@link ChangePropertyCommandADT}.
 * Wire format: {@code containerId/propertyName/oldValue/newValue}.
 */
public class ChangePropertyCommandCodec implements CommandCodec {

  private static final char SEP = '/';

  @Override
  public String getCommandType() { return ChangePropertyCommandADT.COMMAND_TYPE; }

  @Override
  public String encode(CommandADT command) {
    final ChangePropertyCommandADT c = (ChangePropertyCommandADT) command;
    return new SequenceEncoder(SEP)
        .append(nullToEmpty(c.getContainerId()))
        .append(nullToEmpty(c.getPropertyName()))
        .append(nullToEmpty(c.getOldValue()))
        .append(nullToEmpty(c.getNewValue()))
        .getValue();
  }

  @Override
  public CommandADT decode(String encoded) {
    final SequenceEncoder.Decoder st = new SequenceEncoder.Decoder(encoded, SEP);
    return new ChangePropertyCommandADT(
        st.nextToken(""), st.nextToken(""), st.nextToken(""), st.nextToken(""));
  }

  private static String nullToEmpty(String s) { return s == null ? "" : s; }
}
