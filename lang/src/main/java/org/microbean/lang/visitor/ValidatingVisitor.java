/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2023–2024 microBean™.
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
package org.microbean.lang.visitor;

import java.util.Collection;
import java.util.Objects;

import javax.lang.model.element.Element;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;

import org.microbean.lang.TypeAndElementSource;

public final class ValidatingVisitor extends StructuralTypeMapping<Void> {

  public ValidatingVisitor(final TypeAndElementSource tes) {
    super(tes);
  }

  @Override // SimpleTypeVisitor6
  public TypeMirror visitArray(final ArrayType t, final Void x) {
    switch (t.getKind()) {
    case ARRAY:
      break;
    default:
      throw new IllegalArgumentException("t: " + t);
    }
    final TypeMirror componentType = t.getComponentType();
    if (componentType == null || componentType == t) {
      throw new IllegalArgumentException("t: " + t);
    }
    switch (componentType.getKind()) {
    case ARRAY:
    case BOOLEAN:
    case BYTE:
    case CHAR:
    case DECLARED:
    case DOUBLE:
    case FLOAT:
    case INT:
    case LONG:
    case SHORT:
    case TYPEVAR:
      break;
    default:
      throw new IllegalArgumentException("t: " + t);
    }
    return super.visitArray(t, x);
  }

  @Override // SimpleTypeVisitor6
  public TypeMirror visitDeclared(final DeclaredType t, final Void x) {
    final TypeMirror et = t.getEnclosingType();
    if (et == null || et == t) {
      throw new IllegalArgumentException("t: " + t);
    }
    final Iterable<? extends TypeMirror> typeArguments = t.getTypeArguments();
    if (typeArguments == null) {
      throw new IllegalArgumentException("t: " + t);
    }
    for (final TypeMirror typeArgument : typeArguments) {
      if (typeArgument == null || typeArgument == t) {
        throw new IllegalArgumentException("t: " + t);
      }
      switch (typeArgument.getKind()) {
      case DECLARED:
      case TYPEVAR:
      case WILDCARD: // ...right?
        break;
      default:
        throw new IllegalArgumentException("t: " + t);
      }
    }
    final Element e = t.asElement();
    if (e == null) {
      throw new IllegalArgumentException("t: " + t);
    }
    switch (e.getKind()) {
    case ANNOTATION_TYPE:
    case CLASS:
    case ENUM:
    case INTERFACE:
      switch (t.getKind()) {
      case DECLARED:
        break;
      default:
        throw new IllegalArgumentException("t: " + t);
      }
      break;
    default:
      throw new IllegalArgumentException("t: " + t);
    }
    return super.visitDeclared(t, x);
  }

  @Override // SimpleTypeVisitor6
  public TypeMirror visitExecutable(final ExecutableType t, final Void x) {
    final Iterable<? extends TypeMirror> pts = t.getParameterTypes();
    if (pts == null) {
      throw new IllegalArgumentException("t: " + t);
    }
    for (final TypeMirror pt : pts) {
      switch (pt.getKind()) {
      case ARRAY:
      case BOOLEAN:
      case BYTE:
      case CHAR:
      case DECLARED:
      case DOUBLE:
      case FLOAT:
      case INT:
      case LONG:
      case SHORT:
      case TYPEVAR:
        break;
      default:
        throw new IllegalArgumentException("t: " + t);
      }
    }
    final TypeMirror receiverType = t.getReceiverType();
    if (receiverType == null) {
      throw new IllegalArgumentException("t: " + t);
    }
    switch (receiverType.getKind()) {
    case DECLARED:
    case NONE:
      break;
    default:
      throw new IllegalArgumentException("t: " + t);
    }
    final TypeMirror returnType = t.getReturnType();
    if (returnType == null) {
      throw new IllegalArgumentException("t: " + t);
    }
    switch (returnType.getKind()) {
    case ARRAY:
    case BOOLEAN:
    case BYTE:
    case CHAR:
    case DECLARED:
    case DOUBLE:
    case FLOAT:
    case INT:
    case LONG:
    case SHORT:
    case TYPEVAR:
    case VOID:
      break;
    default:
      throw new IllegalArgumentException("t: " + t);
    }
    final Iterable<? extends TypeMirror> tts = t.getThrownTypes();
    if (tts == null) {
      throw new IllegalArgumentException("t: " + t);
    }
    for (final TypeMirror tt : tts) {
      switch (tt.getKind()) {
      case DECLARED:
      case TYPEVAR:
        break;
      default:
        throw new IllegalArgumentException("t: " + t);
      }
    }
    final Iterable<? extends TypeVariable> tvs = t.getTypeVariables();
    if (tvs == null) {
      throw new IllegalArgumentException("t: " + t);
    }
    for (final TypeVariable tv : tvs) {
      switch (tv.getKind()) {
      case TYPEVAR:
        break;
      default:
        throw new IllegalArgumentException("t: " + t);
      }
    }
    return super.visitExecutable(t, x);
  }

  @Override // SimpleTypeVisitor6
  public TypeMirror visitIntersection(final IntersectionType t, final Void x) {
    final Collection<? extends TypeMirror> bounds = t.getBounds();
    if (bounds == null || bounds.isEmpty()) {
      throw new IllegalArgumentException("t: " + t);
    }
    for (final TypeMirror bound : bounds) {
      if (bound == null) {
        throw new IllegalArgumentException("t: " + t);
      }
      switch (bound.getKind()) {
      case DECLARED:
      case TYPEVAR:
        break;
      default:
        throw new IllegalArgumentException("t: " + t);
      }
    }
    return super.visitIntersection(t, x);
  }

  @Override // SimpleTypeVisitor6
  public TypeMirror visitTypeVariable(final TypeVariable t, final Void x) {
    final Element e = t.asElement();
    if (e == null) {
      throw new IllegalArgumentException("t: " + t);
    }
    switch (e.getKind()) {
    case TYPE_PARAMETER:
      break;
    default:
      throw new IllegalArgumentException("t: " + t);
    }
    final TypeMirror lowerBound = t.getLowerBound();
    if (lowerBound == null) {
      throw new IllegalArgumentException("t: " + t);
    }
    switch (lowerBound.getKind()) {
    case DECLARED:
    case INTERSECTION:
    case NULL:
      break;
    default:
      throw new IllegalArgumentException("t: " + t);
    }
    final TypeMirror upperBound = t.getUpperBound();
    if (upperBound == null) {
      throw new IllegalArgumentException("t: " + t);
    }
    switch (upperBound.getKind()) {
    case DECLARED:
    case INTERSECTION:
    case TYPEVAR: // I guess? Should just erase to its bounds/intersection.
      break;
    default:
      throw new IllegalArgumentException("t: " + t);
    }
    return super.visitTypeVariable(t, x);
  }

  @Override // SimpleTypeVisitor6
  public TypeMirror visitUnion(final UnionType t, final Void x) {
    final Collection<? extends TypeMirror> alts = t.getAlternatives();
    if (alts == null || alts.isEmpty()) {
      throw new IllegalArgumentException("t: " + t);
    }
    for (final TypeMirror alt : alts) {
      if (alt == null) {
        throw new IllegalArgumentException("t: " + t);
      }
      switch (alt.getKind()) {
      case DECLARED:
      case TYPEVAR: // maybe?
        break;
      default:
        throw new IllegalArgumentException("t: " + t);
      }
    }
    return super.visitUnion(t, x);
  }

  @Override // SimpleTypeVisitor6
  public TypeMirror visitWildcard(final WildcardType t, final Void x) {
    final TypeMirror extendsBound = t.getExtendsBound();
    final TypeMirror superBound = t.getSuperBound();
    if (extendsBound == null) {
      if (superBound != null) {
        switch (superBound.getKind()) {
        case DECLARED:
        case TYPEVAR:
          break;
        default:
          throw new IllegalArgumentException("t: " + t);
        }
      }
    } else if (superBound == null) {
      if (extendsBound != null) {
        switch (extendsBound.getKind()) {
        case DECLARED:
        case TYPEVAR:
          break;
        default:
          throw new IllegalArgumentException("t: " + t);
        }
      }
    } else {
      throw new IllegalArgumentException("t: " + t);
    }
    return super.visitWildcard(t, x);
  }

  private static final boolean isReference(final TypeKind k) {
    return switch (Objects.requireNonNull(k, "k")) {
    case
      ARRAY,
      DECLARED,
      ERROR,
      INTERSECTION,
      TYPEVAR,
      WILDCARD -> true;
    case
      BOOLEAN,
      BYTE,
      CHAR,
      DOUBLE,
      EXECUTABLE,
      FLOAT,
      INT,
      LONG,
      MODULE,
      NONE,
      NULL,
      OTHER,
      PACKAGE,
      SHORT,
      UNION,
      VOID -> false;
    };
  }

}
