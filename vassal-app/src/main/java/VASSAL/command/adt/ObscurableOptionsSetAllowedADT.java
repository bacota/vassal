/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import VASSAL.build.module.ObscurableOptions;

/**
 * ADT representation of {@link ObscurableOptions.SetAllowed}.
 *
 * <p>When executed, updates the set of player IDs that are allowed to
 * unmask pieces belonging to other players.
 */
public class ObscurableOptionsSetAllowedADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "OBSCURABLE_SET_ALLOWED";

  private final List<String> allowedIds;

  public ObscurableOptionsSetAllowedADT(List<String> allowedIds) {
    this.allowedIds = new ArrayList<>(allowedIds);
  }

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    new ObscurableOptions.SetAllowed(new ArrayList<>(allowedIds)).execute();
  }

  @Override
  protected CommandADT createUndoCommand() { return new NullCommandADT(); }

  @Override
  public String getDetails() { return "allowed=" + allowedIds; } //NON-NLS

  public List<String> getAllowedIds() {
    return Collections.unmodifiableList(allowedIds);
  }
}
