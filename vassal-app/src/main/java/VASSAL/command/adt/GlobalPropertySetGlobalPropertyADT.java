/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.build.GameModule;
import VASSAL.build.module.properties.GlobalProperty;

/**
 * ADT representation of {@link GlobalProperty.SetGlobalProperty}.
 *
 * <p>Finds the {@link GlobalProperty} identified by {@code propertyName} and
 * {@code containerId} and sets its value to {@code newValue}.
 * The undo command restores {@code oldValue}.
 */
public class GlobalPropertySetGlobalPropertyADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "GLOBAL_PROP_SET";

  private final String propertyName;
  private final String containerId;
  private final String oldValue;
  private final String newValue;

  public GlobalPropertySetGlobalPropertyADT(String propertyName, String containerId,
                                              String oldValue, String newValue) {
    this.propertyName = propertyName;
    this.containerId = containerId;
    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    final GameModule gm = GameModule.getGameModule();
    if (gm == null) return;
    for (final GlobalProperty gp : gm.getAllDescendantComponentsOf(GlobalProperty.class)) {
      if (propertyName.equals(gp.getConfigureName())) {
        final String cid = gp.getContainerId();
        if (containerId == null || containerId.isEmpty() || containerId.equals(cid)) {
          new GlobalProperty.SetGlobalProperty(gp, oldValue, newValue).execute();
          return;
        }
      }
    }
  }

  @Override
  protected CommandADT createUndoCommand() {
    return new GlobalPropertySetGlobalPropertyADT(propertyName, containerId, newValue, oldValue);
  }

  @Override
  public String getDetails() {
    return "property=" + propertyName + ",container=" + containerId + ",new=" + newValue; //NON-NLS
  }

  public String getPropertyName() { return propertyName; }
  public String getContainerId() { return containerId; }
  public String getOldValue() { return oldValue; }
  public String getNewValue() { return newValue; }
}
