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

import java.awt.Point;

import VASSAL.build.GameModule;
import VASSAL.build.module.map.Flare;

/**
 * ADT representation of {@link VASSAL.command.FlareCommand}.
 *
 * <p>Triggers a {@link Flare} animation at a given map coordinate.  This
 * command cannot be undone.
 */
public class FlareCommandADT extends AbstractCommandADT {

  /** Command type identifier used by {@link CommandInterpreter}. */
  public static final String COMMAND_TYPE = "FLARE";

  private final String flareId;
  private final int clickX;
  private final int clickY;

  /**
   * @param flareId the {@link Flare#getId()} of the target flare component
   * @param clickX  the x-coordinate of the click point
   * @param clickY  the y-coordinate of the click point
   */
  public FlareCommandADT(String flareId, int clickX, int clickY) {
    this.flareId = flareId;
    this.clickX = clickX;
    this.clickY = clickY;
  }

  @Override
  public String getCommandType() {
    return COMMAND_TYPE;
  }

  /**
   * Looks up the {@link Flare} component by ID and starts the animation.
   */
  @Override
  protected void executeInternal() {
    final GameModule gm = GameModule.getGameModule();
    if (gm == null) return;
    for (final Flare flare : gm.getAllDescendantComponentsOf(Flare.class)) {
      if (flareId.equals(flare.getId())) {
        flare.setClickPoint(new Point(clickX, clickY));
        flare.startAnimation(false);
        return;
      }
    }
  }

  /**
   * Returns {@link NullCommandADT} — flares cannot be undone.
   */
  @Override
  protected CommandADT createUndoCommand() {
    return new NullCommandADT();
  }

  @Override
  public String getDetails() {
    return "flareId=" + flareId + ",x=" + clickX + ",y=" + clickY; //NON-NLS
  }

  public String getFlareId() { return flareId; }
  public int getClickX() { return clickX; }
  public int getClickY() { return clickY; }
}
