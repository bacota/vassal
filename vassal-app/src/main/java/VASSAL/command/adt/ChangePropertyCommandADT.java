/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.build.GameModule;
import VASSAL.build.module.properties.ChangePropertyCommand;

/**
 * ADT representation of {@link ChangePropertyCommand}.
 *
 * <p>Stores the {@code containerId}, {@code propertyName}, {@code oldValue}
 * and {@code newValue}.  At execute time the property is located by searching
 * all registered {@link MutablePropertiesContainer} instances for one whose
 * {@code getMutablePropertiesContainerId()} matches {@code containerId} and
 * that contains a property named {@code propertyName}.
 *
 * <p>The undo command swaps old and new values.
 */
public class ChangePropertyCommandADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "CHANGE_PROP";

  private final String containerId;
  private final String propertyName;
  private final String oldValue;
  private final String newValue;

  public ChangePropertyCommandADT(String containerId, String propertyName,
                                   String oldValue, String newValue) {
    this.containerId = containerId;
    this.propertyName = propertyName;
    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    final GameModule gm = GameModule.getGameModule();
    if (gm == null) return;
    // Build the wire-format string and let GameModule decode it through all
    // registered ChangePropertyCommandEncoders (one per MutablePropertiesContainer).
    // Format: "MutableProperty\t<key>\t<old>\t<new>\t<containerId>"
    final VASSAL.tools.SequenceEncoder se = new VASSAL.tools.SequenceEncoder('\t');
    se.append(propertyName).append(oldValue).append(newValue).append(containerId);
    final String encoded = "MutableProperty\t" + se.getValue(); //NON-NLS
    final VASSAL.command.Command cmd = gm.decode(encoded);
    if (cmd != null) cmd.execute();
  }

  @Override
  protected CommandADT createUndoCommand() {
    return new ChangePropertyCommandADT(containerId, propertyName, newValue, oldValue);
  }

  @Override
  public String getDetails() {
    return "container=" + containerId + ",property=" + propertyName + ",new=" + newValue; //NON-NLS
  }

  public String getContainerId() { return containerId; }
  public String getPropertyName() { return propertyName; }
  public String getOldValue() { return oldValue; }
  public String getNewValue() { return newValue; }
}
