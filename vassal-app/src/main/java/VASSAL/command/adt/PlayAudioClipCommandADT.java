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

import VASSAL.command.PlayAudioClipCommand;

/**
 * ADT representation of {@link VASSAL.command.PlayAudioClipCommand}.
 *
 * <p>When executed, plays the named audio clip.  This command cannot be
 * undone.
 */
public class PlayAudioClipCommandADT extends AbstractCommandADT {

  /** Command type identifier used by {@link CommandInterpreter}. */
  public static final String COMMAND_TYPE = "PLAY_AUDIO";

  private final String clipName;

  /**
   * @param clipName the name of the audio clip to play
   */
  public PlayAudioClipCommandADT(String clipName) {
    this.clipName = clipName;
  }

  @Override
  public String getCommandType() {
    return COMMAND_TYPE;
  }

  /**
   * Delegates execution to {@link PlayAudioClipCommand}.
   */
  @Override
  protected void executeInternal() {
    new PlayAudioClipCommand(clipName).execute();
  }

  /**
   * Returns {@link NullCommandADT} — audio playback cannot be undone.
   */
  @Override
  protected CommandADT createUndoCommand() {
    return new NullCommandADT();
  }

  @Override
  public String getDetails() {
    return "clipName=" + clipName; //NON-NLS
  }

  public String getClipName() {
    return clipName;
  }
}
