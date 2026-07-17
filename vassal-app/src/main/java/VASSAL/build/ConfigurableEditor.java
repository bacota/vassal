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
package VASSAL.build;

import VASSAL.configure.Configurer;

/**
 * Optional capability of a {@link Configurable} that provides a Swing-based
 * property editor. Implementing this interface is how a {@link Configurable}
 * opts in to being editable via {@link #getConfigurer()}; classes that have no
 * editing UI simply don't implement it, and {@link Configurable#getConfigurer()}
 * will return {@code null} for them by default.
 */
public interface ConfigurableEditor {
  /**
   * @return a {@link Configurer} object which can be used to set the
   * attributes of this object, or {@code null} if none is available
   */
  Configurer getConfigurer();

  /**
   * @return the {@link Configurer} for {@code c} if it implements
   * {@link ConfigurableEditor}, or {@code null} otherwise
   */
  static Configurer getConfigurerOf(Configurable c) {
    return c instanceof ConfigurableEditor ? ((ConfigurableEditor) c).getConfigurer() : null;
  }
}
