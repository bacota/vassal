/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import VASSAL.build.GameModule;
import VASSAL.build.module.Map;
import VASSAL.build.module.map.BoardPicker;
import VASSAL.build.module.map.boardPicker.Board;

/**
 * ADT representation of {@link BoardPicker.SetBoards}.
 *
 * <p>Each board entry stores its name, whether it is reversed, and its
 * relative position ({@code x}, {@code y}).  When executed, the appropriate
 * {@link BoardPicker} is found via the {@link Map} identified by
 * {@code mapId}, and the boards are set.
 */
public class BoardPickerSetBoardsADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "BOARD_PICKER_SET";

  /** Immutable record describing one board in the selection. */
  public static final class BoardEntry {
    public final String name;
    public final boolean reversed;
    public final int relX;
    public final int relY;

    public BoardEntry(String name, boolean reversed, int relX, int relY) {
      this.name = name;
      this.reversed = reversed;
      this.relX = relX;
      this.relY = relY;
    }
  }

  private final String mapId;
  private final List<BoardEntry> boardEntries;

  public BoardPickerSetBoardsADT(String mapId, List<BoardEntry> boardEntries) {
    this.mapId = mapId;
    this.boardEntries = new ArrayList<>(boardEntries);
  }

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    final GameModule gm = GameModule.getGameModule();
    if (gm == null) return;
    for (final Map map : gm.getComponentsOf(Map.class)) {
      if (mapId.equals(map.getIdentifier()) || mapId.equals(map.getConfigureName())) {
        final BoardPicker picker = map.getBoardPicker();
        if (picker == null) return;
        final List<Board> boards = new ArrayList<>();
        for (final BoardEntry entry : boardEntries) {
          Board b = picker.getBoard(entry.name);
          if (b != null) {
            if (boards.contains(b)) {
              b = b.copy();
            }
            b.setReversed(entry.reversed);
            b.relativePosition().move(entry.relX, entry.relY);
            boards.add(b);
          }
        }
        new BoardPicker.SetBoards(picker, boards).execute();
        return;
      }
    }
  }

  @Override
  protected CommandADT createUndoCommand() { return new NullCommandADT(); }

  @Override
  public String getDetails() { return "mapId=" + mapId + ",boards=" + boardEntries.size(); } //NON-NLS

  public String getMapId() { return mapId; }
  public List<BoardEntry> getBoardEntries() { return Collections.unmodifiableList(boardEntries); }
}
