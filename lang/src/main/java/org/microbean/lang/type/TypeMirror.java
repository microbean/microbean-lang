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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.lang.model.element.AnnotationMirror;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;

import org.microbean.lang.AnnotatedConstruct;

public abstract sealed class TypeMirror
  extends AnnotatedConstruct
  implements javax.lang.model.type.TypeMirror
  permits ArrayType,
          DefineableType,
          ExecutableType,
          IntersectionType,
          NoType,
          NullType,
          PrimitiveType,
          UnionType,
          WildcardType {

  private final javax.lang.model.type.TypeKind kind;


  /*
   * Constructors.
   */


  protected TypeMirror(final javax.lang.model.type.TypeKind kind) {
    super();
    this.kind = this.validateKind(kind);
  }


  /*
   * Instance methods.
   */


  @Override // TypeMirror
  public final javax.lang.model.type.TypeKind getKind() {
    return this.kind;
  }

  @Override // TypeMirror
  public <R, P> R accept(final TypeVisitor<R, P> v, P p) {
    switch (this.getKind()) {

    case ARRAY:
      return v.visitArray((javax.lang.model.type.ArrayType)this, p);

    case DECLARED:
      return v.visitDeclared((javax.lang.model.type.DeclaredType)this, p);

    case ERROR:
      return v.visitError((javax.lang.model.type.ErrorType)this, p);

    case EXECUTABLE:
      return v.visitExecutable((javax.lang.model.type.ExecutableType)this, p);

    case INTERSECTION:
      return v.visitIntersection((javax.lang.model.type.IntersectionType)this, p);

    case MODULE:
    case NONE:
    case PACKAGE:
    case VOID:
      return v.visitNoType((javax.lang.model.type.NoType)this, p);

    case NULL:
      return v.visitNull((javax.lang.model.type.NullType)this, p);

    case BOOLEAN:
    case BYTE:
    case CHAR:
    case DOUBLE:
    case FLOAT:
    case INT:
    case LONG:
    case SHORT:
      return v.visitPrimitive((javax.lang.model.type.PrimitiveType)this, p);

    case TYPEVAR:
      return v.visitTypeVariable((javax.lang.model.type.TypeVariable)this, p);

    case UNION:
      return v.visitUnion((javax.lang.model.type.UnionType)this, p);

    case WILDCARD:
      return v.visitWildcard((javax.lang.model.type.WildcardType)this, p);

    default:
      return v.visitUnknown(this, p);

    }
  }

  protected TypeKind validateKind(final TypeKind kind) {
    return Objects.requireNonNull(kind, "kind");
  }

}
