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
package org.microbean.lang.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.lang.model.element.AnnotationMirror;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;

public non-sealed class UnionType extends TypeMirror implements javax.lang.model.type.UnionType {

  // I don't even know if this is legal.
  public static final UnionType EMPTY = new UnionType() {
      @Override
      public final void addAlternative(final TypeMirror t) {
        throw new UnsupportedOperationException();
      }
    };

  private final List<TypeMirror> alternatives;

  private final List<TypeMirror> unmodifiableAlternatives;

  public UnionType() {
    super(TypeKind.UNION);
    this.alternatives = new ArrayList<>(5);
    this.unmodifiableAlternatives = Collections.unmodifiableList(this.alternatives);
  }

  @Override
  public final void addAnnotationMirror(final AnnotationMirror a) {
    throw new UnsupportedOperationException();
  }

  @Override // AnnotatedConstruct
  public final List<? extends AnnotationMirror> getAnnotationMirrors() {
    return List.of();
  }

  @Override // TypeMirror
  public <R, P> R accept(final TypeVisitor<R, P> v, P p) {
    return v.visitUnion(this, p);
  }

  @Override
  public final List<? extends TypeMirror> getAlternatives() {
    return this.unmodifiableAlternatives;
  }

  public void addAlternative(final TypeMirror t) {
    this.alternatives.add(validateAlternative(t));
  }

  public final void addAlternatives(final Iterable<? extends TypeMirror> ts) {
    for (final TypeMirror t : ts) {
      this.addAlternative(t);
    }
  }

  private final TypeMirror validateAlternative(final TypeMirror t) {
    if (Objects.requireNonNull(t, "t") == this) {
      throw new IllegalArgumentException("t: " + t);
    }
    return t;
  }

}
