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
 * ADT representation of {@link VASSAL.build.module.noteswindow.SetPrivateTextCommand}.
 *
 * <p>Stores the owner and text of a {@link VASSAL.build.module.noteswindow.PrivateText}.
 * When executed, delegates to the {@link VASSAL.build.module.noteswindow.PrivateNotesController}
 * via the {@link NotesWindow} command encoder.
 */
public class SetPrivateTextCommandADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "SET_PRIVATE_TEXT";

  private final String owner;
  private final String text;

  public SetPrivateTextCommandADT(String owner, String text) {
    this.owner = owner;
    this.text = text;
  }

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    final GameModule gm = GameModule.getGameModule();
    if (gm == null) return;
    // Reconstruct the wire format and let NotesWindow decode+execute
    final VASSAL.tools.SequenceEncoder se = new VASSAL.tools.SequenceEncoder('\t');
    se.append(owner).append(VASSAL.configure.TextConfigurer.escapeNewlines(text));
    final String encoded = VASSAL.build.module.noteswindow.PrivateNotesController.COMMAND_PREFIX
        + se.getValue();
    for (final NotesWindow nw : gm.getComponentsOf(NotesWindow.class)) {
      final VASSAL.command.Command cmd = nw.decode(encoded);
      if (cmd != null) cmd.execute();
      return;
    }
  }

  @Override
  protected CommandADT createUndoCommand() { return new NullCommandADT(); }

  @Override
  public String getDetails() { return "owner=" + owner; } //NON-NLS

  public String getOwner() { return owner; }
  public String getText()  { return text; }
}
