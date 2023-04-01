/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2023 microBean™.
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

/**
 * An object that uses an {@link Equality} implementation for its {@link #hashCode()} and {@link #equals(Object)}
 * implementations, chiefly for use as a key in a {@link java.util.Map} (or a value in a {@link java.util.Set}).
 *
 * {@snippet lang="java" :
 * Map<Key<TypeMirror>, Object> map = new HashMap<>(); // @link substring="Map" target="java.util.Map" @replace substring="new HashMap<>()" replacement="..."
 * map.put(new Key<>(myTypeMirror, new Equality(false)), someValue); // @link substring="Equality" target="Equality"
 * assert map.containsKey(new Key<>(myTypeMirror, new Equality(false)));
 * }
 *
 * @param <T> the type of object a {@link Key} uses; most often an {@link javax.lang.model.AnnotatedConstruct} or subinterface
 *
 * @author <a href="https://about.me/lairdnelson" target="_parent">Laird Nelson</a>
 *
 * @see Equality
 */
public final class Key<T> {

  private final Equality eq;

  private final T k;

  public Key() {
    this(null, new Equality(true));
  }

  public Key(final T key) {
    this(key, new Equality(true));
  }

  public Key(final T key, final boolean includeAnnotations) {
    this(key, new Equality(includeAnnotations));
  }

  public Key(final T key, final Equality eq) {
    super();
    this.k = key;
    this.eq = eq == null ? new Equality(true) : eq;
  }

  @Override
  public final int hashCode() {
    return this.eq.hashCode(this.k);
  }

  @Override
  public final boolean equals(final Object other) {
    if (this == other) {
      return true;
    } else if (other != null && other.getClass() == Key.class) {
      return this.eq.equals(this.k, ((Key)other).k);
    } else {
      return false;
    }
  }

  @Override
  public final String toString() {
    return String.valueOf(this.k);
  }

}
