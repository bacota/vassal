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
 * A no-op {@link CommandADT} that does nothing when executed.
 *
 * <p>{@link #isNull()} returns {@code true} when this command is atomic
 * (has no non-null sub-commands), mirroring the behaviour of
 * {@link VASSAL.command.NullCommand}.
 */
public class NullCommandADT extends AbstractCommandADT {

  /** Command type identifier used by {@link CommandInterpreter}. */
  public static final String COMMAND_TYPE = "NULL";

  @Override
  public String getCommandType() {
    return COMMAND_TYPE;
  }

  /**
   * Does nothing.
   */
  @Override
  protected void executeInternal() {
    // no-op
  }

  /**
   * Returns a new {@link NullCommandADT} as the undo command.
   */
  @Override
  protected CommandADT createUndoCommand() {
    return new NullCommandADT();
  }

  /**
   * Returns {@code true} when this command has no non-null sub-commands.
   */
  @Override
  public boolean isNull() {
    return isAtomic();
  }
}
