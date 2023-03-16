/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2022–2023 microBean™.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.microbean.lang.element;

import javax.lang.model.element.Element;

/**
 * Something that can be enclosed by an {@link Element}.
 *
 * @author <a href="https://about.me/lairdnelson" target="_parent">Laird Nelson</a>
 *
 * @see #getEnclosingElement()
 *
 * @see #setEnclosingElement(Element)
 *
 * @see Element#getEnclosedElements()
 */
public interface Encloseable {

  /**
   * Returns the {@link Element} that encloses this {@link Encloseable}, or {@code null} if this {@link Encloseable} is
   * not currently enclosed.
   *
   * @return the {@link Element} that encloses this {@link Encloseable}, or {@code null}
   *
   * @see #setEnclosingElement(Element)
   */
  Element getEnclosingElement();

  /**
   * Sets the {@link Element} that encloses this {@link Encloseable} to the supplied {@link Element}, which may be
   * {@code null}.
   *
   * <p>An implementation of this method must ensure that a subsequent call to {@link #getEnclosingElement()} will
   * return the very {@link Element} supplied to this method, or undefined behavior will result.</p>
   *
   * @param enclosingElement the {@link Element} that will enclose this {@link Encloseable}; may be {@code null}
   *
   * @see #getEnclosingElement()
   */
  void setEnclosingElement(Element enclosingElement);

}
