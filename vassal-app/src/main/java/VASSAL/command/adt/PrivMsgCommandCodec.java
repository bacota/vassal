/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.tools.SequenceEncoder;

/** {@link CommandCodec} for {@link PrivMsgCommandADT}. Wire: {@code senderName/message}. */
public class PrivMsgCommandCodec implements CommandCodec {

  private static final char SEP = '/';

  @Override
  public String getCommandType() { return PrivMsgCommandADT.COMMAND_TYPE; }

  @Override
  public String encode(CommandADT command) {
    final PrivMsgCommandADT c = (PrivMsgCommandADT) command;
    return new SequenceEncoder(SEP).append(c.getSenderName()).append(c.getMessage()).getValue();
  }

  @Override
  public CommandADT decode(String encoded) {
    final SequenceEncoder.Decoder st = new SequenceEncoder.Decoder(encoded, SEP);
    return new PrivMsgCommandADT(st.nextToken(""), st.nextToken(""));
  }
}
