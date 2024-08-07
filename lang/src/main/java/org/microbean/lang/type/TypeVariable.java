/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2022–2024 microBean™.
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

import java.util.Objects;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;

import org.microbean.lang.TypeAndElementSource;

public non-sealed class TypeVariable extends DefineableType<TypeParameterElement> implements javax.lang.model.type.TypeVariable {

  private final TypeAndElementSource tes;

  private TypeMirror upperBound;

  private TypeMirror lowerBound;

  public TypeVariable(final TypeAndElementSource tes) {
    super(TypeKind.TYPEVAR);
    this.tes = Objects.requireNonNull(tes, "tes");
  }

  public TypeVariable(final TypeAndElementSource tes, final TypeMirror upperBound) {
    this(tes);
    this.setUpperBound(upperBound);
  }

  public TypeVariable(final TypeAndElementSource tes, final TypeMirror upperBound, final TypeMirror lowerBound) {
    this(tes);
    this.setUpperBound(upperBound);
    this.setLowerBound(lowerBound);
  }

  @Override // TypeVariable
  public final TypeMirror getLowerBound() {
    final TypeMirror t = this.lowerBound;
    return t == null ? NullType.INSTANCE : t; // this is correct; can't just return null
  }

  public final void setLowerBound(final TypeMirror lowerBound) {
    final Object old = this.lowerBound;
    if (old == null) {
      if (lowerBound != null) {
        this.lowerBound = validateLowerBound(lowerBound);
      }
    } else if (old != lowerBound) {
      throw new IllegalStateException();
    }
  }

  @Override // TypeVariable
  public final TypeMirror getUpperBound() {
    final TypeMirror t = this.upperBound;
    return t == null ? this.tes.typeElement("java.lang.Object").asType() : t; // this is correct; can't just return null
  }

  public final void setUpperBound(final TypeMirror upperBound) {
    final Object old = this.upperBound;
    if (old == null) {
      if (upperBound != null) {
        this.upperBound = validateUpperBound(upperBound);
      }
    } else if (old != upperBound) {
      throw new IllegalStateException();
    }
  }

  @Override // DefineableType<TypeParameterElement>
  protected TypeParameterElement validateDefiningElement(final TypeParameterElement e) {
    if (e.getKind() != ElementKind.TYPE_PARAMETER) {
      throw new IllegalArgumentException("e: " + e);
    }
    final Object t = e.asType();
    if (t != null && this != t) {
      throw new IllegalArgumentException("e: " + e + "; this (" + this + ") != e.asType() (" + t + ")");
    }
    return e;
  }

  @Override // TypeVariable
  public <R, P> R accept(final TypeVisitor<R, P> v, final P p) {
    return v.visitTypeVariable(this, p);
  }

  @Override // Object
  public String toString() {
    return super.toString() + " extends " + this.getUpperBound();
  }

  private final TypeMirror validateUpperBound(final TypeMirror upperBound) {
    return switch (upperBound.getKind()) {
    case DECLARED, INTERSECTION -> upperBound;
    case TYPEVAR -> {
      if (upperBound == this) {
        throw new IllegalArgumentException("upperBound: " + upperBound);
      }
      yield upperBound;
    }
    default -> throw new IllegalArgumentException("upperBound: " + upperBound);
    };
  }

  private final TypeMirror validateLowerBound(final TypeMirror lowerBound) {
    return lowerBound == null ? this.tes.nullType() : lowerBound;
  }

}
