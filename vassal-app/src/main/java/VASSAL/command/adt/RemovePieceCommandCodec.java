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

/**
 * {@link CommandCodec} for {@link RemovePieceCommandADT}.
 *
 * <p>Wire format: the piece ID string (no separator needed).
 */
public class RemovePieceCommandCodec implements CommandCodec {

  @Override
  public String getCommandType() {
    return RemovePieceCommandADT.COMMAND_TYPE;
  }

  @Override
  public String encode(CommandADT command) {
    return ((RemovePieceCommandADT) command).getPieceId();
  }

  @Override
  public CommandADT decode(String encoded) {
    return new RemovePieceCommandADT(encoded);
  }
}
