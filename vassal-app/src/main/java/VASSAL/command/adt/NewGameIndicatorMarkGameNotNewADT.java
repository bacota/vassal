/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.build.GameModule;
import VASSAL.build.module.NewGameIndicator;

/**
 * ADT representation of {@link NewGameIndicator.MarkGameNotNew}.
 *
 * <p>When executed, marks the game as no longer new by clearing the
 * {@code isNewGame} flag on the first {@link NewGameIndicator} component.
 */
public class NewGameIndicatorMarkGameNotNewADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "MARK_GAME_NOT_NEW";

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    final GameModule gm = GameModule.getGameModule();
    if (gm == null) return;
    for (final NewGameIndicator ind : gm.getComponentsOf(NewGameIndicator.class)) {
      new NewGameIndicator.MarkGameNotNew(ind).execute();
      return;
    }
  }

  @Override
  protected CommandADT createUndoCommand() { return new NullCommandADT(); }

  @Override
  public String getDetails() { return ""; } //NON-NLS
}
