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

  protected final Map<Object, Element> elements;

  protected final Map<Object, TypeMirror> types;
  
  public Modeler() {
    super();
    this.elements = new HashMap<>();
    this.types = new HashMap<>();
  }
  
  public static final <K, E extends Element> Element element(final K k,
                                                             final Supplier<? extends E> s,
                                                             final BiFunction<? super K, ? super E, ? extends Element> f,
                                                             final BiConsumer<? super K, ? super E> c) {
    final E e = s.get();
    Element r = f.apply(k, e);
    if (r == null) {
      c.accept(k, e);
      r = e;
    }
    return r;
  }

  public static final <K, T extends TypeMirror> TypeMirror type(final K k,
                                                                final Supplier<? extends T> s,
                                                                final BiFunction<? super K, ? super T, ? extends TypeMirror> f,
                                                                final BiConsumer<? super K, ? super T> c) {
    final T t = s.get();
    TypeMirror r = f.apply(k, t);
    if (r == null) {
      c.accept(k, t);
      r = t;
    }
    return r;
  }

}
