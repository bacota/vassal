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

import VASSAL.build.GameModule;
import VASSAL.command.AddPiece;
import VASSAL.counters.GamePiece;

/**
 * ADT representation of {@link VASSAL.command.AddPiece}.
 *
 * <p>Adds a game piece to the game state.  The piece is identified by its
 * serialised type descriptor and initial state string.  Undo removes the piece.
 */
public class AddPieceCommandADT extends AbstractCommandADT {

  /** Command type identifier used by {@link CommandInterpreter}. */
  public static final String COMMAND_TYPE = "ADD_PIECE";

  private final String pieceId;
  private final String pieceType;
  private final String state;

  /**
   * @param pieceId   the unique identifier for the piece
   * @param pieceType the serialised type descriptor string
   * @param state     the initial state string
   */
  public AddPieceCommandADT(String pieceId, String pieceType, String state) {
    this.pieceId = pieceId;
    this.pieceType = pieceType;
    this.state = state;
  }

  @Override
  public String getCommandType() {
    return COMMAND_TYPE;
  }

  /**
   * Creates the {@link GamePiece} via {@link GameModule#createPiece} and
   * adds it via {@link AddPiece}.
   */
  @Override
  protected void executeInternal() {
    final GameModule gm = GameModule.getGameModule();
    if (gm == null) return;
    final GamePiece p = gm.createPiece(pieceType);
    if (p != null) {
      p.setId(pieceId);
      new AddPiece(p, state).execute();
    }
  }

  /**
   * Returns a {@link RemovePieceCommandADT} that removes the piece.
   */
  @Override
  protected CommandADT createUndoCommand() {
    return new RemovePieceCommandADT(pieceId);
  }

  @Override
  public String getDetails() {
    return "id=" + pieceId + ",type=" + pieceType; //NON-NLS
  }

  public String getPieceId() { return pieceId; }
  public String getPieceType() { return pieceType; }
  public String getState() { return state; }
}
