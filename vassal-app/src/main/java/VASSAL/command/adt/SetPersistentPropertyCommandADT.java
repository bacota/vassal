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

import VASSAL.command.SetPersistentPropertyCommand;

/**
 * ADT representation of {@link VASSAL.command.SetPersistentPropertyCommand}.
 *
 * <p>Sets a persistent property on a game piece.  The undo command restores
 * the old value.
 */
public class SetPersistentPropertyCommandADT extends AbstractCommandADT {

  /** Command type identifier used by {@link CommandInterpreter}. */
  public static final String COMMAND_TYPE = "SET_PERSISTENT_PROP";

  private final String id;
  private final String key;
  private final String oldValue;
  private final String newValue;

  /**
   * @param id       the id of the target piece
   * @param key      the property key
   * @param oldValue the previous value (used for undo)
   * @param newValue the new value to set
   */
  public SetPersistentPropertyCommandADT(String id, String key,
                                          String oldValue, String newValue) {
    this.id = id;
    this.key = key;
    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  @Override
  public String getCommandType() {
    return COMMAND_TYPE;
  }

  /**
   * Delegates execution to {@link SetPersistentPropertyCommand}.
   */
  @Override
  protected void executeInternal() {
    new SetPersistentPropertyCommand(id, key, oldValue, newValue).execute();
  }

  /**
   * Returns a command that restores the old property value.
   */
  @Override
  protected CommandADT createUndoCommand() {
    return new SetPersistentPropertyCommandADT(id, key, newValue, oldValue);
  }

  @Override
  public String getDetails() {
    return "id=" + id + ",key=" + key + ",old=" + oldValue + ",new=" + newValue; //NON-NLS
  }

  public String getId() { return id; }
  public String getKey() { return key; }
  public String getOldValue() { return oldValue; }
  public String getNewValue() { return newValue; }
}
