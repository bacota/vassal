/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

/**
 * ADT representation of {@link VASSAL.chat.SynchCommand}.
 *
 * <p>Stores the recipient player's name.  Because execution requires a
 * live {@link VASSAL.chat.ChatServerConnection} that cannot be resolved from
 * the GameModule, {@link #isLoggable()} returns {@code false} and
 * {@link #executeInternal()} is a no-op.
 */
public class SynchCommandADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "CHAT_SYNCH";

  private final String recipientName;

  public SynchCommandADT(String recipientName) {
    this.recipientName = recipientName;
  }

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    // No-op: execution requires a live ChatServerConnection
  }

  @Override
  public boolean isLoggable() { return false; }

  @Override
  protected CommandADT createUndoCommand() { return new NullCommandADT(); }

  @Override
  public String getDetails() { return "recipient=" + recipientName; } //NON-NLS

  public String getRecipientName() { return recipientName; }
}
