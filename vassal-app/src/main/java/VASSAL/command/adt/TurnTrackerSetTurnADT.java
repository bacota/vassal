/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.build.GameModule;
import VASSAL.build.module.turn.TurnTracker;

/**
 * ADT representation of {@link TurnTracker.SetTurn}.
 *
 * <p>When executed, sets the state of the {@link TurnTracker} identified by
 * {@code trackerId} to {@code newState}, saving the old state for undo.
 */
public class TurnTrackerSetTurnADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "TURN_SET";

  private final String trackerId;
  private final String newState;
  private final String oldState;

  public TurnTrackerSetTurnADT(String trackerId, String newState, String oldState) {
    this.trackerId = trackerId;
    this.newState = newState;
    this.oldState = oldState;
  }

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    final GameModule gm = GameModule.getGameModule();
    if (gm == null) return;
    for (final TurnTracker tt : gm.getComponentsOf(TurnTracker.class)) {
      if (trackerId.equals(tt.getId())) {
        new TurnTracker.SetTurn(newState, tt).execute();
        return;
      }
    }
  }

  @Override
  protected CommandADT createUndoCommand() {
    return new TurnTrackerSetTurnADT(trackerId, oldState, newState);
  }

  @Override
  public String getDetails() { return "trackerId=" + trackerId + ",newState=" + newState; } //NON-NLS

  public String getTrackerId() { return trackerId; }
  public String getNewState() { return newState; }
  public String getOldState() { return oldState; }
}
