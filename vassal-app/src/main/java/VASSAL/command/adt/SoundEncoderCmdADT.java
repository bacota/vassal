/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

/**
 * ADT representation of {@link VASSAL.chat.SoundEncoder.Cmd}.
 *
 * <p>Stores the sound key.  Because the original command also tracks a
 * sender {@link VASSAL.chat.Player} for anti-abuse logic that requires live
 * chat state, {@link #isLoggable()} returns {@code false}.
 * When executed it plays the configured sound for {@code soundKey} if
 * possible; otherwise it is a no-op.
 */
public class SoundEncoderCmdADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "CHAT_SOUND";

  private final String soundKey;

  public SoundEncoderCmdADT(String soundKey) {
    this.soundKey = soundKey;
  }

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    // No-op: the anti-abuse logic and Player lookup require live chat state
  }

  @Override
  public boolean isLoggable() { return false; }

  @Override
  protected CommandADT createUndoCommand() { return new NullCommandADT(); }

  @Override
  public String getDetails() { return "soundKey=" + soundKey; } //NON-NLS

  public String getSoundKey() { return soundKey; }
}
