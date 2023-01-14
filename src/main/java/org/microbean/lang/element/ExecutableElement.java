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
package org.microbean.lang.element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public final class ExecutableElement extends Parameterizable implements javax.lang.model.element.ExecutableElement {

  private final List<VariableElement> parameters;

  private final List<VariableElement> unmodifiableParameters;

  private Boolean isDefault;

  private Boolean varArgs;

  private AnnotationValue defaultValue;

  public ExecutableElement(final ElementKind kind) {
    super(kind);
    this.parameters = new ArrayList<>(7);
    this.unmodifiableParameters = Collections.unmodifiableList(this.parameters);
    if (kind == ElementKind.CONSTRUCTOR) {
      this.isDefault = false;
      this.setSimpleName("<init>");
    }
  }
  
  public ExecutableElement(final boolean varArgs,
                           final boolean isDefault,
                           final AnnotationValue defaultValue) {
    this(ElementKind.METHOD);
    this.setDefault(isDefault);
    this.setDefaultValue(defaultValue);
  }
  
  public ExecutableElement(final ElementKind kind,
                           final boolean varArgs,
                           final boolean isDefault,
                           final AnnotationValue defaultValue) {
    this(kind);
    this.setVarArgs(varArgs);
    this.setDefault(isDefault);
    this.setDefaultValue(defaultValue);
  }

  @Override // Element
  public final <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    return v.visitExecutable(this, p);
  }

  @Override // Element
  protected final TypeMirror validateType(final TypeMirror type) {
    if (type.getKind() == TypeKind.EXECUTABLE && type instanceof ExecutableType) {
      return type;
    }
    throw new IllegalArgumentException("type: " + type);
  }

  @Override // ExecutableElement
  public final boolean isDefault() {
    final Boolean isDefault = this.isDefault;
    return isDefault == null ? false : isDefault;
  }

  public final void setDefault(final boolean isDefault) {
    final Boolean old = this.isDefault;
    if (old == null) {
      this.isDefault = Boolean.valueOf(isDefault);
    } else if (!old.booleanValue() == isDefault) {
      throw new IllegalStateException();
    }
  }

  @Override // ExecutableElement
  public final boolean isVarArgs() {
    final Boolean varArgs = this.varArgs;
    return varArgs == null ? false : varArgs;
  }

  public final void setVarArgs(final boolean varArgs) {
    final Boolean old = this.varArgs;
    if (old == null) {
      this.varArgs = Boolean.valueOf(varArgs);
    } else if (!old.booleanValue() == varArgs) {
      throw new IllegalStateException();
    }
  }

  @Override // ExecutableElement
  public final AnnotationValue getDefaultValue() {
    return this.defaultValue;
  }

  public final void setDefaultValue(final AnnotationValue defaultValue) {
    final Object old = this.getDefaultValue();
    if (old == null) {
      if (defaultValue != null) {
        this.defaultValue = validateDefaultValue(defaultValue);
      }
    } else if (old != defaultValue) {
      throw new IllegalStateException();
    }
  }

  @Override // ExecutableElement
  public final List<? extends VariableElement> getParameters() {
    return this.unmodifiableParameters;
  }

  public void addParameter(final VariableElement p) {
    this.parameters.add(this.validateParameter(p));
  }

  public void addParameters(final Iterable<? extends VariableElement> ps) {
    for (final VariableElement p : ps) {
      this.addParameter(p);
    }
  }

  private final VariableElement validateParameter(final VariableElement p) {
    return Objects.requireNonNull(p, "p");
  }

  @Override // ExecutableElement
  public final List<? extends TypeMirror> getThrownTypes() {
    return ((ExecutableType)this.asType()).getThrownTypes();
  }

  @Override // ExecutableElement
  public final TypeMirror getReceiverType() {
    return ((ExecutableType)this.asType()).getReceiverType();
  }

  @Override // ExecutableElement
  public final TypeMirror getReturnType() {
    return ((ExecutableType)this.asType()).getReturnType();
  }

  @Override
  public final String toString() {
    final StringBuilder sb = new StringBuilder();
    final List<? extends TypeParameterElement> typeParameterElements = this.getTypeParameters();
    if (!typeParameterElements.isEmpty()) {
      sb.append('<');
      final Iterator<? extends TypeParameterElement> i = typeParameterElements.iterator();
      while (i.hasNext()) {
        sb.append(i.next());
        if (i.hasNext()) {
          sb.append(", ");
        }
      }
      sb.append('>');
    }
    final Name name = this.getSimpleName();
    if (name.contentEquals("<init>")) {
      sb.append(this.getEnclosingElement().getSimpleName());
    } else {
      sb.append(name);
    }
    final List<? extends VariableElement> parameters = this.getParameters();
    if (!parameters.isEmpty()) {
      sb.append('(');
      final Iterator<? extends VariableElement> i = parameters.iterator();
      while (i.hasNext()) {
        sb.append(i.next().asType());
        if (i.hasNext()) {
          sb.append(',');
        }
      }
      sb.append(')');
    }
    return sb.toString();
  }

  @Override // Element
  protected final Modifier validateModifier(final Modifier m) {
    if (Objects.requireNonNull(m, "m") == Modifier.NON_SEALED ||
        m == Modifier.SEALED ||
        m == Modifier.STRICTFP ||
        m == Modifier.TRANSIENT ||
        m == Modifier.VOLATILE) {
      throw new IllegalArgumentException("m: " + m);
    }
    return m;
  }

  @Override // Element
  protected final ElementKind validateKind(final ElementKind kind) {
    switch (kind) {
    case CONSTRUCTOR:
    case INSTANCE_INIT:
    case METHOD:
    case STATIC_INIT:
      return kind;
    default:
      throw new IllegalArgumentException("kind: " + kind);
    }
  }

  @Override // Element
  protected final Name validateSimpleName(final Name name) {
    switch (this.getKind()) {
    case CONSTRUCTOR:
      if (!name.contentEquals("<init>")) {
        throw new IllegalArgumentException("name: " + name);
      }
      break;
    case STATIC_INIT:
      if (!name.contentEquals("<clinit>")) {
        throw new IllegalArgumentException("name: " + name);
      }
      break;
    case INSTANCE_INIT:
      if (!name.isEmpty()) {
        throw new IllegalArgumentException("name: " + name);
      }
      break;
    case METHOD:
      if (name.isEmpty() || name.contentEquals("<init>") || name.contentEquals("<clinit>")) {
        throw new IllegalArgumentException("name: " + name);
      }
      break;
    default:
      throw new AssertionError();
    }
    return name;
  }

  private static final AnnotationValue validateDefaultValue(final AnnotationValue defaultValue) {
    return defaultValue;
  }

}
