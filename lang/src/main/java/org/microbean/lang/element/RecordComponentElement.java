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

import java.util.Objects;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;

import javax.lang.model.type.TypeMirror;

public final class RecordComponentElement extends Element implements javax.lang.model.element.RecordComponentElement {

  private ExecutableElement accessor;
  
  public RecordComponentElement() {
    super(ElementKind.RECORD_COMPONENT);
  }

  @Override // AbstractElement
  public final <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    return v.visitRecordComponent(this, p);
  }

  @Override // Element
  public final boolean isUnnamed() {
    return false;
  }

  @Override // RecordComponentElement
  public final ExecutableElement getAccessor() {
    return this.accessor;
  }

  public final void setAccessor(final ExecutableElement e) {
    final Object old = this.getAccessor();
    if (old == null) {
      if (e != null) {
        this.accessor = validateAccessor(e);
      }
    } else if (old != e) {
      throw new IllegalStateException();
    }
  }

  @Override
  protected final TypeMirror validateType(final TypeMirror type) {
    switch (type.getKind()) {
    case DECLARED:
      return type;
    default:
      throw new IllegalArgumentException("type: " + type);
    }
  }

  private static final ExecutableElement validateAccessor(final ExecutableElement e) {
    return Objects.requireNonNull(e, "e");
  }

}
