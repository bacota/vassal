/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.build.GameModule;
import VASSAL.build.module.properties.ScenarioPropertiesOptionTab;

/**
 * ADT representation of {@link ScenarioPropertiesOptionTab.LockScenarioOptionsTab}.
 *
 * <p>Finds the {@link ScenarioPropertiesOptionTab} with the given
 * {@code tabName} and sets its lock state.  The undo command restores
 * the old lock values.
 */
public class LockScenarioOptionsTabADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "LOCK_SCENARIO_TAB";

  private final String tabName;
  private final String lockedBy;
  private final String lockedPw;
  private final String lockedDt;
  private final String oldLockedBy;
  private final String oldLockedPw;
  private final String oldLockedDt;

  public LockScenarioOptionsTabADT(String tabName,
                                    String lockedBy, String lockedPw, String lockedDt,
                                    String oldLockedBy, String oldLockedPw, String oldLockedDt) {
    this.tabName = tabName;
    this.lockedBy = lockedBy;
    this.lockedPw = lockedPw;
    this.lockedDt = lockedDt;
    this.oldLockedBy = oldLockedBy;
    this.oldLockedPw = oldLockedPw;
    this.oldLockedDt = oldLockedDt;
  }

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    final GameModule gm = GameModule.getGameModule();
    if (gm == null) return;
    for (final ScenarioPropertiesOptionTab tab :
         gm.getAllDescendantComponentsOf(ScenarioPropertiesOptionTab.class)) {
      if (tabName.equals(tab.getConfigureName())) {
        new ScenarioPropertiesOptionTab.LockScenarioOptionsTab(tab, lockedBy, lockedPw, lockedDt)
            .execute();
        return;
      }
    }
  }

  @Override
  protected CommandADT createUndoCommand() {
    return new LockScenarioOptionsTabADT(tabName,
        oldLockedBy, oldLockedPw, oldLockedDt,
        lockedBy, lockedPw, lockedDt);
  }

  @Override
  public String getDetails() { return "tab=" + tabName + ",by=" + lockedBy; } //NON-NLS

  public String getTabName() { return tabName; }
  public String getLockedBy() { return lockedBy; }
  public String getLockedPw() { return lockedPw; }
  public String getLockedDt() { return lockedDt; }
  public String getOldLockedBy() { return oldLockedBy; }
  public String getOldLockedPw() { return oldLockedPw; }
  public String getOldLockedDt() { return oldLockedDt; }
}
