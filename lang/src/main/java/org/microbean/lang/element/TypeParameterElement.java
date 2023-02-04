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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;

import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import org.microbean.lang.type.DefineableType;
import org.microbean.lang.type.Types;

public final class TypeParameterElement extends Element implements javax.lang.model.element.TypeParameterElement {

  private javax.lang.model.element.Element genericElement;

  public TypeParameterElement() {
    super(ElementKind.TYPE_PARAMETER);
  }

  public <T extends DefineableType<? super javax.lang.model.element.TypeParameterElement> & TypeVariable>
    TypeParameterElement(final javax.lang.model.element.Name simpleName, final T typeVariable) {
    this();
    this.setSimpleName(simpleName);
    typeVariable.setDefiningElement(this);
  }

  @Override // Element
  public final <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    return v.visitTypeParameter(this, p);
  }

  @Override // Element
  protected final TypeMirror validateType(final TypeMirror type) {
    if (type.getKind() == TypeKind.TYPEVAR && type instanceof TypeVariable) {
      return type;
    }
    throw new IllegalArgumentException("type: " + type);
  }

  @Override // Element
  public final boolean isUnnamed() {
    return false;
  }

  @Override // TypeParameterElement
  public final List<? extends TypeMirror> getBounds() {
    return boundsFrom((TypeVariable)this.asType());
  }

  @Override // Element
  public final javax.lang.model.element.Element getEnclosingElement() {
    return this.getGenericElement();
  }

  @Override // Element
  public final void setEnclosingElement(final javax.lang.model.element.Element genericElement) {
    this.setGenericElement(genericElement);
  }

  @Override // TypeParameterElement
  public final javax.lang.model.element.Element getGenericElement() {
    return this.genericElement;
  }

  public final void setGenericElement(final javax.lang.model.element.Element genericElement) {
    final Object old = this.getGenericElement();
    if (old == null) {
      if (genericElement != null) {
        this.genericElement = validateGenericElement(genericElement);
      }
    } else if (old != genericElement) {
      throw new IllegalStateException("old: " + old + "; genericElement: " + genericElement);
    }
  }

  @Override
  public final String toString() {
    return this.getSimpleName() + " " + this.asType();
  }

  private final javax.lang.model.element.Element validateGenericElement(final javax.lang.model.element.Element element) {
    if (element == null) {
      return null;
    } else if (element == this) {
      throw new IllegalArgumentException("element: " + element);
    }
    switch (element.getKind()) {
    case CLASS:
    case CONSTRUCTOR:
    case INTERFACE:
    case METHOD:
      return element;
    default:
      throw new IllegalArgumentException("Not a valid generic element: " + element);
    }
  }

  
  /*
   * Static methods.
   */


  private static final TypeVariable validateTypeVariable(final TypeVariable tv) {
    switch (tv.getKind()) {
    case TYPEVAR:
      return tv;
    default:
      throw new IllegalArgumentException("tv: " + tv);
    }
  }

  private static final List<? extends TypeMirror> boundsFrom(final TypeVariable typeVariable) {
    final TypeMirror upperBound = typeVariable.getUpperBound();
    switch (upperBound.getKind()) {
    case INTERSECTION:
      return ((IntersectionType)upperBound).getBounds();
    case ARRAY:
    case DECLARED:
    case TYPEVAR:
      return List.of(upperBound);
    default:
      throw new IllegalArgumentException("typeVariable: " + typeVariable);
    }
  }

}
