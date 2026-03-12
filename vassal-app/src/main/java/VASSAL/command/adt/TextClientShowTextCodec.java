/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

/** {@link CommandCodec} for {@link TextClientShowTextADT}. Wire: the message string. */
public class TextClientShowTextCodec implements CommandCodec {

  @Override
  public String getCommandType() { return TextClientShowTextADT.COMMAND_TYPE; }

  @Override
  public String encode(CommandADT command) {
    return ((TextClientShowTextADT) command).getMessage();
  }

  @Override
  public CommandADT decode(String encoded) { return new TextClientShowTextADT(encoded); }
}
