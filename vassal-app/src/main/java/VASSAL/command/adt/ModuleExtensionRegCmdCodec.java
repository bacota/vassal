/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.tools.SequenceEncoder;

/**
 * {@link CommandCodec} for {@link ModuleExtensionRegCmdADT}.
 * Wire format: {@code name/version} (SequenceEncoder with '/').
 */
public class ModuleExtensionRegCmdCodec implements CommandCodec {

  private static final char SEP = '/';

  @Override
  public String getCommandType() { return ModuleExtensionRegCmdADT.COMMAND_TYPE; }

  @Override
  public String encode(CommandADT command) {
    final ModuleExtensionRegCmdADT c = (ModuleExtensionRegCmdADT) command;
    return new SequenceEncoder(SEP).append(c.getName()).append(c.getVersion()).getValue();
  }

  @Override
  public CommandADT decode(String encoded) {
    final SequenceEncoder.Decoder st = new SequenceEncoder.Decoder(encoded, SEP);
    return new ModuleExtensionRegCmdADT(st.nextToken(""), st.nextToken(""));
  }
}
