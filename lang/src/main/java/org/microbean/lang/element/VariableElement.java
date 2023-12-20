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
package org.microbean.lang.element;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;

import javax.lang.model.type.TypeMirror;

public final class VariableElement extends Element implements javax.lang.model.element.VariableElement {

  public Object constantValue;

  public VariableElement(final ElementKind kind) {
    super(kind);
  }

  public VariableElement(final ElementKind kind, final Object constantValue) {
    super(kind);
    if (constantValue != null) {
      this.setConstantValue(constantValue);
    }
  }

  @Override // Element
  public final <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    return v.visitVariable(this, p);
  }

  @Override // Element
  public final boolean isUnnamed() {
    return false;
  }

  @Override // VariableElement
  public final Object getConstantValue() {
    return this.constantValue;
  }

  public final void setConstantValue(final Object constantValue) {
    final Object old = this.getConstantValue();
    if (old == null) {
      if (constantValue != null) {
        this.constantValue = validateConstantValue(constantValue);
      }
    } else if (old != constantValue) {
      throw new IllegalStateException();
    }
  }

  @Override
  public final String toString() {
    return this.getSimpleName().toString();
  }

  @Override
  protected final ElementKind validateKind(final ElementKind kind) {
    switch (kind) {
    case BINDING_VARIABLE:
    case ENUM:
    case ENUM_CONSTANT:
    case EXCEPTION_PARAMETER:
    case FIELD:
    case LOCAL_VARIABLE:
    case PARAMETER:
    case RESOURCE_VARIABLE:
      return kind;
    default:
      throw new IllegalArgumentException("kind: " + kind);
    }
  }

  @Override
  protected final TypeMirror validateType(final TypeMirror type) {
    switch (type.getKind()) {
    case ARRAY:
    case DECLARED:
    case INTERSECTION:
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
      return type;
    default:
      throw new IllegalArgumentException("type: " + type);
    }
  }

  private final Object validateConstantValue(final Object constantValue) {
    if (constantValue == this) {
      throw new IllegalArgumentException("constantValue: " + constantValue);
    }
    return constantValue;
  }

}
