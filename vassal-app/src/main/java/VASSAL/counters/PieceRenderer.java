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
package VASSAL.counters;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

/**
 * Isolates the on-screen rendering of a {@link GamePiece} or {@link Decorator} trait from
 * its state/type/property logic. A {@link GamePiece} exposes its renderer via
 * {@link GamePiece#getRenderer()}; by default this simply wraps the piece's own
 * {@code draw}/{@code boundingBox}/{@code getShape} methods, so introducing this interface is
 * purely structural and does not change behavior. Traits with dedicated visual logic (e.g.
 * {@link Hideable}, {@link Footprint}) can supply their own {@link PieceRenderer} implementation
 * instead, keeping AWT/Graphics2D code out of the trait's state-handling methods.
 */
public interface PieceRenderer {
  /**
   * Draw the piece.
   * @param g target Graphics object
   * @param x x-location of the center of the piece
   * @param y y-location of the center of the piece
   * @param obs the Component on which this piece is being drawn
   * @param zoom the scaling factor.
   */
  void draw(Graphics g, int x, int y, Component obs, double zoom);

  /**
   * @return The area which the piece occupies when drawn at the point (0,0)
   */
  Rectangle boundingBox();

  /**
   * @return The shape of the piece from the user's viewpoint.
   */
  Shape getShape();
}
