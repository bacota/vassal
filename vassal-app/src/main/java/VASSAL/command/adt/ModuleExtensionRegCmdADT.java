/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.build.module.ModuleExtension;

/**
 * ADT representation of {@link ModuleExtension.RegCmd}.
 *
 * <p>When executed, verifies that the named extension (at the given version)
 * is loaded in the current game module.
 */
public class ModuleExtensionRegCmdADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "MODULE_EXT_REG";

  private final String name;
  private final String version;

  public ModuleExtensionRegCmdADT(String name, String version) {
    this.name = name;
    this.version = version;
  }

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    new ModuleExtension.RegCmd(name, version).execute();
  }

  @Override
  protected CommandADT createUndoCommand() { return new NullCommandADT(); }

  @Override
  public String getDetails() { return "name=" + name + ",version=" + version; } //NON-NLS

  public String getName() { return name; }
  public String getVersion() { return version; }
}
