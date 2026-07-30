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
 * ADT representation of {@link PlayerRoster.Add}.
 *
 * <p>When executed, adds a player entry (id, name, side) to the
 * {@link PlayerRoster}.
 */
public class PlayerRosterAddADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "PLAYER_ROSTER_ADD";

  private final String playerId;
  private final String playerName;
  private final String side;

  public PlayerRosterAddADT(String playerId, String playerName, String side) {
    this.playerId = playerId;
    this.playerName = playerName;
    this.side = side;
  }

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    final GameModule gm = GameModule.getGameModule();
    if (gm == null) return;
    final PlayerRoster pr = gm.getPlayerRoster();
    if (pr != null) {
      new PlayerRoster.Add(pr, playerId, playerName, side).execute();
    }
  }

  @Override
  protected CommandADT createUndoCommand() { return new NullCommandADT(); }

  @Override
  public String getDetails() { return "id=" + playerId + ",name=" + playerName + ",side=" + side; } //NON-NLS

  public String getPlayerId() { return playerId; }
  public String getPlayerName() { return playerName; }
  public String getSide() { return side; }
}
