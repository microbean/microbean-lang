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

import java.util.Objects;

import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import javax.lang.model.util.SimpleTypeVisitor14;

import org.microbean.lang.TypeAndElementSource;

// A mostly slavish port of
// https://github.com/openjdk/jdk/blob/jdk-21%2B22/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L2746-L2780.
// I *believe* this will take something like T extends List<T> and will yield List<T> (which is not the same as erasure
// of a type variable).
public final class BoundingClassVisitor extends SimpleTypeVisitor14<TypeMirror, Void> {

  private final TypeAndElementSource tes;

  private final SupertypeVisitor supertypeVisitor;

  public BoundingClassVisitor(final TypeAndElementSource tes, final SupertypeVisitor supertypeVisitor) {
    super();
    this.tes = Objects.requireNonNull(tes, "tes");
    this.supertypeVisitor = Objects.requireNonNull(supertypeVisitor, "supertypeVisitor");
  }

  @Override // SimpleTypeVisitor14
  protected final TypeMirror defaultAction(final TypeMirror t, final Void x) {
    return t;
  }

  @Override
  public final DeclaredType visitDeclared(final DeclaredType t, final Void x) {
    assert t.getKind() == TypeKind.DECLARED;
    final TypeMirror enclosingType = t.getEnclosingType();
    final TypeMirror visitedEnclosingType = this.visit(enclosingType);
    if (enclosingType == visitedEnclosingType) {
      return t;
    }
    assert visitedEnclosingType.getKind() == TypeKind.DECLARED;
    return tes.declaredType((DeclaredType)visitedEnclosingType, (TypeElement)t.asElement(), t.getTypeArguments().toArray(new TypeMirror[0]));
    /*
    final org.microbean.lang.type.DeclaredType dt = new org.microbean.lang.type.DeclaredType();
    dt.addTypeArguments(t.getTypeArguments());
    dt.addAnnotationMirrors(t.getAnnotationMirrors());
    dt.setDefiningElement((TypeElement)t.asElement());
    dt.setEnclosingType(visitedEnclosingType); // note
    return dt;
    */
  }

  @Override
  public final TypeMirror visitTypeVariable(final TypeVariable t, final Void x) {
    assert t.getKind() == TypeKind.TYPEVAR;
    // Recall that the supertype of a type variable is its "leftmost bound". So:
    // Given T extends S, yield S.
    // Given T extends List<String>, yield List<String> (so this is not erasure).
    return this.visit(this.supertypeVisitor.visit(t));
  }

}
