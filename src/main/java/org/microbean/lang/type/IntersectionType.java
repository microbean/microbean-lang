/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2022 microBean™.
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
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import javax.lang.model.AnnotatedConstruct;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;

public final class IntersectionType extends TypeMirror implements javax.lang.model.type.IntersectionType {

  private final List<javax.lang.model.type.TypeMirror> bounds;

  private final List<javax.lang.model.type.TypeMirror> unmodifiableBounds;

  public IntersectionType() {
    super(TypeKind.INTERSECTION);
    this.bounds = new ArrayList<>(5);
    this.unmodifiableBounds = Collections.unmodifiableList(this.bounds);
  }

  public IntersectionType(final List<? extends javax.lang.model.type.TypeMirror> bounds) {
    this();
    this.addBounds(bounds);
  }

  @Override
  public final void addAnnotationMirror(final AnnotationMirror a) {
    throw new UnsupportedOperationException();
  }

  @Override // TypeMirror
  public final <R, P> R accept(final TypeVisitor<R, P> v, P p) {
    return v.visitIntersection(this, p);
  }

  @Override // IntersectionType
  public final List<? extends javax.lang.model.type.TypeMirror> getBounds() {
    return this.unmodifiableBounds;
  }

  public final void addBound(final javax.lang.model.type.TypeMirror bound) {
    this.bounds.add(validateBound(bound));
  }

  public final void addBounds(final Iterable<? extends javax.lang.model.type.TypeMirror> bounds) {
    for (final javax.lang.model.type.TypeMirror bound : bounds) {
      this.addBound(bound);
    }
  }

  @Override
  public final String toString() {
    final StringJoiner sj = new StringJoiner(" & ");
    for (final javax.lang.model.type.TypeMirror bound : this.bounds) {
      sj.add(bound.toString());
    }
    return sj.toString();
  }

  private final javax.lang.model.type.TypeMirror validateBound(final javax.lang.model.type.TypeMirror bound) {
    if (Objects.requireNonNull(bound, "bound") == this) {
      throw new IllegalArgumentException("bound: " + bound);
    }
    return bound;
  }

  public static IntersectionType of(final List<? extends javax.lang.model.type.TypeMirror> bounds) {
    return new IntersectionType(bounds);
  }

}
