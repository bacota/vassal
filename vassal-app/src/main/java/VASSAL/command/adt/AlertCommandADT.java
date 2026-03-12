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

import VASSAL.command.AlertCommand;

/**
 * ADT representation of {@link VASSAL.command.AlertCommand}.
 *
 * <p>When executed, displays a dialog box with a message.  This command
 * cannot be undone.
 */
public class AlertCommandADT extends AbstractCommandADT {

  /** Command type identifier used by {@link CommandInterpreter}. */
  public static final String COMMAND_TYPE = "ALERT";

  private final String message;

  /**
   * @param message the message to display when the command is executed
   */
  public AlertCommandADT(String message) {
    this.message = message;
  }

  @Override
  public String getCommandType() {
    return COMMAND_TYPE;
  }

  /**
   * Delegates execution to {@link AlertCommand}.
   */
  @Override
  protected void executeInternal() {
    new AlertCommand(message).execute();
  }

  /**
   * Returns {@link NullCommandADT} — alerts cannot be undone.
   */
  @Override
  protected CommandADT createUndoCommand() {
    return new NullCommandADT();
  }

  @Override
  public String getDetails() {
    return "message=" + message; //NON-NLS
  }

  public String getMessage() {
    return message;
  }
}
