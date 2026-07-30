/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import java.awt.Point;

import VASSAL.build.GameModule;
import VASSAL.build.module.map.LOS_Thread;

/**
 * ADT representation of {@link LOS_Thread.LOSCommand}.
 *
 * <p>Sets or resets the line-of-sight thread identified by {@code threadId}.
 * The undo command restores the old anchor, arrow and flag values.
 */
public class LOSThreadLOSCommandADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "LOS_THREAD";

  private final String threadId;
  private final int newAnchorX, newAnchorY;
  private final int newArrowX, newArrowY;
  private final boolean newPersisting;
  private final boolean newMirroring;
  private final boolean reset;
  private final int oldAnchorX, oldAnchorY;
  private final int oldArrowX, oldArrowY;
  private final boolean oldPersisting;
  private final boolean oldMirroring;

  public LOSThreadLOSCommandADT(String threadId,
                                  int newAnchorX, int newAnchorY,
                                  int newArrowX, int newArrowY,
                                  boolean newPersisting, boolean newMirroring,
                                  boolean reset,
                                  int oldAnchorX, int oldAnchorY,
                                  int oldArrowX, int oldArrowY,
                                  boolean oldPersisting, boolean oldMirroring) {
    this.threadId = threadId;
    this.newAnchorX = newAnchorX; this.newAnchorY = newAnchorY;
    this.newArrowX = newArrowX;   this.newArrowY = newArrowY;
    this.newPersisting = newPersisting; this.newMirroring = newMirroring;
    this.reset = reset;
    this.oldAnchorX = oldAnchorX; this.oldAnchorY = oldAnchorY;
    this.oldArrowX = oldArrowX;   this.oldArrowY = oldArrowY;
    this.oldPersisting = oldPersisting; this.oldMirroring = oldMirroring;
  }

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    final GameModule gm = GameModule.getGameModule();
    if (gm == null) return;
    for (final LOS_Thread t : gm.getAllDescendantComponentsOf(LOS_Thread.class)) {
      if (threadId.equals(t.getId())) {
        final LOS_Thread.LOSCommand cmd = reset
            ? new LOS_Thread.LOSCommand(t)
            : new LOS_Thread.LOSCommand(t,
                new Point(newAnchorX, newAnchorY),
                new Point(newArrowX, newArrowY),
                newPersisting, newMirroring);
        cmd.execute();
        return;
      }
    }
  }

  @Override
  protected CommandADT createUndoCommand() {
    return new LOSThreadLOSCommandADT(threadId,
        oldAnchorX, oldAnchorY, oldArrowX, oldArrowY, oldPersisting, oldMirroring, false,
        newAnchorX, newAnchorY, newArrowX, newArrowY, newPersisting, newMirroring);
  }

  @Override
  public String getDetails() {
    return "threadId=" + threadId + ",reset=" + reset; //NON-NLS
  }

  public String getThreadId() { return threadId; }
  public int getNewAnchorX() { return newAnchorX; }
  public int getNewAnchorY() { return newAnchorY; }
  public int getNewArrowX() { return newArrowX; }
  public int getNewArrowY() { return newArrowY; }
  public boolean isNewPersisting() { return newPersisting; }
  public boolean isNewMirroring() { return newMirroring; }
  public boolean isReset() { return reset; }
  public int getOldAnchorX() { return oldAnchorX; }
  public int getOldAnchorY() { return oldAnchorY; }
  public int getOldArrowX() { return oldArrowX; }
  public int getOldArrowY() { return oldArrowY; }
  public boolean isOldPersisting() { return oldPersisting; }
  public boolean isOldMirroring() { return oldMirroring; }
}
