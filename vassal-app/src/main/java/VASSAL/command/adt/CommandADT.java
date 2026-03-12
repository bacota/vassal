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
 * Abstract Data Type interface for commands in the VASSAL engine.
 *
 * <p>Represents a command that can be executed, undone, encoded, and decoded.
 * Commands may be composed into compound commands via {@link #append}.
 * This interface is a parallel, modern alternative to {@link VASSAL.command.Command}
 * and does not extend that class.
 */
public interface CommandADT {

  /**
   * Returns a short string identifier for this command type.
   * Used by {@link CommandInterpreter} to look up the appropriate
   * {@link CommandCodec} during encoding and decoding.
   *
   * @return the command type identifier; never {@code null}
   */
  String getCommandType();

  /**
   * Executes this command and all appended sub-commands.
   */
  void execute();

  /**
   * Returns a command that, when executed, undoes the effect of this command
   * and all its sub-commands.
   *
   * @return the undo command; never {@code null}
   */
  CommandADT getUndoCommand();

  /**
   * Returns {@code true} if this command has no observable effect (i.e. it
   * is a no-op and has no non-null sub-commands).
   *
   * @return {@code true} if this command is a no-op
   */
  boolean isNull();

  /**
   * Returns {@code true} if this command should be stored in a log file.
   * Defaults to {@code !isNull()}.
   *
   * @return {@code true} if this command should be logged
   */
  boolean isLoggable();

  /**
   * Appends {@code c} as a sub-command of this command.
   * If this command {@link #isNull()}, the returned value may be {@code c}
   * itself rather than {@code this}.
   *
   * @param c the sub-command to append; {@code null} or null-commands are ignored
   * @return the resulting (possibly compound) command
   */
  CommandADT append(CommandADT c);

  /**
   * Returns the sub-commands that have been appended to this command.
   *
   * @return an array of sub-commands; never {@code null}
   */
  CommandADT[] getSubCommands();

  /**
   * Returns a human-readable detail string for debugging purposes, or
   * {@code null} if none is available.
   *
   * @return detail string, or {@code null}
   */
  String getDetails();
}
