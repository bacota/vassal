/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

/** {@link CommandCodec} for {@link EventLogStoreEventsADT}. Wire: the events string. */
public class EventLogStoreEventsCodec implements CommandCodec {

  @Override
  public String getCommandType() { return EventLogStoreEventsADT.COMMAND_TYPE; }

  @Override
  public String encode(CommandADT command) {
    return ((EventLogStoreEventsADT) command).getEvents();
  }

  @Override
  public CommandADT decode(String encoded) { return new EventLogStoreEventsADT(encoded); }
}
