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
 * Tests for {@link AbstractCommandADT}.
 */
class AbstractCommandADTTest {

  // -----------------------------------------------------------------------
  // Minimal concrete subclass for testing
  // -----------------------------------------------------------------------

  /** Simple concrete command that records executions and supports undo. */
  private static class TestCommand extends AbstractCommandADT {
    private static final String TYPE = "TEST";

    final List<String> log;
    private final String name;
    private final CommandADT undoCommand;

    TestCommand(String name, List<String> log) {
      this(name, log, null);
    }

    TestCommand(String name, List<String> log, CommandADT undoCommand) {
      this.name = name;
      this.log = log;
      this.undoCommand = undoCommand;
    }

    @Override
    public String getCommandType() {
      return TYPE;
    }

    @Override
    protected void executeInternal() {
      log.add(name);
    }

    @Override
    protected CommandADT createUndoCommand() {
      return undoCommand != null ? undoCommand : new NullCommandADT();
    }

    @Override
    public String getDetails() {
      return name;
    }
  }

  // -----------------------------------------------------------------------
  // Tests — basic execute
  // -----------------------------------------------------------------------

  @Test
  void execute_callsExecuteInternal() {
    final List<String> log = new ArrayList<>();
    final TestCommand cmd = new TestCommand("A", log);

    cmd.execute();

    assertEquals(List.of("A"), log);
  }

  @Test
  void execute_recursesThroughSubCommands() {
    final List<String> log = new ArrayList<>();
    final TestCommand parent = new TestCommand("parent", log);
    final TestCommand child1 = new TestCommand("child1", log);
    final TestCommand child2 = new TestCommand("child2", log);

    parent.append(child1);
    parent.append(child2);
    parent.execute();

    assertEquals(List.of("parent", "child1", "child2"), log);
  }

  // -----------------------------------------------------------------------
  // Tests — sub-command management
  // -----------------------------------------------------------------------

  @Test
  void append_nullIsIgnored() {
    final TestCommand cmd = new TestCommand("A", new ArrayList<>());
    final CommandADT result = cmd.append(null);

    assertSame(cmd, result);
    assertEquals(0, cmd.getSubCommands().length);
  }

  @Test
  void append_nullCommandIsIgnored() {
    final TestCommand cmd = new TestCommand("A", new ArrayList<>());
    final CommandADT result = cmd.append(new NullCommandADT());

    assertSame(cmd, result);
    assertEquals(0, cmd.getSubCommands().length);
  }

  @Test
  void append_nonNullAddsSubCommand() {
    final List<String> log = new ArrayList<>();
    final TestCommand parent = new TestCommand("parent", log);
    final TestCommand child = new TestCommand("child", log);

    parent.append(child);

    assertEquals(1, parent.getSubCommands().length);
    assertSame(child, parent.getSubCommands()[0]);
  }

  @Test
  void append_whenThisIsNull_returnsAppendedCommand() {
    // NullCommandADT.isNull() returns true when atomic
    final NullCommandADT nullCmd = new NullCommandADT();
    final List<String> log = new ArrayList<>();
    final TestCommand child = new TestCommand("child", log);

    final CommandADT result = nullCmd.append(child);

    assertSame(child, result);
  }

  @Test
  void getSubCommands_returnsEmptyArrayWhenNone() {
    final TestCommand cmd = new TestCommand("A", new ArrayList<>());
    assertEquals(0, cmd.getSubCommands().length);
  }

  // -----------------------------------------------------------------------
  // Tests — undo
  // -----------------------------------------------------------------------

  @Test
  void getUndoCommand_returnsCachedUndo() {
    final TestCommand cmd = new TestCommand("A", new ArrayList<>());
    final CommandADT undo1 = cmd.getUndoCommand();
    final CommandADT undo2 = cmd.getUndoCommand();
    assertSame(undo1, undo2);
  }

  @Test
  void getUndoCommand_invalidatesCacheAfterAppend() {
    final List<String> log = new ArrayList<>();
    final TestCommand parent = new TestCommand("parent", log);
    final CommandADT undoBefore = parent.getUndoCommand();

    parent.append(new TestCommand("child", log));
    final CommandADT undoAfter = parent.getUndoCommand();

    assertNotSame(undoBefore, undoAfter);
  }

  @Test
  void getUndoCommand_withSubCommandsUndoesInReverseOrder() {
    final List<String> executeLog = new ArrayList<>();
    final List<String> undoLog = new ArrayList<>();

    final TestCommand undoOfA = new TestCommand("undo-A", undoLog);
    final TestCommand undoOfB = new TestCommand("undo-B", undoLog);

    final TestCommand cmdA = new TestCommand("A", executeLog, undoOfA);
    final TestCommand cmdB = new TestCommand("B", executeLog, undoOfB);

    cmdA.append(cmdB);
    cmdA.execute();

    assertEquals(List.of("A", "B"), executeLog);

    cmdA.getUndoCommand().execute();

    // sub-command B is undone first, then A
    assertEquals(List.of("undo-B", "undo-A"), undoLog);
  }

  // -----------------------------------------------------------------------
  // Tests — isNull / isLoggable
  // -----------------------------------------------------------------------

  @Test
  void isNull_returnsFalseByDefault() {
    assertFalse(new TestCommand("A", new ArrayList<>()).isNull());
  }

  @Test
  void isLoggable_returnsTrueByDefault() {
    assertTrue(new TestCommand("A", new ArrayList<>()).isLoggable());
  }

  // -----------------------------------------------------------------------
  // Tests — toString
  // -----------------------------------------------------------------------

  @Test
  void toString_includesClassSimpleNameAndDetails() {
    final TestCommand cmd = new TestCommand("myCmd", new ArrayList<>());
    final String s = cmd.toString();
    assertTrue(s.contains("TestCommand"));
    assertTrue(s.contains("myCmd"));
  }

  @Test
  void toString_includesSubCommands() {
    final List<String> log = new ArrayList<>();
    final TestCommand parent = new TestCommand("parent", log);
    final TestCommand child = new TestCommand("child", log);
    parent.append(child);

    final String s = parent.toString();
    assertTrue(s.contains("+"));
    assertTrue(s.contains("child"));
  }
}
