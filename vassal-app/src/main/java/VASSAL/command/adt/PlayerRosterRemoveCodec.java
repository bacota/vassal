/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

/** {@link CommandCodec} for {@link PlayerRosterRemoveADT}. Wire: the player ID string. */
public class PlayerRosterRemoveCodec implements CommandCodec {

  @Override
  public String getCommandType() { return PlayerRosterRemoveADT.COMMAND_TYPE; }

  @Override
  public String encode(CommandADT command) {
    return ((PlayerRosterRemoveADT) command).getPlayerId();
  }

  @Override
  public CommandADT decode(String encoded) { return new PlayerRosterRemoveADT(encoded); }
}
