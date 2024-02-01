/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2022–2024 microBean™.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.microbean.lang;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

// Totally ordering (I hope!) Comparator inconsistent with equals that uses names internally to compare types. The names
// used are deliberately undefined but incorporate fully qualified names where possible as well as type argument and
// bound information.
//
// This comparator isn't very useful on its own, although it does establish a total order. Typically you'll want to
// compose this as a last-ditch tiebreaker with ClassesFirstTypeMirrorComparator and
// SpecializationDepthTypeMirrorComparator.
public final class NameTypeMirrorComparator implements Comparator<TypeMirror> {

  public static final NameTypeMirrorComparator INSTANCE = new NameTypeMirrorComparator();

  private NameTypeMirrorComparator() {
    super();
  }

  @Override // Comparator<TypeMirror>
  public final int compare(final TypeMirror t, final TypeMirror s) {
    return
      t == s ? 0 :
      t == null ? 1 :
      s == null ? -1 :
      CharSequence.compare(this.name(t), this.name(s));
  }

  final CharSequence name(final TypeMirror t) {
    return switch (t.getKind()) {
    case ARRAY -> name(((ArrayType)t).getComponentType()) + "[]";
    case BOOLEAN -> "boolean";
    case BYTE -> "byte";
    case CHAR -> "char";
    case DECLARED -> {
      final DeclaredType dt = (DeclaredType)t;
      final TypeElement e = (TypeElement)dt.asElement();
      final List<? extends TypeParameterElement> typeParameters = e.getTypeParameters();
      if (typeParameters.isEmpty()) {
        yield e.getQualifiedName();
      }
      final List<? extends TypeMirror> typeArguments = dt.getTypeArguments();
      if (typeArguments.isEmpty()) {
        yield e.getQualifiedName();
      }
      final StringJoiner sj = new StringJoiner(", ");
      for (final TypeMirror typeArgument : typeArguments) {
        sj.add(name(typeArgument));
      }
      yield e.getQualifiedName() + "<" + sj.toString() + ">";
    }
    case DOUBLE -> "double";
    case FLOAT -> "float";
    case INT -> "int";
    case INTERSECTION -> {
      final List<? extends TypeMirror> bounds = ((IntersectionType)t).getBounds();
      if (bounds.size() <= 1) {
        throw new IllegalArgumentException("t: " + t);
      }
      final List<TypeMirror> sortedBounds = new ArrayList<>(bounds);
      // The bounds of an IntersectionType will always be DeclaredTypes. So all we have to do is put the non-interface
      // ones first.
      //
      // TODO: technically we don't have to do this since this is just to get deterministic order based on names.
      Collections.sort(sortedBounds,
                       new TestingTypeMirrorComparator(type ->
                                                       type.getKind() == TypeKind.DECLARED &&
                                                       !((DeclaredType)type).asElement().getKind().isInterface())
                       .thenComparing(this));
      final StringJoiner sj = new StringJoiner(" & ");
      for (final TypeMirror bound : sortedBounds) {
        sj.add(name(bound));
      }
      yield sj.toString();
    }
    case LONG -> "long";
    case SHORT -> "short";
    case TYPEVAR -> {
      final TypeVariable tv = (TypeVariable)t;
      final TypeMirror bound = tv.getUpperBound();
      yield
        bound == null || bound.getKind() == TypeKind.NONE ? tv.asElement().getSimpleName() + " extends java.lang.Object" :
        tv.asElement().getSimpleName() + " extends " + name(bound);
    }
    case VOID -> "void";
    case WILDCARD -> {
      final WildcardType w = (WildcardType)t;
      final TypeMirror extendsBound = w.getExtendsBound();
      final TypeMirror superBound = w.getSuperBound();
      if (superBound == null) {
        if (extendsBound == null) {
          yield "? extends java.lang.Object"; // we could have just said "?" but this handles the fact that they're interchangeable
        }
        yield "? extends " + name(extendsBound);
      } else if (extendsBound == null) {
        yield "? super " + name(superBound);
      }
      throw new IllegalArgumentException("t: " + t);
    }
    case ERROR -> throw new AssertionError("t.getKind() == TypeKind.ERROR; t: " + t);
    default -> throw new IllegalArgumentException("t: " + t);
    };
  }

}
