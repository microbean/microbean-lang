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
import java.util.Set;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public final class RecordComponentElement extends Element implements javax.lang.model.element.RecordComponentElement {

  private ExecutableElement accessor;
  
  public RecordComponentElement() {
    super(ElementKind.RECORD_COMPONENT);
    // Record components are always public and nothing else.
    this.addModifier(Modifier.PUBLIC);
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
        this.accessor = this.validateAccessor(e);
      }
    } else if (old != e) {
      throw new IllegalStateException("e: " + e + "; old: " + old);
    }
  }

  @Override
  protected final <E extends javax.lang.model.element.Element> E validateEnclosedElement(final E e) {
    throw new IllegalArgumentException("record components cannot enclose Elements");
  }
  
  @Override
  protected final ElementKind validateKind(final ElementKind kind) {
    switch (kind) {
    case RECORD_COMPONENT:
      return kind;
    default:
      throw new IllegalArgumentException("kind: " + kind);
    }
  }

  @Override // Element
  protected final Modifier validateModifier(final Modifier modifier) {
    switch (modifier) {
    case PUBLIC:
      return modifier;
    default:
      throw new IllegalArgumentException("modifier: " + modifier);
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

  private final ExecutableElement validateAccessor(final ExecutableElement e) {
    switch (e.getKind()) {
    case METHOD:
      final Set<Modifier> modifiers = e.getModifiers();
      if (modifiers.contains(Modifier.ABSTRACT)) {
        throw new IllegalArgumentException("abstract accessor: " + e);
      } else if (e.isDefault() || modifiers.contains(Modifier.DEFAULT)) {
        throw new IllegalArgumentException("default accessor: " + e);
      } else if (modifiers.contains(Modifier.NATIVE)) {
        throw new IllegalArgumentException("native accessor: " + e);
      } else if (!modifiers.contains(Modifier.PUBLIC)) {
        throw new IllegalArgumentException("non-public accessor: " + e);
      } else if (modifiers.contains(Modifier.STATIC)) {
        throw new IllegalArgumentException("static accessor: " + e);
      } else if (e.asType().getKind() != TypeKind.EXECUTABLE ||
                 !e.getParameters().isEmpty() ||
                 !e.getThrownTypes().isEmpty() ||
                 !e.getTypeParameters().isEmpty() ||
                 !e.getReturnType().equals(this.asType()) ||
                 !e.getSimpleName().equals(this.getSimpleName())) {
        throw new IllegalArgumentException("invalid accessor: " + e);
      }
      // TODO: here or elsewhere: e must have Modifier.PUBLIC; e must not be static, etc. etc.
      return e;
    default:
      throw new IllegalArgumentException("e: " + e);
    }
  }

}
