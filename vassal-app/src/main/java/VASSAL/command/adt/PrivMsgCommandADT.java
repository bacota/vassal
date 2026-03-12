/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

/**
 * ADT representation of {@link VASSAL.chat.PrivMsgCommand}.
 *
 * <p>Stores the sender name and message text.  Because execution requires a
 * live {@link VASSAL.chat.PrivateChatManager} that cannot be resolved from the
 * GameModule, {@link #isLoggable()} returns {@code false} and
 * {@link #executeInternal()} is a no-op.
 */
public class PrivMsgCommandADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "CHAT_PRIV_MSG";

  private final String senderName;
  private final String message;

  public PrivMsgCommandADT(String senderName, String message) {
    this.senderName = senderName;
    this.message = message;
  }

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    // No-op: execution requires a live PrivateChatManager
  }

  @Override
  public boolean isLoggable() { return false; }

  @Override
  protected CommandADT createUndoCommand() { return new NullCommandADT(); }

  @Override
  public String getDetails() { return "sender=" + senderName; } //NON-NLS

  public String getSenderName() { return senderName; }
  public String getMessage()    { return message; }
}
