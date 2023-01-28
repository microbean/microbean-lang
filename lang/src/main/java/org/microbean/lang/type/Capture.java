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

import javax.lang.model.element.ElementKind;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;

import org.microbean.lang.element.Name;
import org.microbean.lang.element.TypeParameterElement;

public final class Capture extends DefineableType<javax.lang.model.element.TypeParameterElement> implements TypeVariable {

  private TypeMirror upperBound;

  private TypeMirror lowerBound;

  private final WildcardType wildcardType;

  public Capture(final WildcardType w) {
    this(Name.of("<captured wildcard>"), w);
  }

  public Capture(final javax.lang.model.element.Name name, final WildcardType w) {
    super(TypeKind.TYPEVAR);
    this.wildcardType = validateWildcardType(w);
    this.setDefiningElement(new TypeParameterElement(name, this));
  }

  @Override // DefineableType<TypeParameterElement>
  protected final javax.lang.model.element.TypeParameterElement validateDefiningElement(final javax.lang.model.element.TypeParameterElement e) {
    if (e.getKind() != ElementKind.TYPE_PARAMETER) {
      throw new IllegalArgumentException("e: " + e);
    } else if (this != e.asType()) {
      throw new IllegalArgumentException("e: " + e + "; this (" + this + ") != e.asType() (" + e.asType() + ")");
    }
    return e;
  }

  @Override // TypeVariable
  public final <R, P> R accept(final TypeVisitor<R, P> v, final P p) {
    return v.visitTypeVariable(this, p);
  }

  @Override
  public final TypeMirror getLowerBound() {
    return this.lowerBound;
  }

  public final void setLowerBound(final TypeMirror t) {
    final Object old = this.getLowerBound();
    if (old == null) {
      if (t != null) {
        this.lowerBound = validateLowerBound(t);
      }
    } else if (old != t) {
      throw new IllegalStateException();
    }
  }

  @Override // TypeVariable
  public final TypeMirror getUpperBound() {
    return this.upperBound;
  }

  public final void setUpperBound(final TypeMirror t) {
    final Object old = this.getUpperBound();
    if (old == null) {
      if (t != null) {
        this.upperBound = validateUpperBound(t);
      }
    } else if (old != t) {
      throw new IllegalStateException();
    }
  }

  public final WildcardType getWildcardType() {
    return this.wildcardType;
  }


  /*
   * Static methods.
   */


  private static final TypeMirror validateUpperBound(final TypeMirror upperBound) {
    switch (upperBound.getKind()) {
    case DECLARED:
    case INTERSECTION:
    case TYPEVAR:
      return upperBound;
    default:
      throw new IllegalArgumentException("upperBound: " + upperBound);
    }
  }

  private static final TypeMirror validateLowerBound(final TypeMirror lowerBound) {
    return lowerBound;
  }

  private static final WildcardType validateWildcardType(final WildcardType w) {
    if (w != null) {
      switch (w.getKind()) {
      case WILDCARD:
        break;
      default:
        throw new IllegalArgumentException("w: " + w);
      }
    }
    return w;
  }

}
