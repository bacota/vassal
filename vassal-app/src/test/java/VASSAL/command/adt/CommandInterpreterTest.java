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

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import VASSAL.command.ConditionalCommand;

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

  // -----------------------------------------------------------------------
  // Round-trip tests for built-in command types
  // -----------------------------------------------------------------------

  @Test
  void roundTrip_changePiece() {
    final ChangePieceCommandADT cmd = new ChangePieceCommandADT("piece-1", "old", "new");
    final String encoded = interpreter.encode(cmd);
    assertTrue(encoded.startsWith(ChangePieceCommandADT.COMMAND_TYPE + ":"));
    final CommandADT decoded = interpreter.decode(encoded);
    assertInstanceOf(ChangePieceCommandADT.class, decoded);
    final ChangePieceCommandADT d = (ChangePieceCommandADT) decoded;
    assertEquals("piece-1", d.getId());
    assertEquals("old", d.getOldState());
    assertEquals("new", d.getNewState());
  }

  @Test
  void roundTrip_changePiece_nullOldState() {
    final ChangePieceCommandADT cmd = new ChangePieceCommandADT("piece-2", null, "new");
    final CommandADT decoded = interpreter.decode(interpreter.encode(cmd));
    assertInstanceOf(ChangePieceCommandADT.class, decoded);
    assertNull(((ChangePieceCommandADT) decoded).getOldState());
  }

  @Test
  void roundTrip_movePiece() {
    final MovePieceCommandADT cmd = new MovePieceCommandADT(
        "p1", "map-a", new Point(10, 20), "under-1",
        "map-b", new Point(5, 15), null, "player-x");
    final CommandADT decoded = interpreter.decode(interpreter.encode(cmd));
    assertInstanceOf(MovePieceCommandADT.class, decoded);
    final MovePieceCommandADT d = (MovePieceCommandADT) decoded;
    assertEquals("p1", d.getId());
    assertEquals("map-a", d.getNewMapId());
    assertEquals(10, d.getNewPosition().x);
    assertEquals(20, d.getNewPosition().y);
    assertEquals("under-1", d.getNewUnderneathId());
    assertEquals("map-b", d.getOldMapId());
    assertEquals(5, d.getOldPosition().x);
    assertEquals(15, d.getOldPosition().y);
    assertNull(d.getOldUnderneathId());
    assertEquals("player-x", d.getPlayerId());
  }

  @Test
  void roundTrip_addPiece() {
    final AddPieceCommandADT cmd = new AddPieceCommandADT("pid", "BasicPiece;;", "state-data");
    final CommandADT decoded = interpreter.decode(interpreter.encode(cmd));
    assertInstanceOf(AddPieceCommandADT.class, decoded);
    final AddPieceCommandADT d = (AddPieceCommandADT) decoded;
    assertEquals("pid", d.getPieceId());
    assertEquals("BasicPiece;;", d.getPieceType());
    assertEquals("state-data", d.getState());
  }

  @Test
  void roundTrip_removePiece() {
    final RemovePieceCommandADT cmd = new RemovePieceCommandADT("rid");
    final CommandADT decoded = interpreter.decode(interpreter.encode(cmd));
    assertInstanceOf(RemovePieceCommandADT.class, decoded);
    assertEquals("rid", ((RemovePieceCommandADT) decoded).getPieceId());
  }

  @Test
  void roundTrip_alert() {
    final AlertCommandADT cmd = new AlertCommandADT("Hello world!");
    final CommandADT decoded = interpreter.decode(interpreter.encode(cmd));
    assertInstanceOf(AlertCommandADT.class, decoded);
    assertEquals("Hello world!", ((AlertCommandADT) decoded).getMessage());
  }

  @Test
  void roundTrip_playAudioClip() {
    final PlayAudioClipCommandADT cmd = new PlayAudioClipCommandADT("fanfare.wav");
    final CommandADT decoded = interpreter.decode(interpreter.encode(cmd));
    assertInstanceOf(PlayAudioClipCommandADT.class, decoded);
    assertEquals("fanfare.wav", ((PlayAudioClipCommandADT) decoded).getClipName());
  }

  @Test
  void roundTrip_setPersistentProperty() {
    final SetPersistentPropertyCommandADT cmd =
        new SetPersistentPropertyCommandADT("pid", "myKey", "oldVal", "newVal");
    final CommandADT decoded = interpreter.decode(interpreter.encode(cmd));
    assertInstanceOf(SetPersistentPropertyCommandADT.class, decoded);
    final SetPersistentPropertyCommandADT d = (SetPersistentPropertyCommandADT) decoded;
    assertEquals("pid", d.getId());
    assertEquals("myKey", d.getKey());
    assertEquals("oldVal", d.getOldValue());
    assertEquals("newVal", d.getNewValue());
  }

  @Test
  void roundTrip_flare() {
    final FlareCommandADT cmd = new FlareCommandADT("Flare0", 100, 200);
    final CommandADT decoded = interpreter.decode(interpreter.encode(cmd));
    assertInstanceOf(FlareCommandADT.class, decoded);
    final FlareCommandADT d = (FlareCommandADT) decoded;
    assertEquals("Flare0", d.getFlareId());
    assertEquals(100, d.getClickX());
    assertEquals(200, d.getClickY());
  }

  @Test
  void roundTrip_conditional_withLtCondition() {
    final ConditionalCommand.Condition cond =
        new ConditionalCommand.Lt("vassal.version", "3.7.0");
    final ConditionalCommandADT cmd = new ConditionalCommandADT(
        new ConditionalCommand.Condition[]{cond},
        new AlertCommandADT("outdated version"));
    final CommandADT decoded = interpreter.decode(interpreter.encode(cmd));
    assertInstanceOf(ConditionalCommandADT.class, decoded);
    final ConditionalCommandADT d = (ConditionalCommandADT) decoded;
    assertEquals(1, d.getConditions().length);
    assertInstanceOf(ConditionalCommand.Lt.class, d.getConditions()[0]);
    assertInstanceOf(AlertCommandADT.class, d.getDelegate());
    assertEquals("outdated version", ((AlertCommandADT) d.getDelegate()).getMessage());
  }

  @Test
  void roundTrip_conditional_withEqCondition() {
    final ConditionalCommand.Condition cond =
        new ConditionalCommand.Eq("moduleVersion", List.of("1.0", "1.1"));
    final ConditionalCommandADT cmd = new ConditionalCommandADT(
        new ConditionalCommand.Condition[]{cond},
        new NullCommandADT());
    final CommandADT decoded = interpreter.decode(interpreter.encode(cmd));
    assertInstanceOf(ConditionalCommandADT.class, decoded);
    final ConditionalCommandADT d = (ConditionalCommandADT) decoded;
    assertInstanceOf(ConditionalCommand.Eq.class, d.getConditions()[0]);
    final ConditionalCommand.Eq eq =
        (ConditionalCommand.Eq) d.getConditions()[0];
    assertEquals("moduleVersion", eq.getProperty());
    assertEquals(List.of("1.0", "1.1"), eq.getValueList());
  }

  @Test
  void roundTrip_conditional_withNotCondition() {
    final ConditionalCommand.Condition inner =
        new ConditionalCommand.Gt("vassal.version", "3.0.0");
    final ConditionalCommand.Condition cond =
        new ConditionalCommand.Not(inner);
    final ConditionalCommandADT cmd = new ConditionalCommandADT(
        new ConditionalCommand.Condition[]{cond},
        new AlertCommandADT("too old"));
    final CommandADT decoded = interpreter.decode(interpreter.encode(cmd));
    assertInstanceOf(ConditionalCommandADT.class, decoded);
    assertInstanceOf(ConditionalCommand.Not.class,
        ((ConditionalCommandADT) decoded).getConditions()[0]);
  }

  @Test
  void roundTrip_changePiece_withTabInState() {
    // Verify that tab characters inside a payload are properly escaped
    final ChangePieceCommandADT cmd =
        new ChangePieceCommandADT("p", "old\twith\ttabs", "new\tstate");
    final String encoded = interpreter.encode(cmd);
    final CommandADT decoded = interpreter.decode(encoded);
    assertInstanceOf(ChangePieceCommandADT.class, decoded);
    assertEquals("old\twith\ttabs", ((ChangePieceCommandADT) decoded).getOldState());
    assertEquals("new\tstate", ((ChangePieceCommandADT) decoded).getNewState());
  }

  @Test
  void roundTrip_compoundWithNewCommandTypes() {
    final ChangePieceCommandADT parent = new ChangePieceCommandADT("p", "s0", "s1");
    parent.append(new AlertCommandADT("sub-alert"));
    final CommandADT decoded = interpreter.decode(interpreter.encode(parent));
    assertInstanceOf(ChangePieceCommandADT.class, decoded);
    assertEquals(1, decoded.getSubCommands().length);
    assertInstanceOf(AlertCommandADT.class, decoded.getSubCommands()[0]);
    assertEquals("sub-alert", ((AlertCommandADT) decoded.getSubCommands()[0]).getMessage());
  }

  @Test
  void allBuiltinCommandTypes_areRegisteredByDefault() {
    final CommandInterpreter interp = new CommandInterpreter();
    // All built-in types should encode without throwing IAE
    assertDoesNotThrow(() -> interp.encode(new NullCommandADT()));
    assertDoesNotThrow(() -> interp.encode(new ChangePieceCommandADT("id", "old", "new")));
    assertDoesNotThrow(() -> interp.encode(
        new MovePieceCommandADT("id", "m", new Point(0,0), null,
            "m", new Point(0,0), null, null)));
    assertDoesNotThrow(() -> interp.encode(new AddPieceCommandADT("id", "type", "state")));
    assertDoesNotThrow(() -> interp.encode(new RemovePieceCommandADT("id")));
    assertDoesNotThrow(() -> interp.encode(new AlertCommandADT("msg")));
    assertDoesNotThrow(() -> interp.encode(new PlayAudioClipCommandADT("clip")));
    assertDoesNotThrow(() -> interp.encode(
        new SetPersistentPropertyCommandADT("id", "k", "old", "new")));
    assertDoesNotThrow(() -> interp.encode(new FlareCommandADT("f0", 0, 0)));
    assertDoesNotThrow(() -> interp.encode(new ConditionalCommandADT(
        new ConditionalCommand.Condition[0], new NullCommandADT())));
  }
}
