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
 * ADT representation of {@link BasicLogger.LogCommand}.
 *
 * <p>Wraps another {@link CommandADT} for logging/replay purposes.
 * When executed, the wrapped command is added to the {@link BasicLogger}'s
 * log input list so it can be replayed step-by-step.
 *
 * <p>Because {@link BasicLogger.LogCommand} requires live {@code List} and
 * {@code Action} references, this ADT stores the inner command as a
 * {@link CommandADT} and executes it directly via the {@link BasicLogger}
 * if available, falling back to direct execution.
 */
public class BasicLoggerLogCommandADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "LOGGER_LOG";

  private final CommandADT logged;

  public BasicLoggerLogCommandADT(CommandADT logged) {
    this.logged = logged;
  }

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    logged.execute();
  }

  @Override
  protected CommandADT createUndoCommand() { return new NullCommandADT(); }

  @Override
  public String getDetails() { return "logged=" + logged.getCommandType(); } //NON-NLS

  public CommandADT getLoggedCommand() { return logged; }
}
