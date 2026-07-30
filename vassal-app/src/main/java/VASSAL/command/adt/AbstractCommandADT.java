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

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Base abstract implementation of {@link CommandADT} that provides default
 * compound-command support.
 *
 * <p>Concrete subclasses must implement:
 * <ul>
 *   <li>{@link #getCommandType()} — returns a stable type identifier string</li>
 *   <li>{@link #executeInternal()} — performs the command's specific action</li>
 *   <li>{@link #createUndoCommand()} — returns a command that undoes only this
 *       command's action (not sub-commands)</li>
 * </ul>
 *
 * <p>This class mirrors the logic of {@link VASSAL.command.Command} but is
 * independent of that hierarchy.
 */
public abstract class AbstractCommandADT implements CommandADT {

  private final List<CommandADT> subCommands = new LinkedList<>();
  private CommandADT cachedUndo;

  // -----------------------------------------------------------------------
  // CommandADT — execution
  // -----------------------------------------------------------------------

  /**
   * Executes this command by first calling {@link #executeInternal()}, then
   * recursively executing all appended sub-commands.
   */
  @Override
  public void execute() {
    executeInternal();
    for (final CommandADT cmd : subCommands) {
      cmd.execute();
    }
  }

  /**
   * Performs the specific action of this command.
   * Sub-commands are handled separately by {@link #execute()}.
   */
  protected abstract void executeInternal();

  // -----------------------------------------------------------------------
  // CommandADT — undo
  // -----------------------------------------------------------------------

  /**
   * Returns a command that undoes this command and all its sub-commands.
   * Sub-command undos are applied in reverse order, followed by this
   * command's own undo (as returned by {@link #createUndoCommand()}).
   */
  @Override
  public CommandADT getUndoCommand() {
    if (cachedUndo == null) {
      CommandADT undo = new NullCommandADT();
      for (final ListIterator<CommandADT> i =
               subCommands.listIterator(subCommands.size()); i.hasPrevious();) {
        undo = undo.append(i.previous().getUndoCommand());
      }
      undo = undo.append(createUndoCommand());
      cachedUndo = undo;
    }
    return cachedUndo;
  }

  /**
   * Returns a command that undoes only this command's own action (not
   * sub-commands). Return {@code null} or a {@link NullCommandADT} if
   * this command cannot be undone.
   *
   * @return the undo command for this command's action
   */
  protected abstract CommandADT createUndoCommand();

  // -----------------------------------------------------------------------
  // CommandADT — null / loggable
  // -----------------------------------------------------------------------

  /**
   * Returns {@code false} by default. Override in subclasses that are no-ops.
   */
  @Override
  public boolean isNull() {
    return false;
  }

  /**
   * Returns {@code !isNull()} by default.
   */
  @Override
  public boolean isLoggable() {
    return !isNull();
  }

  // -----------------------------------------------------------------------
  // CommandADT — sub-commands
  // -----------------------------------------------------------------------

  /**
   * Returns {@code true} if this command has no non-null sub-commands.
   *
   * @return {@code true} if atomic
   */
  protected boolean isAtomic() {
    for (final CommandADT c : subCommands) {
      if (!c.isNull()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public CommandADT append(CommandADT c) {
    CommandADT result = this;
    if (c != null && !c.isNull()) {
      if (isNull()) {
        result = c;
      }
      subCommands.add(c);
      cachedUndo = null; // invalidate cached undo
    }
    return result;
  }

  @Override
  public CommandADT[] getSubCommands() {
    return subCommands.toArray(new CommandADT[0]);
  }

  // -----------------------------------------------------------------------
  // CommandADT — details / toString
  // -----------------------------------------------------------------------

  @Override
  public String getDetails() {
    return null;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
    final String details = getDetails();
    if (details != null) {
      sb.append('[').append(details).append(']');
    }
    for (final CommandADT c : subCommands) {
      sb.append('+').append(c);
    }
    return sb.toString();
  }
}
