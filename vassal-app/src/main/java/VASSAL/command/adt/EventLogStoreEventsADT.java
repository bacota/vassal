/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.build.GameModule;
import VASSAL.build.module.EventLog;

/**
 * ADT representation of {@link EventLog.StoreEvents}.
 *
 * <p>When executed, replaces the events in the first {@link EventLog}
 * component with the provided encoded events string.
 */
public class EventLogStoreEventsADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "EVENTLOG_STORE";

  private final String events;

  public EventLogStoreEventsADT(String events) {
    this.events = events;
  }

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    final GameModule gm = GameModule.getGameModule();
    if (gm == null) return;
    for (final EventLog log : gm.getComponentsOf(EventLog.class)) {
      new EventLog.StoreEvents(log, events).execute();
      return;
    }
  }

  @Override
  protected CommandADT createUndoCommand() { return new NullCommandADT(); }

  @Override
  public String getDetails() { return "events(len)=" + events.length(); } //NON-NLS

  public String getEvents() { return events; }
}
