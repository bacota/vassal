/*
 *
 * Copyright (c) 2000-2003 by Rodney Kinney
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
package VASSAL.build.module;

import java.awt.Point;
import java.awt.Rectangle;

import VASSAL.build.module.map.PieceCollection;
import VASSAL.build.module.map.boardPicker.Board;
import VASSAL.build.module.map.boardPicker.board.Region;
import VASSAL.build.module.map.boardPicker.board.mapgrid.Zone;
import VASSAL.counters.GamePiece;
import VASSAL.counters.PieceFinder;

/**
 * The board/grid/piece-geometry contract of a {@link Map}, isolated from the surrounding
 * Swing component so it can be reasoned about (and eventually reused) independently.
 */
public interface MapModel {
  /** @return the {@link Board} at map coordinate {@code p}, or {@code null} if none */
  Board findBoard(Point p);

  /** @return the {@link Zone} at map coordinate {@code p}, or {@code null} if none */
  Zone findZone(Point p);

  /** @return the {@link Zone} named {@code name}, or {@code null} if none */
  Zone findZone(String name);

  /** @return the {@link Region} named {@code name}, or {@code null} if none */
  Region findRegion(String name);

  /** @return {@code p} snapped to the nearest grid location, per this map's grid settings */
  Point snapTo(Point p, boolean force, boolean onlyCenter);

  /** @return {@code p} converted from map coordinates to component coordinates */
  Point mapToComponent(Point p);

  /** @return {@code r} converted from map coordinates to component coordinates */
  Rectangle mapToComponent(Rectangle r);

  /** @return {@code p} converted from component coordinates to map coordinates */
  Point componentToMap(Point p);

  /** @return {@code r} converted from component coordinates to map coordinates */
  Rectangle componentToMap(Rectangle r);

  /** @return a description of the board/zone/deck location at map coordinate {@code p} */
  String locationName(Point p);

  /** @return all {@link GamePiece}s currently on this map */
  GamePiece[] getAllPieces();

  /** @return the {@link PieceCollection} backing this map */
  PieceCollection getPieceCollection();

  /** @return the topmost piece at {@code pt} accepted by {@code finder}, or {@code null} */
  GamePiece findPiece(Point pt, PieceFinder finder);

  /** @return the topmost piece at {@code pt} accepted by {@code finder}, searching all stacks */
  GamePiece findAnyPiece(Point pt, PieceFinder finder);

  /** Adds {@code p} to this map */
  void addPiece(GamePiece p);

  /** Removes {@code p} from this map */
  void removePiece(GamePiece p);
}
