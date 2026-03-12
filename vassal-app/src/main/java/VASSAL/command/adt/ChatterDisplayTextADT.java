/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.build.GameModule;
import VASSAL.build.module.Chatter;

/**
 * ADT representation of {@link Chatter.DisplayText}.
 *
 * <p>When executed, displays a text message in the game's {@link Chatter}
 * text area.
 */
public class ChatterDisplayTextADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "CHATTER_DISPLAY";

  private final String message;

  public ChatterDisplayTextADT(String message) {
    this.message = message;
  }

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    final GameModule gm = GameModule.getGameModule();
    if (gm == null) return;
    final Chatter chatter = gm.getChatter();
    if (chatter != null) {
      new Chatter.DisplayText(chatter, message).execute();
    }
  }

  @Override
  protected CommandADT createUndoCommand() { return new NullCommandADT(); }

  @Override
  public String getDetails() { return "message=" + message; } //NON-NLS

  public String getMessage() { return message; }
}
