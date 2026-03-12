/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import java.util.ArrayList;
import java.util.List;

import VASSAL.tools.SequenceEncoder;

/**
 * {@link CommandCodec} for {@link BoardPickerSetBoardsADT}.
 *
 * <p>Wire format: {@code mapId\0boardName\0reversed\0relX\0relY\0boardName\0...}
 * where {@code \0} is ASCII NUL used as a field separator (safe because board
 * names/map IDs are printable strings).  Each board occupies 4 tokens.
 * An empty boards list produces just the mapId.
 */
public class BoardPickerSetBoardsCodec implements CommandCodec {

  private static final char SEP = '\0'; // NUL separator — not present in names

  @Override
  public String getCommandType() { return BoardPickerSetBoardsADT.COMMAND_TYPE; }

  @Override
  public String encode(CommandADT command) {
    final BoardPickerSetBoardsADT c = (BoardPickerSetBoardsADT) command;
    final SequenceEncoder se = new SequenceEncoder(SEP);
    se.append(c.getMapId());
    for (final BoardPickerSetBoardsADT.BoardEntry entry : c.getBoardEntries()) {
      se.append(entry.name)
        .append(entry.reversed)
        .append(entry.relX)
        .append(entry.relY);
    }
    return se.getValue();
  }

  @Override
  public CommandADT decode(String encoded) {
    final SequenceEncoder.Decoder st = new SequenceEncoder.Decoder(encoded, SEP);
    final String mapId = st.nextToken("");
    final List<BoardPickerSetBoardsADT.BoardEntry> entries = new ArrayList<>();
    while (st.hasMoreTokens()) {
      final String name = st.nextToken("");
      final boolean reversed = st.nextBoolean(false);
      final int relX = st.nextInt(0);
      final int relY = st.nextInt(0);
      entries.add(new BoardPickerSetBoardsADT.BoardEntry(name, reversed, relX, relY));
    }
    return new BoardPickerSetBoardsADT(mapId, entries);
  }
}
