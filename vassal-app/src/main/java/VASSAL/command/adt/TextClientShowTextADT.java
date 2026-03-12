/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

/**
 * ADT representation of {@link VASSAL.chat.peer2peer.TextClient.ShowText}.
 *
 * <p>When executed, prints the message to {@link System#out} (mirroring the
 * original {@code executeCommand()} behaviour).
 */
public class TextClientShowTextADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "TEXT_CLIENT_SHOW";

  private final String message;

  public TextClientShowTextADT(String message) {
    this.message = message;
  }

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    System.out.println(message);
  }

  @Override
  protected CommandADT createUndoCommand() { return new NullCommandADT(); }

  @Override
  public String getDetails() { return "message=" + message; } //NON-NLS

  public String getMessage() { return message; }
}
