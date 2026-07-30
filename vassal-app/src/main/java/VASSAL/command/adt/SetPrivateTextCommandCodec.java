/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.tools.SequenceEncoder;

/**
 * {@link CommandCodec} for {@link SetPrivateTextCommandADT}.
 * Wire format: {@code owner/text} (SequenceEncoder with '/').
 */
public class SetPrivateTextCommandCodec implements CommandCodec {

  private static final char SEP = '/';

  @Override
  public String getCommandType() { return SetPrivateTextCommandADT.COMMAND_TYPE; }

  @Override
  public String encode(CommandADT command) {
    final SetPrivateTextCommandADT c = (SetPrivateTextCommandADT) command;
    return new SequenceEncoder(SEP).append(c.getOwner()).append(c.getText()).getValue();
  }

  @Override
  public CommandADT decode(String encoded) {
    final SequenceEncoder.Decoder st = new SequenceEncoder.Decoder(encoded, SEP);
    return new SetPrivateTextCommandADT(st.nextToken(""), st.nextToken(""));
  }
}
