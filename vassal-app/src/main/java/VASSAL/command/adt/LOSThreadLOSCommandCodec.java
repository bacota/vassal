/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.tools.SequenceEncoder;

/**
 * {@link CommandCodec} for {@link LOSThreadLOSCommandADT}.
 *
 * <p>Wire format (fields separated by {@code '/'}):
 * {@code threadId/nAX/nAY/nArX/nArY/nPersist/nMirror/reset/oAX/oAY/oArX/oArY/oPersist/oMirror}
 */
public class LOSThreadLOSCommandCodec implements CommandCodec {

  private static final char SEP = '/';

  @Override
  public String getCommandType() { return LOSThreadLOSCommandADT.COMMAND_TYPE; }

  @Override
  public String encode(CommandADT command) {
    final LOSThreadLOSCommandADT c = (LOSThreadLOSCommandADT) command;
    return new SequenceEncoder(SEP)
        .append(c.getThreadId())
        .append(c.getNewAnchorX()).append(c.getNewAnchorY())
        .append(c.getNewArrowX()).append(c.getNewArrowY())
        .append(c.isNewPersisting()).append(c.isNewMirroring())
        .append(c.isReset())
        .append(c.getOldAnchorX()).append(c.getOldAnchorY())
        .append(c.getOldArrowX()).append(c.getOldArrowY())
        .append(c.isOldPersisting()).append(c.isOldMirroring())
        .getValue();
  }

  @Override
  public CommandADT decode(String encoded) {
    final SequenceEncoder.Decoder st = new SequenceEncoder.Decoder(encoded, SEP);
    final String threadId = st.nextToken("");
    final int nAX = st.nextInt(0), nAY = st.nextInt(0);
    final int nArX = st.nextInt(0), nArY = st.nextInt(0);
    final boolean nP = st.nextBoolean(false), nM = st.nextBoolean(false);
    final boolean reset = st.nextBoolean(false);
    final int oAX = st.nextInt(0), oAY = st.nextInt(0);
    final int oArX = st.nextInt(0), oArY = st.nextInt(0);
    final boolean oP = st.nextBoolean(false), oM = st.nextBoolean(false);
    return new LOSThreadLOSCommandADT(threadId,
        nAX, nAY, nArX, nArY, nP, nM, reset,
        oAX, oAY, oArX, oArY, oP, oM);
  }
}
