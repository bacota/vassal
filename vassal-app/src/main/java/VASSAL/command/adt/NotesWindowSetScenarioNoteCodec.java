/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

/** {@link CommandCodec} for {@link NotesWindowSetScenarioNoteADT}. Wire: the message string. */
public class NotesWindowSetScenarioNoteCodec implements CommandCodec {

  @Override
  public String getCommandType() { return NotesWindowSetScenarioNoteADT.COMMAND_TYPE; }

  @Override
  public String encode(CommandADT command) {
    return ((NotesWindowSetScenarioNoteADT) command).getMessage();
  }

  @Override
  public CommandADT decode(String encoded) { return new NotesWindowSetScenarioNoteADT(encoded); }
}
