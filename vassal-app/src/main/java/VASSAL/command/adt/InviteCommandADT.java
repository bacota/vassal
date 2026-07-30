/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.tools.SequenceEncoder;

/**
 * ADT representation of {@link VASSAL.chat.InviteCommand}.
 *
 * <p>Stores player, playerId, and room.  Because {@link VASSAL.chat.InviteCommand}
 * requires a live {@link VASSAL.chat.ChatServerConnection}, this ADT does not
 * attempt to execute the invite dialog; {@link #isLoggable()} returns
 * {@code false}.
 */
public class InviteCommandADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "CHAT_INVITE";

  private final String player;
  private final String playerId;
  private final String room;

  public InviteCommandADT(String player, String playerId, String room) {
    this.player = player;
    this.playerId = playerId;
    this.room = room;
  }

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    // No-op: execution requires a live ChatServerConnection
  }

  @Override
  public boolean isLoggable() { return false; }

  @Override
  protected CommandADT createUndoCommand() { return new NullCommandADT(); }

  @Override
  public String getDetails() { return "player=" + player + ",room=" + room; } //NON-NLS

  public String getPlayer()   { return player; }
  public String getPlayerId() { return playerId; }
  public String getRoom()     { return room; }
}
