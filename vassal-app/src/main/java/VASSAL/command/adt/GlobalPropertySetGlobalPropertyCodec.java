/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.tools.SequenceEncoder;

/**
 * {@link CommandCodec} for {@link GlobalPropertySetGlobalPropertyADT}.
 * Wire format: {@code propertyName/containerId/oldValue/newValue}.
 * Null/empty values are preserved as empty tokens.
 */
public class GlobalPropertySetGlobalPropertyCodec implements CommandCodec {

  private static final char SEP = '/';

  @Override
  public String getCommandType() { return GlobalPropertySetGlobalPropertyADT.COMMAND_TYPE; }

  @Override
  public String encode(CommandADT command) {
    final GlobalPropertySetGlobalPropertyADT c = (GlobalPropertySetGlobalPropertyADT) command;
    return new SequenceEncoder(SEP)
        .append(nullToEmpty(c.getPropertyName()))
        .append(nullToEmpty(c.getContainerId()))
        .append(nullToEmpty(c.getOldValue()))
        .append(nullToEmpty(c.getNewValue()))
        .getValue();
  }

  @Override
  public CommandADT decode(String encoded) {
    final SequenceEncoder.Decoder st = new SequenceEncoder.Decoder(encoded, SEP);
    return new GlobalPropertySetGlobalPropertyADT(
        st.nextToken(""), st.nextToken(""), st.nextToken(""), st.nextToken(""));
  }

  private static String nullToEmpty(String s) { return s == null ? "" : s; }
}
