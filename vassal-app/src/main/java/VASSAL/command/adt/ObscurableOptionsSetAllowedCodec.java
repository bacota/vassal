/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@link CommandCodec} for {@link ObscurableOptionsSetAllowedADT}.
 *
 * <p>Wire format: allowed IDs joined by {@code '\u001F'} (ASCII Unit Separator).
 * An empty list is encoded as an empty string.
 */
public class ObscurableOptionsSetAllowedCodec implements CommandCodec {

  private static final char SEP = '\u001F'; // ASCII Unit Separator

  @Override
  public String getCommandType() { return ObscurableOptionsSetAllowedADT.COMMAND_TYPE; }

  @Override
  public String encode(CommandADT command) {
    final List<String> ids = ((ObscurableOptionsSetAllowedADT) command).getAllowedIds();
    if (ids.isEmpty()) return "";
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < ids.size(); i++) {
      if (i > 0) sb.append(SEP);
      sb.append(ids.get(i));
    }
    return sb.toString();
  }

  @Override
  public CommandADT decode(String encoded) {
    final List<String> ids = new ArrayList<>();
    if (!encoded.isEmpty()) {
      for (final String id : encoded.split(String.valueOf(SEP), -1)) {
        ids.add(id);
      }
    }
    return new ObscurableOptionsSetAllowedADT(ids);
  }
}
