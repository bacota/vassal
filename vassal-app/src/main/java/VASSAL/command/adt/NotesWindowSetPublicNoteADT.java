/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.build.GameModule;
import VASSAL.build.module.NotesWindow;

/**
 * ADT representation of {@link NotesWindow.SetPublicNote}.
 *
 * <p>When executed, sets the public note text in the first
 * {@link NotesWindow} component found in the module.
 */
public class NotesWindowSetPublicNoteADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "NOTES_SET_PUBLIC";

  private final String message;

  public NotesWindowSetPublicNoteADT(String message) {
    this.message = message;
  }

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    final GameModule gm = GameModule.getGameModule();
    if (gm == null) return;
    for (final NotesWindow nw : gm.getComponentsOf(NotesWindow.class)) {
      nw.new SetPublicNote(message).execute();
      return;
    }
  }

  @Override
  protected CommandADT createUndoCommand() { return new NullCommandADT(); }

  @Override
  public String getDetails() { return "message=" + message; } //NON-NLS

  public String getMessage() { return message; }
}
