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

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;

public final class ArrayType extends TypeMirror implements javax.lang.model.type.ArrayType {

  private javax.lang.model.type.TypeMirror componentType;

  public ArrayType() {
    super(TypeKind.ARRAY);
  }

  public ArrayType(final javax.lang.model.type.TypeMirror componentType) {
    this();
    this.setComponentType(componentType);
  }


  /*
   * Instance methods.
   */


  @Override // TypeMirror
  public final <R, P> R accept(final TypeVisitor<R, P> v, final P p) {
    return v.visitArray(this, p);
  }

  @Override // ArrayType
  public final javax.lang.model.type.TypeMirror getComponentType() {
    return this.componentType;
  }

  public final void setComponentType(final javax.lang.model.type.TypeMirror componentType) {
    final javax.lang.model.type.TypeMirror old = this.getComponentType();
    if (old == null) {
      if (componentType != null) {
        this.componentType = this.validateComponentType(componentType);
      }
    } else if (old != componentType) {
      throw new IllegalStateException();
    }
  }

  public final String toString() {
    return this.componentType + "[]";
  }

  private final javax.lang.model.type.TypeMirror validateComponentType(final javax.lang.model.type.TypeMirror componentType) {
    if (componentType == this) {
      throw new IllegalArgumentException("componentType: " + componentType);
    }
    switch (componentType.getKind()) {
    case ARRAY:
    case DECLARED:
    // case INTERSECTION:
    case BOOLEAN:
    case BYTE:
    case CHAR:
    case DOUBLE:
    case FLOAT:
    case INT:
    case LONG:
    case SHORT:
    case TYPEVAR:
    case WILDCARD:
      return componentType;
    default:
      throw new IllegalArgumentException("componentType: " + componentType);
    }
  }

}
