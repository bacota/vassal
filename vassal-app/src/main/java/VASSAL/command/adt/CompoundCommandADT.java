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
 * A composite {@link CommandADT} that acts as a container for sub-commands.
 *
 * <p>A {@code CompoundCommandADT} is itself null (has no intrinsic action) but
 * serves as a convenient grouping mechanism.  Sub-commands appended via
 * {@link #append} are executed in order when {@link #execute()} is called.
 *
 * <p>Example usage:
 * <pre>
 *   CommandADT compound = new CompoundCommandADT()
 *       .append(commandA)
 *       .append(commandB);
 *   compound.execute(); // executes A then B
 * </pre>
 */
public class CompoundCommandADT extends AbstractCommandADT {

  /** Command type identifier used by {@link CommandInterpreter}. */
  public static final String COMMAND_TYPE = "COMPOUND";

  @Override
  public String getCommandType() {
    return COMMAND_TYPE;
  }

  /**
   * Does nothing — the compound command has no intrinsic action.
   */
  @Override
  protected void executeInternal() {
    // no intrinsic action; sub-commands are executed by AbstractCommandADT.execute()
  }

  /**
   * Returns a new {@link NullCommandADT} as the undo command for this
   * container's own action.  Sub-command undos are handled by
   * {@link AbstractCommandADT#getUndoCommand()}.
   */
  @Override
  protected CommandADT createUndoCommand() {
    return new NullCommandADT();
  }

  /**
   * A {@code CompoundCommandADT} is always considered null (no intrinsic
   * action).  It becomes non-null only in terms of the sub-commands it holds,
   * which is handled by the {@link #append} logic in
   * {@link AbstractCommandADT}.
   */
  @Override
  public boolean isNull() {
    return true;
  }
}
