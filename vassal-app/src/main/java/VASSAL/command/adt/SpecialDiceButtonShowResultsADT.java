/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import java.util.Arrays;

import VASSAL.build.GameModule;
import VASSAL.build.module.SpecialDiceButton;
import VASSAL.tools.UniqueIdManager;

/**
 * ADT representation of {@link SpecialDiceButton.ShowResults}.
 *
 * <p>When executed, delivers dice roll results to the {@link SpecialDiceButton}
 * identified by {@code buttonId} (its {@link UniqueIdManager} identifier).
 */
public class SpecialDiceButtonShowResultsADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "SPECIAL_DICE_SHOW";

  private final String buttonId;
  private final int[] rolls;

  public SpecialDiceButtonShowResultsADT(String buttonId, int[] rolls) {
    this.buttonId = buttonId;
    this.rolls = Arrays.copyOf(rolls, rolls.length);
  }

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    final GameModule gm = GameModule.getGameModule();
    if (gm == null) return;
    for (final SpecialDiceButton btn : gm.getComponentsOf(SpecialDiceButton.class)) {
      if (buttonId.equals(btn.getIdentifier()) || buttonId.equals(btn.getId())) {
        new SpecialDiceButton.ShowResults(btn, Arrays.copyOf(rolls, rolls.length)).execute();
        return;
      }
    }
  }

  @Override
  protected CommandADT createUndoCommand() { return new NullCommandADT(); }

  @Override
  public String getDetails() { return "buttonId=" + buttonId + ",rolls=" + Arrays.toString(rolls); } //NON-NLS

  public String getButtonId() { return buttonId; }
  public int[] getRolls() { return Arrays.copyOf(rolls, rolls.length); }
}
