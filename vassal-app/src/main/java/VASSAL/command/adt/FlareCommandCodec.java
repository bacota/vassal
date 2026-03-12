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

import VASSAL.tools.SequenceEncoder;

/**
 * {@link CommandCodec} for {@link FlareCommandADT}.
 *
 * <p>Wire format (fields separated by {@code '/'}):
 * {@code flareId/clickX/clickY}.
 */
public class FlareCommandCodec implements CommandCodec {

  private static final char SEP = '/';

  @Override
  public String getCommandType() {
    return FlareCommandADT.COMMAND_TYPE;
  }

  @Override
  public String encode(CommandADT command) {
    final FlareCommandADT c = (FlareCommandADT) command;
    return new SequenceEncoder(SEP)
        .append(c.getFlareId())
        .append(c.getClickX())
        .append(c.getClickY())
        .getValue();
  }

  @Override
  public CommandADT decode(String encoded) {
    final SequenceEncoder.Decoder st = new SequenceEncoder.Decoder(encoded, SEP);
    final String flareId = st.nextToken("");
    final int clickX = st.nextInt(0);
    final int clickY = st.nextInt(0);
    return new FlareCommandADT(flareId, clickX, clickY);
  }
}
