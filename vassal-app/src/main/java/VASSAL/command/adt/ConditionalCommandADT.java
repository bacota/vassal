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

import VASSAL.command.ConditionalCommand;

/**
 * ADT representation of {@link VASSAL.command.ConditionalCommand}.
 *
 * <p>Evaluates a set of {@link ConditionalCommand.Condition} objects and, if
 * all are satisfied, executes the {@code delegate} {@link CommandADT}.
 * This command cannot be undone.
 *
 * <p>The conditions reuse the existing {@link ConditionalCommand.Condition}
 * hierarchy, which checks live {@link VASSAL.build.GameModule} properties.
 */
public class ConditionalCommandADT extends AbstractCommandADT {

  /** Command type identifier used by {@link CommandInterpreter}. */
  public static final String COMMAND_TYPE = "CONDITIONAL";

  private final ConditionalCommand.Condition[] conditions;
  private final CommandADT delegate;

  /**
   * @param conditions the conditions that must all be satisfied for
   *                   {@code delegate} to be executed; never {@code null}
   * @param delegate   the command to execute when all conditions pass;
   *                   never {@code null}
   */
  public ConditionalCommandADT(ConditionalCommand.Condition[] conditions,
                                CommandADT delegate) {
    this.conditions = conditions.clone();
    this.delegate = delegate;
  }

  @Override
  public String getCommandType() {
    return COMMAND_TYPE;
  }

  /**
   * Checks each condition in order and executes {@code delegate} only if all
   * are satisfied.
   */
  @Override
  protected void executeInternal() {
    for (final ConditionalCommand.Condition condition : conditions) {
      if (!condition.isSatisfied()) {
        return;
      }
    }
    delegate.execute();
  }

  /**
   * Returns {@link NullCommandADT} — conditional commands cannot be undone.
   */
  @Override
  protected CommandADT createUndoCommand() {
    return new NullCommandADT();
  }

  @Override
  public String getDetails() {
    return "conditions=" + conditions.length + ",delegate=" + delegate; //NON-NLS
  }

  public ConditionalCommand.Condition[] getConditions() {
    return conditions.clone();
  }

  public CommandADT getDelegate() {
    return delegate;
  }
}
