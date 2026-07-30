/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

/**
 * {@link CommandCodec} for {@link SetupCommandADT}.
 * Wire format: {@code "true"} or {@code "false"}.
 */
public class SetupCommandCodec implements CommandCodec {

  @Override
  public String getCommandType() { return SetupCommandADT.COMMAND_TYPE; }

  @Override
  public String encode(CommandADT command) {
    return String.valueOf(((SetupCommandADT) command).isGameStarting());
  }

  @Override
  public CommandADT decode(String encoded) {
    return new SetupCommandADT(Boolean.parseBoolean(encoded));
  }
}
