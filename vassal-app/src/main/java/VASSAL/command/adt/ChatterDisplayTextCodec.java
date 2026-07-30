/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

/** {@link CommandCodec} for {@link ChatterDisplayTextADT}. Wire: the message string. */
public class ChatterDisplayTextCodec implements CommandCodec {

  @Override
  public String getCommandType() { return ChatterDisplayTextADT.COMMAND_TYPE; }

  @Override
  public String encode(CommandADT command) {
    return ((ChatterDisplayTextADT) command).getMessage();
  }

  @Override
  public CommandADT decode(String encoded) { return new ChatterDisplayTextADT(encoded); }
}
