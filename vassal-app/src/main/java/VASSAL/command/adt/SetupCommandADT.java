/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.build.GameModule;
import VASSAL.build.module.GameState;

/**
 * ADT representation of {@link GameState.SetupCommand}.
 *
 * <p>When executed, calls {@link GameState#setup(boolean)} to start or end a game.
 */
public class SetupCommandADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "GAME_SETUP";

  private final boolean gameStarting;

  public SetupCommandADT(boolean gameStarting) {
    this.gameStarting = gameStarting;
  }

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    new GameState.SetupCommand(gameStarting).execute();
  }

  @Override
  protected CommandADT createUndoCommand() { return new NullCommandADT(); }

  @Override
  public String getDetails() { return "gameStarting=" + gameStarting; } //NON-NLS

  public boolean isGameStarting() { return gameStarting; }
}
