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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.lang.model.element.Element;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import javax.lang.model.util.SimpleTypeVisitor14;

import org.microbean.lang.Equality;

import org.microbean.lang.element.DelegatingElement;

import org.microbean.lang.type.NoType;
import org.microbean.lang.type.Types;

// Basically done
//
// https://github.com/openjdk/jdk/blob/jdk-20+13/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L2165-L2221
final class AsSuperVisitor extends SimpleTypeVisitor14<TypeMirror, Element> {

  private final Set<DelegatingElement> seenTypes; // in the compiler, the field is called seenTypes but stores Symbols (Elements).

  private final Equality equality;

  private final Types types;

  private final SupertypeVisitor supertypeVisitor;

  private final SubtypeVisitor subtypeVisitor;

  AsSuperVisitor(final Equality equality,
                 final Types types,
                 final SupertypeVisitor supertypeVisitor,
                 final SubtypeVisitor subtypeVisitor) {
    super();
    this.seenTypes = new HashSet<>();
    this.equality = equality == null ? new Equality(true) : equality;
    this.types = Objects.requireNonNull(types, "types");
    this.supertypeVisitor = Objects.requireNonNull(supertypeVisitor, "supertypeVisitor");
    this.subtypeVisitor = Objects.requireNonNull(subtypeVisitor, "subtypeVisitor");
  }

  final TypeMirror asOuterSuper(TypeMirror t, final Element sym) {
    switch (t.getKind()) {
    case ARRAY:
      final TypeMirror elementType = sym.asType();
      return this.subtypeVisitor.withCapture(true).visit(t, elementType) ? elementType : null;
    case DECLARED:
    case INTERSECTION:
      do {
        final TypeMirror s = this.visit(t, sym);
        if (s != null) {
          return s;
        }
        t = t.getKind() == TypeKind.DECLARED ? ((DeclaredType)t).getEnclosingType() : NoType.NONE;
      } while (t.getKind() == TypeKind.DECLARED || t.getKind() == TypeKind.INTERSECTION);
      return null;
    case ERROR:
      return t;
    case TYPEVAR:
    default:
      return null;
    }
  }

  @Override
  public final TypeMirror visitArray(final ArrayType t, final Element sym) {
    assert t.getKind() == TypeKind.ARRAY;
    final TypeMirror x = sym.asType();
    return this.subtypeVisitor.withCapture(true).visit(t, x) ? x : null;
  }

  @Override
  public final TypeMirror visitDeclared(final DeclaredType t, final Element sym) {
    assert t.getKind() == TypeKind.DECLARED;
    return this.visitDeclaredOrIntersection(t, sym);
  }

  private final TypeMirror visitDeclaredOrIntersection(final TypeMirror t, final Element sym) {
    assert t.getKind() == TypeKind.DECLARED || t.getKind() == TypeKind.INTERSECTION;
    final Element te = this.types.asElement(t, true);
    if (te != null) {
      if (this.equality.equals(te, sym)) {
        return t;
      }
      final DelegatingElement c = DelegatingElement.of(te);
      if (this.seenTypes.add(c)) {
        try {
          final TypeMirror st = this.supertypeVisitor.visit(t);
          switch (st.getKind()) {
          case DECLARED:
          case INTERSECTION:
          case TYPEVAR:
            final TypeMirror x = this.visit(st, sym);
            if (x != null) {
              return x;
            }
            break;
          default:
            break;
          }
          if (sym.getKind().isInterface()) {
            for (final TypeMirror iface : this.supertypeVisitor.interfacesVisitor().visit(t)) {
              final TypeMirror x = this.visit(iface, sym);
              if (x != null) {
                return x;
              }
            }
          }
        } finally {
          this.seenTypes.remove(c);
        }
      }
    }
    return null;
  }

  @Override
  public final TypeMirror visitError(final ErrorType t, final Element sym) {
    assert t.getKind() == TypeKind.ERROR;
    return t;
  }

  @Override
  public final TypeMirror visitIntersection(final IntersectionType t, final Element sym) {
    assert t.getKind() == TypeKind.INTERSECTION;
    return this.visitDeclaredOrIntersection(t, sym);
  }

  @Override
  public final TypeMirror visitTypeVariable(final TypeVariable t, final Element sym) {
    assert t.getKind() == TypeKind.TYPEVAR;
    return this.equality.equals(t.asElement(), sym) ? t : this.visit(t.getUpperBound(), sym);
  }

}
