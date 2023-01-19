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
package org.microbean.lang.element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.QualifiedNameable;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import org.microbean.lang.type.DefineableType;

public final class TypeElement extends Parameterizable implements javax.lang.model.element.TypeElement {

  private final NestingKind nestingKind;

  private TypeMirror superclass;

  private final List<TypeMirror> interfaces;

  private final List<TypeMirror> unmodifiableInterfaces;

  private final List<TypeMirror> permittedSubclasses;

  private final List<TypeMirror> unmodifiablePermittedSubclasses;

  public TypeElement(final ElementKind kind) {
    this(kind, NestingKind.TOP_LEVEL);
  }

  public TypeElement(final ElementKind kind,
                     final NestingKind nestingKind) {
    super(kind);
    this.nestingKind = nestingKind == null ? NestingKind.TOP_LEVEL : nestingKind;
    this.interfaces = new ArrayList<>(5);
    this.unmodifiableInterfaces = Collections.unmodifiableList(this.interfaces);
    this.permittedSubclasses = new ArrayList<>(5);
    this.unmodifiablePermittedSubclasses = Collections.unmodifiableList(this.permittedSubclasses);
  }

  @Override // TypeElement
  public final <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    return v.visitType(this, p);
  }

  @Override // TypeElement
  public final List<? extends TypeMirror> getInterfaces() {
    return this.unmodifiableInterfaces;
  }

  public final void addInterface(final TypeMirror i) {
    switch (i.getKind()) {
    case DECLARED:
      switch (((DeclaredType)i).asElement().getKind()) {
      case INTERFACE:
        this.interfaces.add(i);
        return;
      default:
        break;
      }
    default:
      break;
    }
    throw new IllegalArgumentException("i: " + i);
  }

  @Override // TypeElement
  public final NestingKind getNestingKind() {
    return this.nestingKind;
  }

  @Override // TypeElement
  public final javax.lang.model.element.Name getQualifiedName() {
    final QualifiedNameable qn;
    switch (this.getNestingKind()) {
    case ANONYMOUS:      
    case LOCAL:
      qn = null;
      break;
    case MEMBER:
    case TOP_LEVEL:
      qn = (QualifiedNameable)this.getEnclosingElement();
      break;
    default:
      throw new AssertionError();
    }
    return qn == null ? this.getSimpleName() : Name.of(qn.getQualifiedName() + "." + this.getSimpleName());
  }

  @Override // TypeElement
  public final TypeMirror getSuperclass() {
    return this.superclass;
  }

  public final void setSuperclass(final TypeMirror superclass) {
    final Object old = this.getSuperclass();
    if (old == null) {
      if (superclass != null) {
        this.superclass = validateSuperclass(superclass);
      }
    } else if (old != superclass) {
      throw new IllegalStateException();
    }
  }

  @Override // TypeElement
  public final List<? extends TypeMirror> getPermittedSubclasses() {
    return this.unmodifiablePermittedSubclasses;
  }

  public final void addPermittedSubclass(final TypeMirror t) {
    this.permittedSubclasses.add(this.validatePermittedSubclass(t));
  }

  public final void addPermittedSubclasses(final Iterable<? extends TypeMirror> ts) {
    for (final TypeMirror t : ts) {
      this.addPermittedSubclass(t);
    }
  }

  protected TypeMirror validatePermittedSubclass(final TypeMirror t) {
    return Objects.requireNonNull(t, "t");
  }
  
  @Override // Element
  protected ElementKind validateKind(final ElementKind kind) {
    switch (kind) {
    case ANNOTATION_TYPE:
    case CLASS:
    case ENUM:
    case INTERFACE:
    case RECORD:
      return kind;
    default:
      throw new IllegalArgumentException("kind: " + kind);
    }
  }

  @Override // Element
  protected TypeMirror validateType(final TypeMirror type) {
    switch (type.getKind()) {
    case DECLARED:
    case ERROR:
      return type;
    default:
      throw new IllegalArgumentException("type: " + type);
    }
  }

  @Override
  public final String toString() {
    final CharSequence n = this.getQualifiedName();
    if (n == null) {
      return "<unknown>";
    } else if (this.isUnnamed() || n.length() <= 0) {
      return "<anonymous>";
    } else {
      return n.toString();
    }
  }
  
  private static final <T extends TypeMirror> T validateSuperclass(final T superclass) {
    return superclass;
  }

}
