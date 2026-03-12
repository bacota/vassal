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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CompoundCommandADT}.
 */
class CompoundCommandADTTest {

  // -----------------------------------------------------------------------
  // Helper
  // -----------------------------------------------------------------------

  private static AbstractCommandADT recordingCommand(String name, List<String> log) {
    return new AbstractCommandADT() {
      @Override
      public String getCommandType() { return "FAKE"; }

      @Override
      protected void executeInternal() { log.add(name); }

      @Override
      protected CommandADT createUndoCommand() {
        return recordingCommand("undo-" + name, log);
      }
    };
  }

  // -----------------------------------------------------------------------
  // Tests
  // -----------------------------------------------------------------------

  @Test
  void commandType_isCOMPOUND() {
    assertEquals(CompoundCommandADT.COMMAND_TYPE, new CompoundCommandADT().getCommandType());
    assertEquals("COMPOUND", new CompoundCommandADT().getCommandType());
  }

  @Test
  void isNull_alwaysTrue() {
    assertTrue(new CompoundCommandADT().isNull());
  }

  @Test
  void isLoggable_falseByDefault() {
    assertFalse(new CompoundCommandADT().isLoggable());
  }

  @Test
  void execute_doesNothingWithNoSubCommands() {
    assertDoesNotThrow(() -> new CompoundCommandADT().execute());
  }

  @Test
  void execute_runsAllSubCommandsInOrder() {
    final List<String> log = new ArrayList<>();
    final CompoundCommandADT compound = new CompoundCommandADT();

    compound.append(recordingCommand("A", log));
    compound.append(recordingCommand("B", log));
    compound.append(recordingCommand("C", log));

    compound.execute();

    assertEquals(List.of("A", "B", "C"), log);
  }

  @Test
  void append_returnsSubCommandWhenCompoundIsNull() {
    final List<String> log = new ArrayList<>();
    final CompoundCommandADT compound = new CompoundCommandADT();
    final AbstractCommandADT sub = recordingCommand("sub", log);

    final CommandADT result = compound.append(sub);

    // Because compound.isNull() == true, result should be sub
    assertSame(sub, result);
  }

  @Test
  void getSubCommands_returnsAllAppended() {
    final List<String> log = new ArrayList<>();
    final CompoundCommandADT compound = new CompoundCommandADT();

    final AbstractCommandADT a = recordingCommand("A", log);
    final AbstractCommandADT b = recordingCommand("B", log);

    compound.append(a);
    compound.append(b);

    final CommandADT[] subs = compound.getSubCommands();
    assertEquals(2, subs.length);
    assertSame(a, subs[0]);
    assertSame(b, subs[1]);
  }

  @Test
  void getUndoCommand_undoesSubCommandsInReverse() {
    final List<String> undoLog = new ArrayList<>();
    final CompoundCommandADT compound = new CompoundCommandADT();

    final AbstractCommandADT cmdA = new AbstractCommandADT() {
      @Override public String getCommandType() { return "FAKE"; }
      @Override protected void executeInternal() {}
      @Override
      protected CommandADT createUndoCommand() {
        return recordingCommand("undo-A", undoLog);
      }
    };
    final AbstractCommandADT cmdB = new AbstractCommandADT() {
      @Override public String getCommandType() { return "FAKE"; }
      @Override protected void executeInternal() {}
      @Override
      protected CommandADT createUndoCommand() {
        return recordingCommand("undo-B", undoLog);
      }
    };

    compound.append(cmdA);
    compound.append(cmdB);

    compound.getUndoCommand().execute();

    assertEquals(List.of("undo-B", "undo-A"), undoLog);
  }

  @Test
  void toString_includesSubCommands() {
    final List<String> log = new ArrayList<>();
    final CompoundCommandADT compound = new CompoundCommandADT();
    compound.append(recordingCommand("sub1", log));

    final String s = compound.toString();
    assertTrue(s.contains("CompoundCommandADT"));
    assertTrue(s.contains("+"));
  }
}
