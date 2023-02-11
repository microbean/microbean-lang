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
package org.microbean.lang;

import java.util.HashMap;
import java.util.Map;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.lang.model.element.Element;

import javax.lang.model.type.TypeMirror;

// NOT thread safe
public class Modeler {

  private final Map<Object, Element> elements;

  private final Map<Object, TypeMirror> types;
  
  public Modeler() {
    super();
    this.elements = new HashMap<>();
    this.types = new HashMap<>();
  }

  @SuppressWarnings("unchecked")
  protected final <K, E extends Element> E element(final K k,
                                                   final Supplier<? extends E> s,
                                                   final BiConsumer<? super K, ? super E> c) {
    E r = (E)this.elements.get(k);
    if (r == null) {
      final E e = s.get();
      r = (E)this.elements.putIfAbsent(k, e);
      if (r == null) {
        c.accept(k, e);
        return e;
      }
    }
    return r;
  }

  @SuppressWarnings("unchecked")
  protected final <K, T extends TypeMirror> T type(final K k,
                                                   final Supplier<? extends T> s,
                                                   final BiConsumer<? super K, ? super T> c) {
    T r = (T)this.types.get(k);
    if (r == null) {
      final T t = s.get();
      r = (T)this.types.putIfAbsent(k, t);
      if (r == null) {
        c.accept(k, t);
        return t;
      }
    }
    return r;
  }
  
}
