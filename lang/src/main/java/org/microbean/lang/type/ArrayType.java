/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2022–2023 microBean™.
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
package org.microbean.lang.type;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;

/**
 * A mutable implementation of the {@link javax.lang.model.type.ArrayType} interface.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
public final class ArrayType extends TypeMirror implements javax.lang.model.type.ArrayType {

  private javax.lang.model.type.TypeMirror componentType;

  /**
   * Creates a new {@link ArrayType}.
   *
   * <p>The return value of the {@link #getComponentType()} method will be {@code null} until {@link
   * #setComponentType(javax.lang.model.type.TypeMirror)} has been called with a valid argument.</p>
   *
   * @see #getComponentType()
   *
   * @see #setComponentType(javax.lang.model.type.TypeMirror)
   */
  public ArrayType() {
    super(TypeKind.ARRAY);
  }

  /**
   * Creates a new {@link ArrayType}.
   *
   * @param componentType a {@link javax.lang.model.type.TypeMirror} that will be returned by the {@link
   * #getComponentType()} method; may be {@code null}
   *
   * @see #getComponentType()
   *
   * @see #setComponentType(javax.lang.model.type.TypeMirror)
   */
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

  /**
   * Installs the {@link javax.lang.model.type.TypeMirror} to be returned by the {@link #getComponentType()} method.
   *
   * @param componentType the component type; may be {@code null} but only if the current value returned by the {@link
   * #getComponentType()} method is {@code null}; once a component type has been installed, it may not be changed to any
   * other value
   *
   * @exception IllegalStateException if the supplied {@link componentType} is different from the already installed type
   */
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

  @Override // Object
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
