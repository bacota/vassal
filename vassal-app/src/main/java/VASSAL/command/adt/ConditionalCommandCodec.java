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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import VASSAL.command.ConditionalCommand;
import VASSAL.tools.SequenceEncoder;

/**
 * {@link CommandCodec} for {@link ConditionalCommandADT}.
 *
 * <p>This codec requires a reference to the owning {@link CommandInterpreter}
 * so that it can recursively encode and decode the delegate {@link CommandADT}.
 *
 * <h3>Wire format</h3>
 * Two {@code '/'}-separated fields:
 * <ol>
 *   <li>conditions — a {@code '\u001E'} (ASCII Record Separator) delimited list
 *       of condition strings, each using the format described below.</li>
 *   <li>delegate — the full encoded form of the delegate command (as produced
 *       by the interpreter).</li>
 * </ol>
 *
 * <h4>Condition format</h4>
 * <ul>
 *   <li>{@code EQ:<property>:<val1>\u001F<val2>...} — equality check</li>
 *   <li>{@code LT:<property>:<value>} — version less-than check</li>
 *   <li>{@code GT:<property>:<value>} — version greater-than check</li>
 *   <li>{@code NOT:<sub-condition>} — negation (only one level deep)</li>
 * </ul>
 */
public class ConditionalCommandCodec implements CommandCodec {

  /** Separator between individual condition strings. */
  private static final char CONDITION_SEP = '\u001E'; // ASCII Record Separator

  /** Separator between allowed values inside an Eq condition. */
  private static final char VALUE_SEP = '\u001F'; // ASCII Unit Separator

  private static final char FIELD_SEP = '/';

  private final CommandInterpreter interpreter;

  /**
   * @param interpreter the interpreter used to encode/decode the delegate command
   */
  public ConditionalCommandCodec(CommandInterpreter interpreter) {
    this.interpreter = interpreter;
  }

  @Override
  public String getCommandType() {
    return ConditionalCommandADT.COMMAND_TYPE;
  }

  @Override
  public String encode(CommandADT command) {
    final ConditionalCommandADT c = (ConditionalCommandADT) command;
    final String conditionsStr = encodeConditions(c.getConditions());
    final String delegateStr = interpreter.encode(c.getDelegate());
    return new SequenceEncoder(FIELD_SEP)
        .append(conditionsStr)
        .append(delegateStr)
        .getValue();
  }

  @Override
  public CommandADT decode(String encoded) {
    final SequenceEncoder.Decoder st = new SequenceEncoder.Decoder(encoded, FIELD_SEP);
    final String conditionsStr = st.nextToken("");
    final String delegateStr = st.nextToken("");
    final ConditionalCommand.Condition[] conditions = decodeConditions(conditionsStr);
    final CommandADT delegate = interpreter.decode(delegateStr);
    return new ConditionalCommandADT(conditions, delegate);
  }

  // -----------------------------------------------------------------------
  // Condition encoding helpers
  // -----------------------------------------------------------------------

  private String encodeConditions(ConditionalCommand.Condition[] conditions) {
    if (conditions.length == 0) {
      return "";
    }
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < conditions.length; i++) {
      if (i > 0) sb.append(CONDITION_SEP);
      sb.append(encodeCondition(conditions[i]));
    }
    return sb.toString();
  }

  private String encodeCondition(ConditionalCommand.Condition condition) {
    if (condition instanceof ConditionalCommand.Eq) {
      final ConditionalCommand.Eq eq = (ConditionalCommand.Eq) condition;
      final StringBuilder sb = new StringBuilder("EQ:");
      sb.append(eq.getProperty()).append(':');
      final List<String> values = eq.getValueList();
      for (int i = 0; i < values.size(); i++) {
        if (i > 0) sb.append(VALUE_SEP);
        sb.append(values.get(i));
      }
      return sb.toString();
    }
    else if (condition instanceof ConditionalCommand.Lt) {
      final ConditionalCommand.Lt lt = (ConditionalCommand.Lt) condition;
      return "LT:" + lt.getProperty() + ":" + lt.getValue();
    }
    else if (condition instanceof ConditionalCommand.Gt) {
      final ConditionalCommand.Gt gt = (ConditionalCommand.Gt) condition;
      return "GT:" + gt.getProperty() + ":" + gt.getValue();
    }
    else if (condition instanceof ConditionalCommand.Not) {
      final ConditionalCommand.Not not = (ConditionalCommand.Not) condition;
      return "NOT:" + encodeCondition(not.getSubCondition());
    }
    else {
      throw new IllegalArgumentException(
          "Unknown Condition type: " + condition.getClass().getName());
    }
  }

  private ConditionalCommand.Condition[] decodeConditions(String encoded) {
    if (encoded == null || encoded.isEmpty()) {
      return new ConditionalCommand.Condition[0];
    }
    final String[] parts = encoded.split(String.valueOf(CONDITION_SEP), -1);
    final List<ConditionalCommand.Condition> conditions = new ArrayList<>(parts.length);
    for (final String part : parts) {
      if (!part.isEmpty()) {
        conditions.add(decodeCondition(part));
      }
    }
    return conditions.toArray(new ConditionalCommand.Condition[0]);
  }

  private ConditionalCommand.Condition decodeCondition(String encoded) {
    if (encoded.startsWith("EQ:")) {
      final String rest = encoded.substring(3);
      final int colonIdx = rest.indexOf(':');
      final String property = colonIdx >= 0 ? rest.substring(0, colonIdx) : rest;
      final String valuesStr = colonIdx >= 0 ? rest.substring(colonIdx + 1) : "";
      final List<String> values = new ArrayList<>();
      if (!valuesStr.isEmpty()) {
        for (final String v : valuesStr.split(String.valueOf(VALUE_SEP), -1)) {
          values.add(v);
        }
      }
      return new ConditionalCommand.Eq(property, values);
    }
    else if (encoded.startsWith("LT:")) {
      final String rest = encoded.substring(3);
      final int colonIdx = rest.indexOf(':');
      final String property = colonIdx >= 0 ? rest.substring(0, colonIdx) : rest;
      final String value = colonIdx >= 0 ? rest.substring(colonIdx + 1) : "";
      return new ConditionalCommand.Lt(property, value);
    }
    else if (encoded.startsWith("GT:")) {
      final String rest = encoded.substring(3);
      final int colonIdx = rest.indexOf(':');
      final String property = colonIdx >= 0 ? rest.substring(0, colonIdx) : rest;
      final String value = colonIdx >= 0 ? rest.substring(colonIdx + 1) : "";
      return new ConditionalCommand.Gt(property, value);
    }
    else if (encoded.startsWith("NOT:")) {
      return new ConditionalCommand.Not(decodeCondition(encoded.substring(4)));
    }
    else {
      throw new IllegalArgumentException("Unknown condition encoding: " + encoded);
    }
  }
}
