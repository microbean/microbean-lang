/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2022 microBean™.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.lang.model.element.AnnotationMirror;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.TypeVariable;

public final class ExecutableType extends TypeMirror implements javax.lang.model.type.ExecutableType {

  private final List<javax.lang.model.type.TypeMirror> parameterTypes;

  private final List<javax.lang.model.type.TypeMirror> unmodifiableParameterTypes;

  private javax.lang.model.type.TypeMirror receiverType;

  private javax.lang.model.type.TypeMirror returnType;

  private final List<javax.lang.model.type.TypeMirror> thrownTypes;

  private final List<javax.lang.model.type.TypeMirror> unmodifiableThrownTypes;

  private final List<TypeVariable> typeVariables;

  private final List<TypeVariable> unmodifiableTypeVariables;

  public ExecutableType() {
    super(TypeKind.EXECUTABLE);
    this.parameterTypes = new ArrayList<>(5);
    this.unmodifiableParameterTypes = Collections.unmodifiableList(this.parameterTypes);
    this.thrownTypes = new ArrayList<>(5);
    this.unmodifiableThrownTypes = Collections.unmodifiableList(this.thrownTypes);
    this.typeVariables = new ArrayList<>(5);
    this.unmodifiableTypeVariables = Collections.unmodifiableList(this.typeVariables);
  }

  public ExecutableType(javax.lang.model.type.ExecutableType t) {
    this(t, t.getTypeVariables());
  }

  public ExecutableType(javax.lang.model.type.ExecutableType t, final Iterable<? extends javax.lang.model.type.TypeVariable> typeVariables) {
    this();
    if (t.getKind() != TypeKind.EXECUTABLE) {
      throw new IllegalArgumentException();
    }
    this.addParameterTypes(t.getParameterTypes());
    this.setReceiverType(t.getReceiverType());
    this.setReturnType(t.getReturnType());
    this.addThrownTypes(t.getThrownTypes());
    this.addTypeVariables(typeVariables);
  }

  @Override
  public final void addAnnotationMirror(final AnnotationMirror a) {
    throw new UnsupportedOperationException();
  }

  @Override // AnnotatedConstruct
  public final List<? extends AnnotationMirror> getAnnotationMirrors() {
    return List.of();
  }

  @Override // TypeMirror
  public final <R, P> R accept(final TypeVisitor<R, P> v, P p) {
    return v.visitExecutable(this, p);
  }

  @Override // ExecutableType
  public final List<? extends javax.lang.model.type.TypeMirror> getParameterTypes() {
    return this.unmodifiableParameterTypes;
  }

  public final void addParameterType(final javax.lang.model.type.TypeMirror t) {
    this.parameterTypes.add(validateParameterType(t));
  }

  public final void addParameterTypes(final Iterable<? extends javax.lang.model.type.TypeMirror> ts) {
    for (final javax.lang.model.type.TypeMirror t : ts) {
      this.addParameterType(t);
    }
  }

  @Override // ExecutableType
  public final List<? extends javax.lang.model.type.TypeMirror> getThrownTypes() {
    return this.unmodifiableThrownTypes;
  }

  public final void addThrownType(final javax.lang.model.type.TypeMirror t) {
    this.thrownTypes.add(validateThrownType(t));
  }

  public final void addThrownTypes(final Iterable<? extends javax.lang.model.type.TypeMirror> ts) {
    for (final javax.lang.model.type.TypeMirror t : ts) {
      this.addThrownType(t);
    }
  }

  @Override // ExecutableType
  public final List<? extends TypeVariable> getTypeVariables() {
    return this.unmodifiableTypeVariables;
  }

  public final void addTypeVariable(final TypeVariable t) {
    this.typeVariables.add(validateTypeVariable(t));
  }

  public final void addTypeVariables(final Iterable<? extends javax.lang.model.type.TypeVariable> ts) {
    for (final javax.lang.model.type.TypeVariable t : ts) {
      this.addTypeVariable(t);
    }
  }

  @Override // ExecutableType
  public final javax.lang.model.type.TypeMirror getReceiverType() {
    final javax.lang.model.type.TypeMirror t = this.receiverType;
    return t == null ? NoType.NONE : t;
  }

  public final void setReceiverType(final javax.lang.model.type.TypeMirror t) {
    final Object old = this.receiverType;
    if (old == null) {
      if (t != null) {
        this.receiverType = validateReceiverType(t);
      }
    } else if (old != t) {
      throw new IllegalStateException();
    }
  }

  @Override // ExecutableType
  public final javax.lang.model.type.TypeMirror getReturnType() {
    final javax.lang.model.type.TypeMirror t = this.returnType;
    return t == null ? NoType.VOID : t;
  }

  public final void setReturnType(final javax.lang.model.type.TypeMirror t) {
    final Object old = this.returnType;
    if (old == null) {
      if (t != null) {
        this.returnType = validateReturnType(t);
      }
    } else if (old != t) {
      throw new IllegalStateException();
    }
  }

  private final javax.lang.model.type.TypeMirror validateParameterType(final javax.lang.model.type.TypeMirror t) {
    if (t == this) {
      throw new IllegalArgumentException("t: " + t);
    }
    return Objects.requireNonNull(t, "t");
  }

  private final javax.lang.model.type.TypeMirror validateReturnType(final javax.lang.model.type.TypeMirror t) {
    if (t == this) {
      throw new IllegalArgumentException("t: " + t);
    }
    return t == null ? NoType.VOID : t;
  }

  private final javax.lang.model.type.TypeMirror validateReceiverType(final javax.lang.model.type.TypeMirror t) {
    if (t == this) {
      throw new IllegalArgumentException("t: " + t);
    }
    return t == null ? NoType.NONE : t;
  }

  private final javax.lang.model.type.TypeMirror validateThrownType(final javax.lang.model.type.TypeMirror t) {
    if (t == this) {
      throw new IllegalArgumentException("t: " + t);
    }
    return Objects.requireNonNull(t, "t");
  }

  private static final TypeVariable validateTypeVariable(final TypeVariable t) {
    return Objects.requireNonNull(t, "t");
  }

}
