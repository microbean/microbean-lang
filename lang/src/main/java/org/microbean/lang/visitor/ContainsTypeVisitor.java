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
package org.microbean.lang.visitor;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.lang.model.type.ErrorType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;

import javax.lang.model.util.SimpleTypeVisitor14;

import org.microbean.lang.type.Capture;
import org.microbean.lang.type.Types;

// Basically done.
/*
 * <p>From the documentation of {@code com.sun.tools.javac.code.Types#containsType(Type, Type)}:</p>
 *
 * <blockquote><p>Check if {@code t} contains {@code s}.</p>
 *
 * <p>{@code T} contains {@code S} if:</p>
 *
 * <p>{@code L(T) <: L(S) && U(S) <: U(T)}</p>
 *
 * <p>This relation is only used by {@code ClassType.isSubtype()} [in fact this is not true], that is,</p>
 *
 * <p>{@code C<S> <: C<T> if T contains S.}</p>
 *
 * <p>Because of F-bounds, this relation can lead to infinite recursion.  Thus we must somehow break that recursion.
 * Notice that containsType() is only called from ClassType.isSubtype() [not true].  Since the arguments have already
 * been checked against their bounds [not true], we know:</p>
 *
 * <p>{@code U(S) <: U(T) if T is "super" bound (U(T) *is* the bound)}</p>
 *
 * <p>{@code L(T) <: L(S) if T is "extends" bound (L(T) is bottom)}</p></blockquote>
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jls/se19/html/jls-4.html#jls-4.5.1">Type Arguments of
 * Parameterized Types</a>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Bounded_quantification#F-bounded_quantification">F-bounded
 * quantification</a>
 */
// https://github.com/openjdk/jdk/blob/jdk-20+12/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1562-L1611
public final class ContainsTypeVisitor extends SimpleTypeVisitor14<Boolean, TypeMirror> {

  private final Types types;

  private IsSameTypeVisitor isSameTypeVisitor;

  private SubtypeVisitor subtypeVisitor;

  public ContainsTypeVisitor(final Types types) {
    super(Boolean.FALSE);
    this.types = Objects.requireNonNull(types, "types");
  }

  final boolean visit(final List<? extends TypeMirror> t, final List<? extends TypeMirror> s) {
    final Iterator<? extends TypeMirror> tIterator = t.iterator();
    final Iterator<? extends TypeMirror> sIterator = s.iterator();
    while (tIterator.hasNext() && sIterator.hasNext() && this.visit(tIterator.next(), sIterator.next())) {
      // do nothing
    }
    return !tIterator.hasNext() && !sIterator.hasNext();
  }

  public final void setIsSameTypeVisitor(final IsSameTypeVisitor v) {
    if (this.isSameTypeVisitor != null) {
      throw new IllegalStateException();
    }
    this.isSameTypeVisitor = Objects.requireNonNull(v, "v");
  }

  public final void setSubtypeVisitor(final SubtypeVisitor v) {
    if (this.subtypeVisitor != null) {
      throw new IllegalStateException();
    }
    this.subtypeVisitor = Objects.requireNonNull(v, "v");
  }

  @Override
  protected final Boolean defaultAction(final TypeMirror t, final TypeMirror s) {
    return this.isSameTypeVisitor.visit(t, s);
  }

  @Override
  public final Boolean visitError(final ErrorType e, final TypeMirror s) {
    assert e.getKind() == TypeKind.ERROR;
    return Boolean.TRUE;
  }

  @Override
  public final Boolean visitWildcard(final WildcardType w, final TypeMirror s) {
    assert w.getKind() == TypeKind.WILDCARD;
    switch (s.getKind()) {
    case TYPEVAR:
      // Return true if s is a SyntheticCapturedType that captures w,
      // which just means that the wildcard that s captures is the
      // same one as w.
      return s instanceof Capture sct && this.visitWildcard(w, sct.getWildcardType());
    case WILDCARD:
      return this.visitWildcard(w, (WildcardType)s);
    default:
      return false;
    }
  }

  private final boolean visitWildcard(final WildcardType t, final WildcardType s) {
    assert t.getKind() == TypeKind.WILDCARD && s.getKind() == TypeKind.WILDCARD;
    if (this.isSameTypeVisitor.visit(t, s)) {
      return true;
    }
    final TypeMirror tSuperBound = t.getSuperBound();
    if (tSuperBound == null) {
      // Extends bounded (or unbounded).  So extends bounded AND:
      return this.subtypeVisitor.withCapture(false).visit(this.types.extendsBound(s), this.types.extendsBound(t));
    } else if (t.getExtendsBound() == null) {
      // Super bounded.  So super bounded AND:
      return this.subtypeVisitor.withCapture(false).visit(tSuperBound, this.types.superBound(s));
    } else {
      throw new IllegalArgumentException("t: " + t + "; s: " + s);
    }
  }

}
