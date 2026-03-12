/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.tools.SequenceEncoder;

/**
 * {@link CommandCodec} for {@link AddSecretNoteCommandADT}.
 *
 * <p>Wire format (fields separated by {@code '/'}):
 * {@code name/owner/text/hidden/dateMillis/handle}
 * where {@code dateMillis} is -1 when the date is null.
 */
public class AddSecretNoteCommandCodec implements CommandCodec {

  private static final char SEP = '/';

  @Override
  public String getCommandType() { return AddSecretNoteCommandADT.COMMAND_TYPE; }

  @Override
  public String encode(CommandADT command) {
    final AddSecretNoteCommandADT c = (AddSecretNoteCommandADT) command;
    return new SequenceEncoder(SEP)
        .append(nullToEmpty(c.getName()))
        .append(nullToEmpty(c.getOwner()))
        .append(nullToEmpty(c.getText()))
        .append(c.isHidden())
        .append(c.getDateMillis())
        .append(nullToEmpty(c.getHandle()))
        .getValue();
  }

  @Override
  public CommandADT decode(String encoded) {
    final SequenceEncoder.Decoder st = new SequenceEncoder.Decoder(encoded, SEP);
    final String name    = st.nextToken("");
    final String owner   = st.nextToken("");
    final String text    = st.nextToken("");
    final boolean hidden = st.nextBoolean(false);
    final long dateMs    = st.nextLong(-1L);
    final String handle  = st.nextToken("");
    return new AddSecretNoteCommandADT(name, owner, text, hidden, dateMs, handle);
  }

  private static String nullToEmpty(String s) { return s == null ? "" : s; }
}
