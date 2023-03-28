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

import javax.lang.model.element.Element;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import javax.lang.model.util.SimpleTypeVisitor14;

import org.microbean.lang.ElementSource;
import org.microbean.lang.Equality;

import org.microbean.lang.type.Types;

import static org.microbean.lang.type.Types.allTypeArguments;
import static org.microbean.lang.type.Types.isStatic;

// Basically done
// https://github.com/openjdk/jdk/blob/jdk-20+13/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L2294-L2340
public final class MemberTypeVisitor extends SimpleTypeVisitor14<TypeMirror, Element> {

  private final ElementSource elementSource;
  
  private final Equality equality;

  private final Types types;

  private final AsSuperVisitor asSuperVisitor;

  private final EraseVisitor eraseVisitor;

  private final SupertypeVisitor supertypeVisitor;

  public MemberTypeVisitor(final ElementSource elementSource,
                           final Equality equality,
                           final Types types,
                           final AsSuperVisitor asSuperVisitor,
                           final EraseVisitor eraseVisitor,
                           final SupertypeVisitor supertypeVisitor) {
    super();
    this.elementSource = Objects.requireNonNull(elementSource, "elementSource");
    this.equality = equality == null ? new Equality(true) : equality;
    this.types = Objects.requireNonNull(types, "types");
    this.asSuperVisitor = Objects.requireNonNull(asSuperVisitor, "asSuperVisitor");
    this.eraseVisitor = Objects.requireNonNull(eraseVisitor, "eraseVisitor");
    this.supertypeVisitor = Objects.requireNonNull(supertypeVisitor, "supertypeVisitor");
  }

  @Override
  protected final TypeMirror defaultAction(final TypeMirror t, final Element e) {
    return e.asType();
  }

  @Override
  public final TypeMirror visitDeclared(final DeclaredType t, final Element e) {
    assert t.getKind() == TypeKind.DECLARED;
    return this.visitDeclaredOrIntersection(t, e);
  }

  private final TypeMirror visitDeclaredOrIntersection(final TypeMirror t, final Element e) {
    assert t.getKind() == TypeKind.DECLARED || t.getKind() == TypeKind.INTERSECTION;
    if (!isStatic(e)) {
      final Element enclosingElement = e.getEnclosingElement();
      final TypeMirror enclosingType = enclosingElement.asType();
      final List<? extends TypeMirror> enclosingTypeTypeArguments = allTypeArguments(enclosingType);
      if (!enclosingTypeTypeArguments.isEmpty()) {
        assert enclosingType.getKind() == TypeKind.DECLARED;
        assert enclosingType instanceof DeclaredType;
        final TypeMirror baseType = this.asSuperVisitor.asOuterSuper(t, enclosingElement);
        if (baseType != null) {
          final List<? extends TypeMirror> baseTypeTypeArguments = allTypeArguments(baseType);
          if (baseTypeTypeArguments.isEmpty()) {
            // baseType is raw
            return this.eraseVisitor.visit(e.asType());
          } else {
            return new SubstituteVisitor(this.elementSource,
                                         this.equality,
                                         this.supertypeVisitor,
                                         enclosingTypeTypeArguments,
                                         baseTypeTypeArguments)
              .visit(e.asType());
          }
        }
      }
    }
    return e.asType();
  }

  @Override
  public final TypeMirror visitError(final ErrorType t, final Element e) {
    assert t.getKind() == TypeKind.ERROR;
    return t;
  }

  @Override
  public final TypeMirror visitIntersection(final IntersectionType t, final Element e) {
    assert t.getKind() == TypeKind.INTERSECTION;
    return this.visitDeclaredOrIntersection(t, e);
  }

  @Override
  public final TypeMirror visitTypeVariable(final TypeVariable t, final Element e) {
    assert t.getKind() == TypeKind.TYPEVAR;
    return this.visit(t.getUpperBound(), e);
  }

  @Override
  public final TypeMirror visitWildcard(final WildcardType t, final Element e) {
    assert t.getKind() == TypeKind.WILDCARD;
    return this.visit(this.types.extendsBound(t), e);
  }

}
