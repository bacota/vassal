/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

import VASSAL.build.GameModule;
import VASSAL.counters.Deck;

/**
 * ADT representation of {@link Deck.LoadDeckCommand}.
 *
 * <p>When executed, clears the {@link Deck} identified by {@code deckId}
 * and repopulates it from the sub-commands (which should be
 * {@link AddPieceCommandADT} instances appended to this command).
 *
 * <p>The actual piece-addition is handled by the sub-commands that must be
 * appended to this command before execution.
 */
public class DeckLoadDeckCommandADT extends AbstractCommandADT {

  public static final String COMMAND_TYPE = "DECK_LOAD";

  private final String deckId;

  public DeckLoadDeckCommandADT(String deckId) {
    this.deckId = deckId;
  }

  @Override
  public String getCommandType() { return COMMAND_TYPE; }

  @Override
  protected void executeInternal() {
    final GameModule gm = GameModule.getGameModule();
    if (gm == null) return;
    final Deck deck = (Deck) gm.getGameState().getPieceForId(deckId);
    if (deck != null) {
      deck.removeAll();
    }
  }

  @Override
  protected CommandADT createUndoCommand() { return new NullCommandADT(); }

  @Override
  public String getDetails() { return "deckId=" + deckId; } //NON-NLS

  public String getDeckId() { return deckId; }
}
