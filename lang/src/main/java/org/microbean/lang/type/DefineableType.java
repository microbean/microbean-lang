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

import javax.lang.model.element.Element;

import javax.lang.model.type.TypeKind;

public abstract sealed class DefineableType<E extends Element> extends TypeMirror permits Capture, DeclaredType, TypeVariable {

  private E definingElement;

  protected DefineableType(final TypeKind kind) {
    super(kind);
  }

  public final E asElement() {
    return this.definingElement;
  }

  public final void setDefiningElement(final E definingElement) {
    final E old = this.asElement();
    if (old == null) {
      if (definingElement != null) {
        this.definingElement = this.validateDefiningElement(definingElement);
      }
    } else if (old != definingElement) {
      throw new IllegalStateException();
    }
  }

  public final boolean isDefined() {
    return this.asElement() != null;
  }

  public final javax.lang.model.type.TypeMirror getElementType() {
    final Element e = this.asElement();
    return e == null ? null : e.asType();
  }

  protected abstract E validateDefiningElement(final E e);

  @Override
  protected TypeKind validateKind(final TypeKind kind) {
    switch (kind) {
    case DECLARED:
    case ERROR:
    case TYPEVAR:
      return kind;
    default:
      throw new IllegalArgumentException("kind: " + kind);
    }
  }

}
