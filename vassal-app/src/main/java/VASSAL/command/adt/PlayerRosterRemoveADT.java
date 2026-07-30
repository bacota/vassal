/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.build.GameModule;
import VASSAL.build.module.PlayerRoster;

/**
 * ADT representation of {@link PlayerRoster.Remove}.
 *
 * <p>When executed, removes the player entry with the given ID from the
 * {@link PlayerRoster}.
 */
public class PlayerRosterRemoveADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "PLAYER_ROSTER_REMOVE";

  private final String playerId;

  public PlayerRosterRemoveADT(String playerId) {
    this.playerId = playerId;
  }

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    final GameModule gm = GameModule.getGameModule();
    if (gm == null) return;
    final PlayerRoster pr = gm.getPlayerRoster();
    if (pr != null) {
      new PlayerRoster.Remove(pr, playerId).execute();
    }
  }

  @Override
  protected CommandADT createUndoCommand() { return new NullCommandADT(); }

  @Override
  public String getDetails() { return "id=" + playerId; } //NON-NLS

  public String getPlayerId() { return playerId; }
}
