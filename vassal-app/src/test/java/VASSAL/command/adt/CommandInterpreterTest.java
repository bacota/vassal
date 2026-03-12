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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CommandInterpreter}.
 */
class CommandInterpreterTest {

  // -----------------------------------------------------------------------
  // Helpers — simple "PING" command for round-trip tests
  // -----------------------------------------------------------------------

  private static class PingCommand extends AbstractCommandADT {
    static final String TYPE = "PING";
    final String payload;

    PingCommand(String payload) {
      this.payload = payload;
    }

    @Override
    public String getCommandType() { return TYPE; }

    @Override
    protected void executeInternal() {}

    @Override
    protected CommandADT createUndoCommand() { return new NullCommandADT(); }

    @Override
    public String getDetails() { return payload; }
  }

  private static class PingCodec implements CommandCodec {
    @Override
    public String getCommandType() { return PingCommand.TYPE; }

    @Override
    public String encode(CommandADT command) {
      return ((PingCommand) command).payload;
    }

    @Override
    public CommandADT decode(String encoded) {
      return new PingCommand(encoded);
    }
  }

  // -----------------------------------------------------------------------
  // Setup
  // -----------------------------------------------------------------------

  private CommandInterpreter interpreter;

  @BeforeEach
  void setUp() {
    interpreter = new CommandInterpreter();
    interpreter.registerCodec(PingCommand.TYPE, new PingCodec());
  }

  // -----------------------------------------------------------------------
  // Tests — registerCodec / unregisterCodec
  // -----------------------------------------------------------------------

  @Test
  void registerCodec_throwsOnNullType() {
    assertThrows(IllegalArgumentException.class,
        () -> interpreter.registerCodec(null, new PingCodec()));
  }

  @Test
  void registerCodec_throwsOnNullCodec() {
    assertThrows(IllegalArgumentException.class,
        () -> interpreter.registerCodec("X", null));
  }

  @Test
  void unregisterCodec_throwsOnNullType() {
    assertThrows(IllegalArgumentException.class,
        () -> interpreter.unregisterCodec(null));
  }

  @Test
  void unregisterCodec_removesCodec() {
    interpreter.unregisterCodec(PingCommand.TYPE);
    assertThrows(IllegalArgumentException.class,
        () -> interpreter.encode(new PingCommand("hello")));
  }

  @Test
  void unregisterCodec_doesNothingForUnknownType() {
    assertDoesNotThrow(() -> interpreter.unregisterCodec("UNKNOWN"));
  }

  // -----------------------------------------------------------------------
  // Tests — encode / decode (single command)
  // -----------------------------------------------------------------------

  @Test
  void encode_nullCommand_throwsIAE() {
    assertThrows(IllegalArgumentException.class, () -> interpreter.encode(null));
  }

  @Test
  void decode_nullString_throwsIAE() {
    assertThrows(IllegalArgumentException.class, () -> interpreter.decode(null));
  }

  @Test
  void encode_producesTypePrefixedString() {
    final String encoded = interpreter.encode(new PingCommand("hello"));
    assertTrue(encoded.startsWith("PING:"), "Expected PING: prefix, got: " + encoded);
    assertTrue(encoded.endsWith("hello"));
  }

  @Test
  void decode_restoresCommand() {
    final String encoded = interpreter.encode(new PingCommand("world"));
    final CommandADT decoded = interpreter.decode(encoded);
    assertInstanceOf(PingCommand.class, decoded);
    assertEquals("world", ((PingCommand) decoded).payload);
  }

  @Test
  void encodeDecodeRoundTrip_nullCommand() {
    final NullCommandADT nullCmd = new NullCommandADT();
    final String encoded = interpreter.encode(nullCmd);
    final CommandADT decoded = interpreter.decode(encoded);
    assertInstanceOf(NullCommandADT.class, decoded);
    assertTrue(decoded.isNull());
  }

  @Test
  void encode_throwsForUnknownCommandType() {
    // A command with a type that has no registered codec
    final CommandADT unknown = new AbstractCommandADT() {
      @Override
      public String getCommandType() { return "UNKNOWN_TYPE"; }

      @Override
      protected void executeInternal() {}

      @Override
      protected CommandADT createUndoCommand() { return new NullCommandADT(); }
    };
    assertThrows(IllegalArgumentException.class, () -> interpreter.encode(unknown));
  }

  @Test
  void decode_throwsForUnknownCommandType() {
    assertThrows(IllegalArgumentException.class,
        () -> interpreter.decode("UNKNOWN_TYPE:payload"));
  }

  @Test
  void decode_throwsForMalformedString() {
    assertThrows(IllegalArgumentException.class,
        () -> interpreter.decode("no-separator-here"));
  }

  // -----------------------------------------------------------------------
  // Tests — compound encode / decode
  // -----------------------------------------------------------------------

  @Test
  void encode_compoundCommand_startsWith_COMPOUND() {
    final PingCommand parent = new PingCommand("parent");
    parent.append(new PingCommand("child"));

    final String encoded = interpreter.encode(parent);
    assertTrue(encoded.startsWith("COMPOUND:"),
        "Expected COMPOUND: prefix, got: " + encoded);
  }

  @Test
  void decode_compoundCommand_restoresSubCommands() {
    final PingCommand parent = new PingCommand("parent");
    parent.append(new PingCommand("child1"));
    parent.append(new PingCommand("child2"));

    final String encoded = interpreter.encode(parent);
    final CommandADT decoded = interpreter.decode(encoded);

    assertInstanceOf(PingCommand.class, decoded);
    assertEquals("parent", ((PingCommand) decoded).payload);

    final CommandADT[] subs = decoded.getSubCommands();
    assertEquals(2, subs.length);
    assertInstanceOf(PingCommand.class, subs[0]);
    assertEquals("child1", ((PingCommand) subs[0]).payload);
    assertInstanceOf(PingCommand.class, subs[1]);
    assertEquals("child2", ((PingCommand) subs[1]).payload);
  }

  @Test
  void encodeDecodeRoundTrip_compoundWithMixedTypes() {
    final PingCommand parent = new PingCommand("top");
    parent.append(new PingCommand("mid"));
    parent.append(new NullCommandADT()); // NullCommandADT.isNull() == true -> ignored by append

    // Only the non-null sub-command should be present
    final String encoded = interpreter.encode(parent);
    final CommandADT decoded = interpreter.decode(encoded);

    assertInstanceOf(PingCommand.class, decoded);
    assertEquals("top", ((PingCommand) decoded).payload);
    assertEquals(1, decoded.getSubCommands().length);
  }

  // -----------------------------------------------------------------------
  // Tests — execute
  // -----------------------------------------------------------------------

  @Test
  void execute_callsCommandExecute() {
    final List<String> log = new ArrayList<>();
    final AbstractCommandADT cmd = new AbstractCommandADT() {
      @Override
      public String getCommandType() { return "X"; }

      @Override
      protected void executeInternal() { log.add("executed"); }

      @Override
      protected CommandADT createUndoCommand() { return new NullCommandADT(); }
    };

    interpreter.execute(cmd);

    assertEquals(List.of("executed"), log);
  }

  @Test
  void execute_nullCommand_throwsIAE() {
    assertThrows(IllegalArgumentException.class, () -> interpreter.execute(null));
  }
}
