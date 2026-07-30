/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import java.util.Date;

import VASSAL.build.GameModule;
import VASSAL.build.module.NotesWindow;

/**
 * ADT representation of {@link VASSAL.build.module.noteswindow.AddSecretNoteCommand}.
 *
 * <p>Stores all fields of the {@link SecretNote} (name, owner, text, hidden,
 * date, handle) as plain strings.  When executed, constructs the
 * {@link SecretNote} and adds it through the first {@link NotesWindow}'s
 * {@link VASSAL.build.module.noteswindow.SecretNotesController}.
 */
public class AddSecretNoteCommandADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "ADD_SECRET_NOTE";

  private final String name;
  private final String owner;
  private final String text;
  private final boolean hidden;
  private final long dateMillis;   // Date stored as epoch millis; -1 = null
  private final String handle;

  public AddSecretNoteCommandADT(String name, String owner, String text,
                                   boolean hidden, long dateMillis, String handle) {
    this.name = name;
    this.owner = owner;
    this.text = text;
    this.hidden = hidden;
    this.dateMillis = dateMillis;
    this.handle = handle;
  }

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    final GameModule gm = GameModule.getGameModule();
    if (gm == null) return;
    // Build the wire-format string for SecretNotesController and
    // let NotesWindow decode it (which wires in the correct Interface reference)
    final Date date = dateMillis >= 0 ? new Date(dateMillis) : null;
    final String dateStr = date == null ? "" //NON-NLS
        : new java.text.SimpleDateFormat("MM/dd/yyyy h:mm a").format(date); //NON-NLS
    final VASSAL.tools.SequenceEncoder se = new VASSAL.tools.SequenceEncoder('\t');
    se.append(name)
      .append(owner)
      .append(hidden)
      .append(VASSAL.configure.TextConfigurer.escapeNewlines(text))
      .append(dateStr)
      .append(handle);
    final String encoded = VASSAL.build.module.noteswindow.SecretNotesController.COMMAND_PREFIX
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
  public String getDetails() { return "name=" + name + ",owner=" + owner; } //NON-NLS

  public String getName()   { return name; }
  public String getOwner()  { return owner; }
  public String getText()   { return text; }
  public boolean isHidden() { return hidden; }
  public long getDateMillis() { return dateMillis; }
  public String getHandle() { return handle; }
}
