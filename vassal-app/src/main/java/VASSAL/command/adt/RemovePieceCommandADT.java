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
import VASSAL.command.RemovePiece;
import VASSAL.counters.GamePiece;

/**
 * ADT representation of {@link VASSAL.command.RemovePiece}.
 *
 * <p>Removes a game piece from the game state.  The undo command is an
 * {@link AddPieceCommandADT} that restores the piece to its pre-removal state.
 * Because the undo data (piece type and state) is only available while the
 * piece still exists, {@link #getUndoCommand()} must be called <em>before</em>
 * {@link #execute()} is invoked.
 */
public class RemovePieceCommandADT extends AbstractCommandADT {

  /** Command type identifier used by {@link CommandInterpreter}. */
  public static final String COMMAND_TYPE = "REMOVE_PIECE";

  private final String pieceId;

  /**
   * @param pieceId the unique identifier of the piece to remove
   */
  public RemovePieceCommandADT(String pieceId) {
    this.pieceId = pieceId;
  }

  @Override
  public String getCommandType() {
    return COMMAND_TYPE;
  }

  /**
   * Delegates execution to {@link RemovePiece}.
   */
  @Override
  protected void executeInternal() {
    new RemovePiece(pieceId).execute();
  }

  /**
   * Returns an {@link AddPieceCommandADT} that restores the piece.
   * The piece must still exist in the game state when this method is called.
   * Returns {@link NullCommandADT} if the piece cannot be found.
   */
  @Override
  protected CommandADT createUndoCommand() {
    final GameModule gm = GameModule.getGameModule();
    if (gm == null) return new NullCommandADT();
    final GamePiece target = gm.getGameState().getPieceForId(pieceId);
    if (target == null) return new NullCommandADT();
    return new AddPieceCommandADT(target.getId(), target.getType(), target.getState());
  }

  @Override
  public String getDetails() {
    return "id=" + pieceId; //NON-NLS
  }

  public String getPieceId() { return pieceId; }
}
