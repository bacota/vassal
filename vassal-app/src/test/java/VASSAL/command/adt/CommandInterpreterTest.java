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
    // New command types
    assertDoesNotThrow(() -> interp.encode(new SetupCommandADT(true)));
    assertDoesNotThrow(() -> interp.encode(new BasicLoggerUndoCommandADT(false)));
    assertDoesNotThrow(() -> interp.encode(new BasicLoggerLogCommandADT(new NullCommandADT())));
    assertDoesNotThrow(() -> interp.encode(new ModuleExtensionRegCmdADT("MyExt", "1.0")));
    assertDoesNotThrow(() -> interp.encode(new ObscurableOptionsSetAllowedADT(List.of("id1", "id2"))));
    assertDoesNotThrow(() -> interp.encode(new NewGameIndicatorMarkGameNotNewADT()));
    assertDoesNotThrow(() -> interp.encode(new ChatterDisplayTextADT("hello")));
    assertDoesNotThrow(() -> interp.encode(new NotesWindowSetScenarioNoteADT("scenario")));
    assertDoesNotThrow(() -> interp.encode(new NotesWindowSetPublicNoteADT("public")));
    assertDoesNotThrow(() -> interp.encode(new PlayerRosterAddADT("pid", "Alice", "Allies")));
    assertDoesNotThrow(() -> interp.encode(new PlayerRosterRemoveADT("pid")));
    assertDoesNotThrow(() -> interp.encode(new EventLogStoreEventsADT("events")));
    assertDoesNotThrow(() -> interp.encode(new TurnTrackerSetTurnADT("tt0", "state1", "state0")));
    assertDoesNotThrow(() -> interp.encode(
        new GlobalPropertySetGlobalPropertyADT("prop", "cid", "old", "new")));
    assertDoesNotThrow(() -> interp.encode(
        new LOSThreadLOSCommandADT("los0", 1,2,3,4,true,false,false, 0,0,0,0,false,false)));
    assertDoesNotThrow(() -> interp.encode(
        new SpecialDiceButtonShowResultsADT("sdb0", new int[]{1,2,3})));
    assertDoesNotThrow(() -> interp.encode(new BoardPickerSetBoardsADT("mapId",
        List.of(new BoardPickerSetBoardsADT.BoardEntry("BoardA", false, 0, 0)))));
    assertDoesNotThrow(() -> interp.encode(new DeckLoadDeckCommandADT("deck1")));
    assertDoesNotThrow(() -> interp.encode(
        new LockScenarioOptionsTabADT("tab", "Bob", "pw", "2024-01-01", "", "", "")));
    assertDoesNotThrow(() -> interp.encode(
        new AddSecretNoteCommandADT("note1", "owner1", "text1", true, -1L, "handle1")));
    assertDoesNotThrow(() -> interp.encode(new SetPrivateTextCommandADT("owner1", "private text")));
    assertDoesNotThrow(() -> interp.encode(
        new ChangePropertyCommandADT("container1", "prop1", "old", "new")));
    assertDoesNotThrow(() -> interp.encode(new InviteCommandADT("Alice", "pid", "room1")));
    assertDoesNotThrow(() -> interp.encode(new PrivMsgCommandADT("Alice", "hi!")));
    assertDoesNotThrow(() -> interp.encode(new SynchCommandADT("Alice")));
    assertDoesNotThrow(() -> interp.encode(new SoundEncoderCmdADT("wake")));
    assertDoesNotThrow(() -> interp.encode(new TextClientShowTextADT("hello stdout")));
  }

  // -----------------------------------------------------------------------
  // Round-trip tests for the new 27 command types
  // -----------------------------------------------------------------------

  @Test
  void roundTrip_setupCommand() {
    for (final boolean b : new boolean[]{true, false}) {
      final CommandADT decoded = interpreter.decode(interpreter.encode(new SetupCommandADT(b)));
      assertInstanceOf(SetupCommandADT.class, decoded);
      assertEquals(b, ((SetupCommandADT) decoded).isGameStarting());
    }
  }

  @Test
  void roundTrip_basicLoggerUndoCommand() {
    for (final boolean b : new boolean[]{true, false}) {
      final CommandADT decoded = interpreter.decode(interpreter.encode(new BasicLoggerUndoCommandADT(b)));
      assertInstanceOf(BasicLoggerUndoCommandADT.class, decoded);
      assertEquals(b, ((BasicLoggerUndoCommandADT) decoded).isInProgress());
    }
  }

  @Test
  void roundTrip_basicLoggerLogCommand() {
    final BasicLoggerLogCommandADT cmd = new BasicLoggerLogCommandADT(new AlertCommandADT("inner"));
    final CommandADT decoded = interpreter.decode(interpreter.encode(cmd));
    assertInstanceOf(BasicLoggerLogCommandADT.class, decoded);
    assertInstanceOf(AlertCommandADT.class, ((BasicLoggerLogCommandADT) decoded).getLoggedCommand());
    assertEquals("inner", ((AlertCommandADT)
        ((BasicLoggerLogCommandADT) decoded).getLoggedCommand()).getMessage());
  }

  @Test
  void roundTrip_moduleExtensionRegCmd() {
    final CommandADT decoded = interpreter.decode(
        interpreter.encode(new ModuleExtensionRegCmdADT("MyExt", "2.3.0")));
    assertInstanceOf(ModuleExtensionRegCmdADT.class, decoded);
    assertEquals("MyExt", ((ModuleExtensionRegCmdADT) decoded).getName());
    assertEquals("2.3.0", ((ModuleExtensionRegCmdADT) decoded).getVersion());
  }

  @Test
  void roundTrip_obscurableOptionsSetAllowed() {
    final List<String> ids = List.of("player1", "player2");
    final CommandADT decoded = interpreter.decode(
        interpreter.encode(new ObscurableOptionsSetAllowedADT(ids)));
    assertInstanceOf(ObscurableOptionsSetAllowedADT.class, decoded);
    assertEquals(ids, ((ObscurableOptionsSetAllowedADT) decoded).getAllowedIds());
  }

  @Test
  void roundTrip_obscurableOptionsSetAllowed_empty() {
    final CommandADT decoded = interpreter.decode(
        interpreter.encode(new ObscurableOptionsSetAllowedADT(List.of())));
    assertInstanceOf(ObscurableOptionsSetAllowedADT.class, decoded);
    assertTrue(((ObscurableOptionsSetAllowedADT) decoded).getAllowedIds().isEmpty());
  }

  @Test
  void roundTrip_newGameIndicatorMarkGameNotNew() {
    final CommandADT decoded = interpreter.decode(
        interpreter.encode(new NewGameIndicatorMarkGameNotNewADT()));
    assertInstanceOf(NewGameIndicatorMarkGameNotNewADT.class, decoded);
  }

  @Test
  void roundTrip_chatterDisplayText() {
    final CommandADT decoded = interpreter.decode(
        interpreter.encode(new ChatterDisplayTextADT("Hello world")));
    assertInstanceOf(ChatterDisplayTextADT.class, decoded);
    assertEquals("Hello world", ((ChatterDisplayTextADT) decoded).getMessage());
  }

  @Test
  void roundTrip_notesWindowSetScenarioNote() {
    final CommandADT decoded = interpreter.decode(
        interpreter.encode(new NotesWindowSetScenarioNoteADT("Scenario note")));
    assertInstanceOf(NotesWindowSetScenarioNoteADT.class, decoded);
    assertEquals("Scenario note", ((NotesWindowSetScenarioNoteADT) decoded).getMessage());
  }

  @Test
  void roundTrip_notesWindowSetPublicNote() {
    final CommandADT decoded = interpreter.decode(
        interpreter.encode(new NotesWindowSetPublicNoteADT("Public note")));
    assertInstanceOf(NotesWindowSetPublicNoteADT.class, decoded);
    assertEquals("Public note", ((NotesWindowSetPublicNoteADT) decoded).getMessage());
  }

  @Test
  void roundTrip_playerRosterAdd() {
    final CommandADT decoded = interpreter.decode(
        interpreter.encode(new PlayerRosterAddADT("pid1", "Alice", "Allies")));
    assertInstanceOf(PlayerRosterAddADT.class, decoded);
    final PlayerRosterAddADT d = (PlayerRosterAddADT) decoded;
    assertEquals("pid1", d.getPlayerId());
    assertEquals("Alice", d.getPlayerName());
    assertEquals("Allies", d.getSide());
  }

  @Test
  void roundTrip_playerRosterRemove() {
    final CommandADT decoded = interpreter.decode(
        interpreter.encode(new PlayerRosterRemoveADT("pid2")));
    assertInstanceOf(PlayerRosterRemoveADT.class, decoded);
    assertEquals("pid2", ((PlayerRosterRemoveADT) decoded).getPlayerId());
  }

  @Test
  void roundTrip_eventLogStoreEvents() {
    final CommandADT decoded = interpreter.decode(
        interpreter.encode(new EventLogStoreEventsADT("event1;event2;event3")));
    assertInstanceOf(EventLogStoreEventsADT.class, decoded);
    assertEquals("event1;event2;event3", ((EventLogStoreEventsADT) decoded).getEvents());
  }

  @Test
  void roundTrip_turnTrackerSetTurn() {
    final CommandADT decoded = interpreter.decode(
        interpreter.encode(new TurnTrackerSetTurnADT("tt0", "Round 2", "Round 1")));
    assertInstanceOf(TurnTrackerSetTurnADT.class, decoded);
    final TurnTrackerSetTurnADT d = (TurnTrackerSetTurnADT) decoded;
    assertEquals("tt0", d.getTrackerId());
    assertEquals("Round 2", d.getNewState());
    assertEquals("Round 1", d.getOldState());
  }

  @Test
  void roundTrip_globalPropertySetGlobalProperty() {
    final CommandADT decoded = interpreter.decode(interpreter.encode(
        new GlobalPropertySetGlobalPropertyADT("myProp", "myContainer", "oldVal", "newVal")));
    assertInstanceOf(GlobalPropertySetGlobalPropertyADT.class, decoded);
    final GlobalPropertySetGlobalPropertyADT d = (GlobalPropertySetGlobalPropertyADT) decoded;
    assertEquals("myProp", d.getPropertyName());
    assertEquals("myContainer", d.getContainerId());
    assertEquals("oldVal", d.getOldValue());
    assertEquals("newVal", d.getNewValue());
  }

  @Test
  void roundTrip_losThreadLOSCommand() {
    final LOSThreadLOSCommandADT cmd = new LOSThreadLOSCommandADT(
        "los0", 10, 20, 30, 40, true, false, false,
        0, 0, 0, 0, false, false);
    final CommandADT decoded = interpreter.decode(interpreter.encode(cmd));
    assertInstanceOf(LOSThreadLOSCommandADT.class, decoded);
    final LOSThreadLOSCommandADT d = (LOSThreadLOSCommandADT) decoded;
    assertEquals("los0", d.getThreadId());
    assertEquals(10, d.getNewAnchorX());
    assertEquals(20, d.getNewAnchorY());
    assertEquals(30, d.getNewArrowX());
    assertEquals(40, d.getNewArrowY());
    assertTrue(d.isNewPersisting());
    assertFalse(d.isNewMirroring());
    assertFalse(d.isReset());
  }

  @Test
  void roundTrip_losThreadLOSCommand_reset() {
    final LOSThreadLOSCommandADT cmd = new LOSThreadLOSCommandADT(
        "los1", 0, 0, 0, 0, false, false, true,
        5, 6, 7, 8, false, false);
    final CommandADT decoded = interpreter.decode(interpreter.encode(cmd));
    assertInstanceOf(LOSThreadLOSCommandADT.class, decoded);
    assertTrue(((LOSThreadLOSCommandADT) decoded).isReset());
  }

  @Test
  void roundTrip_specialDiceButtonShowResults() {
    final SpecialDiceButtonShowResultsADT cmd =
        new SpecialDiceButtonShowResultsADT("sdb0", new int[]{3, 1, 4, 1, 5});
    final CommandADT decoded = interpreter.decode(interpreter.encode(cmd));
    assertInstanceOf(SpecialDiceButtonShowResultsADT.class, decoded);
    final SpecialDiceButtonShowResultsADT d = (SpecialDiceButtonShowResultsADT) decoded;
    assertEquals("sdb0", d.getButtonId());
    final int[] rolls = d.getRolls();
    assertEquals(5, rolls.length);
    assertEquals(3, rolls[0]);
    assertEquals(5, rolls[4]);
  }

  @Test
  void roundTrip_boardPickerSetBoards_empty() {
    final CommandADT decoded = interpreter.decode(
        interpreter.encode(new BoardPickerSetBoardsADT("myMap", List.of())));
    assertInstanceOf(BoardPickerSetBoardsADT.class, decoded);
    final BoardPickerSetBoardsADT d = (BoardPickerSetBoardsADT) decoded;
    assertEquals("myMap", d.getMapId());
    assertTrue(d.getBoardEntries().isEmpty());
  }

  @Test
  void roundTrip_boardPickerSetBoards_withEntries() {
    final List<BoardPickerSetBoardsADT.BoardEntry> entries = List.of(
        new BoardPickerSetBoardsADT.BoardEntry("BoardA", true, 3, 7),
        new BoardPickerSetBoardsADT.BoardEntry("BoardB", false, 0, 0)
    );
    final CommandADT decoded = interpreter.decode(
        interpreter.encode(new BoardPickerSetBoardsADT("myMap", entries)));
    assertInstanceOf(BoardPickerSetBoardsADT.class, decoded);
    final List<BoardPickerSetBoardsADT.BoardEntry> decodedEntries =
        ((BoardPickerSetBoardsADT) decoded).getBoardEntries();
    assertEquals(2, decodedEntries.size());
    assertEquals("BoardA", decodedEntries.get(0).name);
    assertTrue(decodedEntries.get(0).reversed);
    assertEquals(3, decodedEntries.get(0).relX);
    assertEquals(7, decodedEntries.get(0).relY);
    assertEquals("BoardB", decodedEntries.get(1).name);
  }

  @Test
  void roundTrip_deckLoadDeckCommand() {
    final CommandADT decoded = interpreter.decode(
        interpreter.encode(new DeckLoadDeckCommandADT("deck-42")));
    assertInstanceOf(DeckLoadDeckCommandADT.class, decoded);
    assertEquals("deck-42", ((DeckLoadDeckCommandADT) decoded).getDeckId());
  }

  @Test
  void roundTrip_lockScenarioOptionsTab() {
    final CommandADT decoded = interpreter.decode(interpreter.encode(
        new LockScenarioOptionsTabADT("Tab1", "Bob", "secret", "2024-01-15",
            "", "", "")));
    assertInstanceOf(LockScenarioOptionsTabADT.class, decoded);
    final LockScenarioOptionsTabADT d = (LockScenarioOptionsTabADT) decoded;
    assertEquals("Tab1", d.getTabName());
    assertEquals("Bob", d.getLockedBy());
    assertEquals("secret", d.getLockedPw());
    assertEquals("2024-01-15", d.getLockedDt());
  }

  @Test
  void roundTrip_lockScenarioOptionsTab_undo() {
    final LockScenarioOptionsTabADT cmd = new LockScenarioOptionsTabADT(
        "Tab1", "Bob", "pw", "now", "Alice", "pw2", "then");
    final CommandADT undo = cmd.getUndoCommand();
    assertInstanceOf(LockScenarioOptionsTabADT.class, undo);
    final LockScenarioOptionsTabADT u = (LockScenarioOptionsTabADT) undo;
    assertEquals("Alice", u.getLockedBy());
    assertEquals("pw2", u.getLockedPw());
    assertEquals("then", u.getLockedDt());
    assertEquals("Bob", u.getOldLockedBy());
  }

  @Test
  void roundTrip_addSecretNoteCommand() {
    final long now = 1704067200000L; // 2024-01-01 00:00:00 UTC
    final CommandADT decoded = interpreter.decode(interpreter.encode(
        new AddSecretNoteCommandADT("My Note", "owner123", "Note body", true, now, "myHandle")));
    assertInstanceOf(AddSecretNoteCommandADT.class, decoded);
    final AddSecretNoteCommandADT d = (AddSecretNoteCommandADT) decoded;
    assertEquals("My Note", d.getName());
    assertEquals("owner123", d.getOwner());
    assertEquals("Note body", d.getText());
    assertTrue(d.isHidden());
    assertEquals(now, d.getDateMillis());
    assertEquals("myHandle", d.getHandle());
  }

  @Test
  void roundTrip_addSecretNoteCommand_nullDate() {
    final CommandADT decoded = interpreter.decode(interpreter.encode(
        new AddSecretNoteCommandADT("n", "o", "t", false, -1L, "h")));
    assertInstanceOf(AddSecretNoteCommandADT.class, decoded);
    assertEquals(-1L, ((AddSecretNoteCommandADT) decoded).getDateMillis());
  }

  @Test
  void roundTrip_setPrivateTextCommand() {
    final CommandADT decoded = interpreter.decode(interpreter.encode(
        new SetPrivateTextCommandADT("owner1", "private text content")));
    assertInstanceOf(SetPrivateTextCommandADT.class, decoded);
    assertEquals("owner1", ((SetPrivateTextCommandADT) decoded).getOwner());
    assertEquals("private text content", ((SetPrivateTextCommandADT) decoded).getText());
  }

  @Test
  void roundTrip_changePropertyCommand() {
    final CommandADT decoded = interpreter.decode(interpreter.encode(
        new ChangePropertyCommandADT("container1", "prop1", "oldVal", "newVal")));
    assertInstanceOf(ChangePropertyCommandADT.class, decoded);
    final ChangePropertyCommandADT d = (ChangePropertyCommandADT) decoded;
    assertEquals("container1", d.getContainerId());
    assertEquals("prop1", d.getPropertyName());
    assertEquals("oldVal", d.getOldValue());
    assertEquals("newVal", d.getNewValue());
  }

  @Test
  void roundTrip_inviteCommand() {
    final CommandADT decoded = interpreter.decode(interpreter.encode(
        new InviteCommandADT("Alice", "player-id-1", "Room A")));
    assertInstanceOf(InviteCommandADT.class, decoded);
    final InviteCommandADT d = (InviteCommandADT) decoded;
    assertEquals("Alice", d.getPlayer());
    assertEquals("player-id-1", d.getPlayerId());
    assertEquals("Room A", d.getRoom());
    assertFalse(d.isLoggable());
  }

  @Test
  void roundTrip_privMsgCommand() {
    final CommandADT decoded = interpreter.decode(interpreter.encode(
        new PrivMsgCommandADT("Alice", "Hello Bob!")));
    assertInstanceOf(PrivMsgCommandADT.class, decoded);
    assertEquals("Alice", ((PrivMsgCommandADT) decoded).getSenderName());
    assertEquals("Hello Bob!", ((PrivMsgCommandADT) decoded).getMessage());
    assertFalse(decoded.isLoggable());
  }

  @Test
  void roundTrip_synchCommand() {
    final CommandADT decoded = interpreter.decode(interpreter.encode(
        new SynchCommandADT("Bob")));
    assertInstanceOf(SynchCommandADT.class, decoded);
    assertEquals("Bob", ((SynchCommandADT) decoded).getRecipientName());
    assertFalse(decoded.isLoggable());
  }

  @Test
  void roundTrip_soundEncoderCmd() {
    final CommandADT decoded = interpreter.decode(interpreter.encode(
        new SoundEncoderCmdADT("wakeup.wav")));
    assertInstanceOf(SoundEncoderCmdADT.class, decoded);
    assertEquals("wakeup.wav", ((SoundEncoderCmdADT) decoded).getSoundKey());
    assertFalse(decoded.isLoggable());
  }

  @Test
  void roundTrip_textClientShowText() {
    final CommandADT decoded = interpreter.decode(interpreter.encode(
        new TextClientShowTextADT("Hello stdout!")));
    assertInstanceOf(TextClientShowTextADT.class, decoded);
    assertEquals("Hello stdout!", ((TextClientShowTextADT) decoded).getMessage());
  }
}
