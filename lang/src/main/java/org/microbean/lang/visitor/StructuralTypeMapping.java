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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;

import javax.lang.model.util.SimpleTypeVisitor14;

import org.microbean.lang.TypeAndElementSource;

// See https://github.com/openjdk/jdk/blob/jdk-20+11/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Type.java#L244
class StructuralTypeMapping<S> extends SimpleTypeVisitor14<TypeMirror, S> {

  protected final TypeAndElementSource elementSource;

  StructuralTypeMapping(final TypeAndElementSource elementSource) {
    super();
    this.elementSource = Objects.requireNonNull(elementSource, "elementSource");
  }

  @Override // SimpleTypeVisitor6
  protected TypeMirror defaultAction(final TypeMirror t, final S ignored) {
    return t;
  }

  public final List<? extends TypeMirror> visit(final List<? extends TypeMirror> ts, final S s) {
    if (ts.isEmpty()) {
      return ts;
    }
    final List<TypeMirror> list = new ArrayList<>(ts.size());
    boolean changed = false;
    for (final TypeMirror t : ts) {
      final TypeMirror visitedT = this.visit(t, s);
      list.add(visitedT);
      if (!changed && visitedT != t) {
        changed = true;
      }
    }
    if (changed) {
      return Collections.unmodifiableList(list);
    }
    return ts;
  }

  @Override // SimpleTypeVisitor6
  public TypeMirror visitArray(final ArrayType t, final S s) {
    assert t.getKind() == TypeKind.ARRAY;
    final TypeMirror componentType  = t.getComponentType();
    final TypeMirror visitedComponentType = this.visit(componentType, s);
    if (componentType == visitedComponentType) {
      return t;
    }
    return new org.microbean.lang.type.ArrayType(visitedComponentType);
  }

  @Override // SimpleTypeVisitor6
  public TypeMirror visitDeclared(final DeclaredType t, final S s) {
    assert t.getKind() == TypeKind.DECLARED;
    final TypeMirror enclosingType = t.getEnclosingType();
    final TypeMirror visitedEnclosingType = this.visit(enclosingType, s);
    final List<? extends TypeMirror> typeArguments = t.getTypeArguments();
    final List<? extends TypeMirror> visitedTypeArguments = this.visit(typeArguments, s);
    if (enclosingType == visitedEnclosingType && typeArguments == visitedTypeArguments) {
      return t;
    }
    final org.microbean.lang.type.DeclaredType r = new org.microbean.lang.type.DeclaredType();
    r.setEnclosingType(visitedEnclosingType);
    r.addTypeArguments(visitedTypeArguments);
    r.setDefiningElement((javax.lang.model.element.TypeElement)t.asElement());
    return r;
  }

  // See https://github.com/openjdk/jdk/blob/jdk-20+11/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Type.java#L290-L313
  @Override // SimpleTypeVisitor6
  public TypeMirror visitExecutable(final ExecutableType t, final S s) {
    assert t.getKind() == TypeKind.EXECUTABLE;
    // The compiler does not visit an executable type's receiver type or type variables.  We follow suit.
    final List<? extends TypeMirror> parameterTypes = t.getParameterTypes();
    final List<? extends TypeMirror> visitedParameterTypes = this.visit(parameterTypes, s);
    final TypeMirror returnType = t.getReturnType();
    final TypeMirror visitedReturnType = this.visit(returnType, s);
    final List<? extends TypeMirror> thrownTypes = t.getThrownTypes();
    final List<? extends TypeMirror> visitedThrownTypes = this.visit(thrownTypes, s);
    if (parameterTypes == visitedParameterTypes &&
        returnType == visitedReturnType &&
        thrownTypes == visitedThrownTypes) {
      return t;
    }
    final org.microbean.lang.type.ExecutableType r = new org.microbean.lang.type.ExecutableType();
    r.addParameterTypes(visitedParameterTypes);
    r.setReceiverType(t.getReceiverType());
    r.setReturnType(visitedReturnType);
    r.addThrownTypes(visitedThrownTypes);
    r.addTypeVariables(t.getTypeVariables());
    return r;
  }

  @Override // SimpleTypeVisitor6
  public TypeMirror visitWildcard(final WildcardType t, final S s) {
    assert t.getKind() == TypeKind.WILDCARD;
    TypeMirror extendsBound = t.getExtendsBound();
    final TypeMirror superBound = t.getSuperBound();
    final TypeMirror visitedExtendsBound;
    final TypeMirror visitedSuperBound;
    if (extendsBound == null) {
      if (superBound == null) {
        extendsBound = this.elementSource.typeElement("java.lang.Object").asType();
        visitedExtendsBound = this.visit(extendsBound, s);
        visitedSuperBound = null;
      } else {
        visitedExtendsBound = null;
        visitedSuperBound = this.visit(superBound, s);
      }
    } else if (superBound == null) {
      visitedExtendsBound = this.visit(extendsBound, s);
      visitedSuperBound = null;
    } else {
      throw new IllegalArgumentException("t: " + t);
    }
    if (extendsBound == visitedExtendsBound && superBound == visitedSuperBound) {
      return t;
    }
    return org.microbean.lang.type.WildcardType.of(visitedExtendsBound, visitedSuperBound);
  }

}
