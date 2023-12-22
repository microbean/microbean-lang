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

import java.util.List;
import java.util.Objects;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.TypeMirror;

import org.microbean.lang.Equality;

/**
 * A simple implementation of the {@link javax.lang.model.element.AnnotationValue} interface.
 *
 * @author <a href="https://about.me/lairdnelson/" target="_top">Laird Nelson</a>
 */
public final class AnnotationValue implements javax.lang.model.element.AnnotationValue {


  /*
   * Instance fields.
   */


  private final Object value;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link AnnotationValue}.
   *
   * @param value the value to be returned by the {@link #getValue()} method; may be {@code null}
   *
   * @see #getValue()
   */
  public AnnotationValue(final Object value) {
    super();
    this.value = Objects.requireNonNull(value, "value");
  }


  /*
   * Instance methods.
   */


  @Override // AnnotationValue
  @SuppressWarnings("unchecked")
  public final <R, P> R accept(final AnnotationValueVisitor<R, P> v, final P p) {
    return switch (this.getValue()) {
    case String s -> v.visitString(s, p);
    case Boolean b -> v.visitBoolean(b, p);
    case Integer i -> v.visitInt(i, p);
    case VariableElement e -> v.visitEnumConstant(e, p);
    case javax.lang.model.element.AnnotationValue a -> v.visit(a, p);
    case AnnotationMirror a -> v.visitAnnotation(a, p);
    case List<?> list -> v.visitArray((List<? extends AnnotationValue>)list, p);
    case TypeMirror t -> v.visitType(t, p);
    case Byte b -> v.visitByte(b, p);
    case Character c -> v.visitChar(c, p);
    case Double d -> v.visitDouble(d, p);
    case Float f -> v.visitFloat(f, p);
    case Long l -> v.visitLong(l, p);
    case Short s -> v.visitShort(s, p);
    default -> v.visitUnknown(this, p);
    };
  }

  @Override // AnnotationValue
  public final Object getValue() {
    return this.value;
  }

  @Override // Object
  public final int hashCode() {
    return Equality.hashCode(this, true);
  }

  @Override // Object
  public final boolean equals(final Object other) {
    if (this == other) {
      return true;
    } else if (other instanceof javax.lang.model.element.AnnotationValue her) { // instanceof is on purpose
      return Equality.equals(this, her, true);
    } else {
      return false;
    }
  }


  /*
   * Static methods.
   */


  /**
   * Returns an {@link AnnotationValue} whose {@link #getValue()} method will return the supplied {@code value}.
   *
   * @param value the value; may be {@code null}
   *
   * @return an {@link AnnotationValue} whose {@link #getValue()} method will return the supplied {@code value}; never
   * {@code null}
   *
   * @see #getValue()
   */
  public static final AnnotationValue of(final Object value) {
    return new AnnotationValue(value);
  }

}
