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
 * Tests for {@link NullCommandADT}.
 */
class NullCommandADTTest {

  @Test
  void commandType_isNULL() {
    assertEquals(NullCommandADT.COMMAND_TYPE, new NullCommandADT().getCommandType());
    assertEquals("NULL", new NullCommandADT().getCommandType());
  }

  @Test
  void isNull_trueWhenAtomic() {
    final NullCommandADT cmd = new NullCommandADT();
    assertTrue(cmd.isNull());
  }

  @Test
  void isLoggable_falseWhenNull() {
    final NullCommandADT cmd = new NullCommandADT();
    assertFalse(cmd.isLoggable());
  }

  @Test
  void execute_doesNothing() {
    // Should not throw
    assertDoesNotThrow(() -> new NullCommandADT().execute());
  }

  @Test
  void isNull_falseWhenHasNonNullSubCommand() {
    // Create a concrete non-null sub-command
    final NullCommandADT cmd = new NullCommandADT();

    // A concrete non-null command to append
    final AbstractCommandADT nonNull = new AbstractCommandADT() {
      @Override
      public String getCommandType() { return "FAKE"; }

      @Override
      protected void executeInternal() {}

      @Override
      protected CommandADT createUndoCommand() { return new NullCommandADT(); }
    };

    // append returns nonNull because cmd.isNull() == true
    final CommandADT result = cmd.append(nonNull);
    assertSame(nonNull, result);

    // The NullCommandADT now has nonNull as a sub-command
    // but isNull() is determined by isAtomic() on cmd itself
    // Since the sub-command was added to cmd AND cmd was null, result is nonNull
    // The sub-command *was* added to cmd's list, so cmd.isNull() should be false
    assertFalse(cmd.isNull());
  }

  @Test
  void getUndoCommand_returnsNonNull() {
    final NullCommandADT cmd = new NullCommandADT();
    assertNotNull(cmd.getUndoCommand());
  }

  @Test
  void getUndoCommand_isNullCommand() {
    final NullCommandADT cmd = new NullCommandADT();
    assertTrue(cmd.getUndoCommand().isNull());
  }

  @Test
  void append_nullIsIgnored() {
    final NullCommandADT cmd = new NullCommandADT();
    final CommandADT result = cmd.append(null);
    assertSame(cmd, result);
  }

  @Test
  void execute_withSubCommandsExecutesThem() {
    final List<String> log = new ArrayList<>();
    final NullCommandADT cmd = new NullCommandADT();

    final AbstractCommandADT sub = new AbstractCommandADT() {
      @Override
      public String getCommandType() { return "FAKE"; }

      @Override
      protected void executeInternal() { log.add("sub-executed"); }

      @Override
      protected CommandADT createUndoCommand() { return new NullCommandADT(); }
    };

    // After append, result is sub (because cmd.isNull() was true before sub was added)
    // But sub was still added to cmd's list; cmd is now non-null
    cmd.append(sub);
    cmd.execute(); // executes NullCommandADT's no-op, then the sub-command
    assertEquals(List.of("sub-executed"), log);
  }
}
