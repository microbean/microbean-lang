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
import java.util.List;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

public abstract sealed class Parameterizable extends Element implements javax.lang.model.element.Parameterizable
  permits ExecutableElement, TypeElement {

  private final List<TypeParameterElement> typeParameters;

  private final List<TypeParameterElement> unmodifiableTypeParameters;

  protected Parameterizable(ElementKind kind) {
    super(kind); // no need to validate; sealed class; subclasses already validate
    this.typeParameters = new ArrayList<>(5);
    this.unmodifiableTypeParameters = Collections.unmodifiableList(this.typeParameters);
  }

  @Override // Parameterizable
  public List<? extends TypeParameterElement> getTypeParameters() {
    return this.unmodifiableTypeParameters;
  }

  public final <TP extends TypeParameterElement & Encloseable> void addTypeParameter(final TP tp) {
    this.typeParameters.add(validateAndEncloseTypeParameter(tp));
  }

  public final <TP extends TypeParameterElement & Encloseable> void addTypeParameters(final Iterable<? extends TP> tps) {
    for (final TP tp : tps) {
      this.addTypeParameter(tp);
    }
  }

  private final <TP extends TypeParameterElement & Encloseable> TP validateAndEncloseTypeParameter(final TP tp) {
    switch (tp.getKind()) {
    case TYPE_PARAMETER:
      final TypeMirror t = tp.asType();
      switch (t.getKind()) {
      case TYPEVAR:
        tp.setEnclosingElement(this); // idempotent
        assert tp.getGenericElement() == this;
        assert tp.getEnclosingElement() == this;
        assert tp.asType() == t;        
        assert t instanceof TypeVariable tv ? tv.asElement() == tp : true;
        return tp;
      default:
        throw new IllegalArgumentException("typeParameter: " + tp + "; ((TypeVariable)tp.asType()).getKind(): " + t.getKind());
      }
    default:
      throw new IllegalArgumentException("typeParameter: " + tp);
    }
  }

  @Override // Element
  public final boolean isUnnamed() {
    return false;
  }


  /*
   * Static methods.
   */


  public static final List<? extends TypeMirror> typeArguments(final TypeMirror t) {
    switch (t.getKind()) {
    case DECLARED:
      return ((DeclaredType)t).getTypeArguments();
    case EXECUTABLE:
      return ((ExecutableType)t).getTypeVariables();
    default:
      return List.of();
    }
  }

}
