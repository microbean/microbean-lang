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

import java.util.List;
import java.util.Objects;

import javax.lang.model.type.ErrorType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;

import javax.lang.model.util.SimpleTypeVisitor14;

import org.microbean.lang.ElementSource;

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

  private final ElementSource elementSource;
  
  private final Types types;

  private IsSameTypeVisitor isSameTypeVisitor;

  private SubtypeVisitor subtypeVisitor;

  public ContainsTypeVisitor(final ElementSource elementSource, final Types types) {
    super(Boolean.FALSE /* default value */);
    this.elementSource = Objects.requireNonNull(elementSource, "elementSource");
    this.types = Objects.requireNonNull(types, "types");
  }

  // https://github.com/openjdk/jdk/blob/jdk-20+12/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1524-L1531
  final boolean visit(final List<? extends TypeMirror> t, final List<? extends TypeMirror> s) {
    if (t.size() == s.size()) {
      for (int i = 0; i < t.size(); i++) {
        if (!this.visit(t.get(i), s.get(i))) {
          return false;
        }
      }
      return true;
    }
    return false;
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

  // https://github.com/openjdk/jdk/blob/jdk-20+12/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1585-L1596
  @Override
  public final Boolean visitWildcard(final WildcardType w, final TypeMirror s) {
    assert w.getKind() == TypeKind.WILDCARD;
    switch (s.getKind()) {
    case TYPEVAR:
      // Return true if s is a Capture that captures w, which just means that the wildcard that s captures is the same
      // one as w.
      return s instanceof Capture sct && this.visitWildcard(w, sct.getWildcardType());
    case WILDCARD:
      if (this.isSameTypeVisitor.visit(w, s)) {
        return true;
      }
      // fall through
    default:
      final TypeMirror wSuperBound = w.getSuperBound();
      if (wSuperBound == null) {
        // ? or ? extends Foo
        // Upper/extends-bounded (and possibly unbounded, which is the same thing).
        return this.subtypeVisitor.withCapture(false).visit(this.types.extendsBound(s), this.types.extendsBound(w));
      } else if (w.getExtendsBound() == null) {
        // ? super Foo
        // Lower/super-bounded.
        return this.subtypeVisitor.withCapture(false).visit(wSuperBound, this.types.superBound(s));
      } else {
        throw new IllegalArgumentException("w: " + w);
      }
    }
  }

}
