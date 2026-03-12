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

import VASSAL.command.ChangePiece;

/**
 * ADT representation of {@link VASSAL.command.ChangePiece}.
 *
 * <p>Changes the state of a game piece identified by {@code id}.
 * The undo command is another {@code ChangePieceCommandADT} with the
 * old and new states reversed.
 */
public class ChangePieceCommandADT extends AbstractCommandADT {

  /** Command type identifier used by {@link CommandInterpreter}. */
  public static final String COMMAND_TYPE = "CHANGE_PIECE";

  private final String id;
  private final String oldState;
  private final String newState;

  /**
   * @param id       the id of the game piece to change
   * @param oldState the previous state of the piece (may be {@code null})
   * @param newState the new state of the piece
   */
  public ChangePieceCommandADT(String id, String oldState, String newState) {
    this.id = id;
    this.oldState = oldState;
    this.newState = newState;
  }

  @Override
  public String getCommandType() {
    return COMMAND_TYPE;
  }

  /**
   * Delegates execution to {@link ChangePiece}.
   */
  @Override
  protected void executeInternal() {
    new ChangePiece(id, oldState, newState).execute();
  }

  /**
   * Returns a {@code ChangePieceCommandADT} that restores the old state.
   */
  @Override
  protected CommandADT createUndoCommand() {
    if (oldState != null) {
      return new ChangePieceCommandADT(id, newState, oldState);
    }
    return new NullCommandADT();
  }

  /**
   * Returns {@code true} if old and new states are equal and the command is atomic.
   */
  @Override
  public boolean isNull() {
    return newState != null && newState.equals(oldState) && isAtomic();
  }

  @Override
  public String getDetails() {
    return "id=" + id + ",oldState=" + oldState + ",newState=" + newState; //NON-NLS
  }

  public String getId() {
    return id;
  }

  public String getOldState() {
    return oldState;
  }

  public String getNewState() {
    return newState;
  }
}
