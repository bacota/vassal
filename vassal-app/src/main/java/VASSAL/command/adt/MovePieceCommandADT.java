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

import java.awt.Point;

import VASSAL.command.MovePiece;

/**
 * ADT representation of {@link VASSAL.command.MovePiece}.
 *
 * <p>Moves a game piece to a new location.  The undo command swaps old and
 * new positional data.
 */
public class MovePieceCommandADT extends AbstractCommandADT {

  /** Command type identifier used by {@link CommandInterpreter}. */
  public static final String COMMAND_TYPE = "MOVE_PIECE";

  private final String id;
  private final String newMapId;
  private final Point newPosition;
  private final String newUnderneathId;
  private final String oldMapId;
  private final Point oldPosition;
  private final String oldUnderneathId;
  private final String playerId;

  public MovePieceCommandADT(String id,
                              String newMapId, Point newPosition, String newUnderneathId,
                              String oldMapId, Point oldPosition, String oldUnderneathId,
                              String playerId) {
    this.id = id;
    this.newMapId = newMapId;
    this.newPosition = newPosition;
    this.newUnderneathId = newUnderneathId;
    this.oldMapId = oldMapId;
    this.oldPosition = oldPosition;
    this.oldUnderneathId = oldUnderneathId;
    this.playerId = playerId;
  }

  @Override
  public String getCommandType() {
    return COMMAND_TYPE;
  }

  /**
   * Delegates execution to {@link MovePiece}.
   */
  @Override
  protected void executeInternal() {
    new MovePiece(id, newMapId, newPosition, newUnderneathId,
        oldMapId, oldPosition, oldUnderneathId, playerId).execute();
  }

  /**
   * Returns a {@code MovePieceCommandADT} that moves the piece back.
   */
  @Override
  protected CommandADT createUndoCommand() {
    return new MovePieceCommandADT(id,
        oldMapId, oldPosition, oldUnderneathId,
        newMapId, newPosition, newUnderneathId,
        playerId);
  }

  @Override
  public String getDetails() {
    return "id=" + id + ",map=" + newMapId + ",position=" + newPosition + ",under=" + newUnderneathId; //NON-NLS
  }

  public String getId() { return id; }
  public String getNewMapId() { return newMapId; }
  public Point getNewPosition() { return newPosition; }
  public String getNewUnderneathId() { return newUnderneathId; }
  public String getOldMapId() { return oldMapId; }
  public Point getOldPosition() { return oldPosition; }
  public String getOldUnderneathId() { return oldUnderneathId; }
  public String getPlayerId() { return playerId; }
}
