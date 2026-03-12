/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.build.GameModule;
import VASSAL.build.module.BasicLogger;

/**
 * ADT representation of {@link BasicLogger.UndoCommand}.
 *
 * <p>When executed, records whether an undo operation is in progress
 * on the {@link BasicLogger}.
 */
public class BasicLoggerUndoCommandADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "LOGGER_UNDO";

  private final boolean inProgress;

  public BasicLoggerUndoCommandADT(boolean inProgress) {
    this.inProgress = inProgress;
  }

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    new BasicLogger.UndoCommand(inProgress).execute();
  }

  @Override
  protected CommandADT createUndoCommand() { return new NullCommandADT(); }

  @Override
  public String getDetails() { return "inProgress=" + inProgress; } //NON-NLS

  public boolean isInProgress() { return inProgress; }
}
