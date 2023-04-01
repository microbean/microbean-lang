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
package org.microbean.lang.type;

import java.lang.annotation.Annotation;

import java.util.List;
import java.util.Objects;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;

import org.microbean.lang.ElementSource;
import org.microbean.lang.Equality;

import org.microbean.lang.type.NoType;
import org.microbean.lang.type.Types;

public final class DelegatingTypeMirror implements ArrayType, ErrorType, ExecutableType, IntersectionType, javax.lang.model.type.NoType, NullType, PrimitiveType, TypeVariable, UnionType, WildcardType {

  private final ElementSource elementSource;

  private final TypeMirror delegate;

  private final Equality ehc;

  private DelegatingTypeMirror(final TypeMirror delegate, final ElementSource elementSource, final Equality ehc) {
    super();
    this.delegate = Objects.requireNonNull(delegate, "delegate");
    this.elementSource = Objects.requireNonNull(elementSource, "elementSource");
    this.ehc = ehc == null ? new Equality(true) : ehc;
  }

  @Override // TypeMirror
  public final <R, P> R accept(final TypeVisitor<R, P> v, final P p) {
    return this.delegate.accept(v, p);
  }

  @Override // Various
  public final Element asElement() {
    return switch (this.delegate.getKind()) {
    case DECLARED -> ((DeclaredType)this.delegate).asElement();
    case TYPEVAR -> ((TypeVariable)this.delegate).asElement();
    default -> null;
    };
  }

  public final TypeMirror delegate() {
    return this.delegate;
  }

  @Override // UnionType
  public final List<? extends TypeMirror> getAlternatives() {
    return switch (this.delegate.getKind()) {
    case UNION -> ((UnionType)this.delegate).getAlternatives();
    default -> List.of();
    };
  }

  @Override // TypeMirror
  public final <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
    return this.delegate.getAnnotation(annotationType);
  }

  @Override // TypeMirror
  public final List<? extends AnnotationMirror> getAnnotationMirrors() {
    return this.delegate.getAnnotationMirrors();
  }

  @Override // TypeMirror
  public final <A extends Annotation> A[] getAnnotationsByType(final Class<A> annotationType) {
    return this.delegate.getAnnotationsByType(annotationType);
  }

  @Override // IntersectionType
  public final List<? extends TypeMirror> getBounds() {
    return switch (this.delegate.getKind()) {
    case INTERSECTION -> ((IntersectionType)this.delegate).getBounds();
    default -> List.of();
    };
  }

  @Override // ArrayType
  public final TypeMirror getComponentType() {
    return switch (this.delegate.getKind()) {
    case ARRAY -> ((ArrayType)this.delegate).getComponentType();
    default -> NoType.NONE;
    };
  }

  @Override // DeclaredType
  public final TypeMirror getEnclosingType() {
    return switch(this.delegate.getKind()) {
    case DECLARED -> ((DeclaredType)this.delegate).getEnclosingType();
    default -> NoType.NONE;
    };
  }

  @Override // WildcardType
  public final TypeMirror getExtendsBound() {
    return switch (this.delegate.getKind()) {
    case WILDCARD -> ((WildcardType)this.delegate).getExtendsBound();
    default -> null;
    };
  }

  @Override // TypeMirror
  public final TypeKind getKind() {
    return this.delegate.getKind();
  }

  @Override // TypeVariable
  public final TypeMirror getLowerBound() {
    return switch (this.delegate.getKind()) {
    case TYPEVAR -> ((TypeVariable)this.delegate).getLowerBound();
    default -> org.microbean.lang.type.NullType.INSTANCE; // bottom type, not NONE type
    };
  }

  @Override // TypeVariable
  public final TypeMirror getUpperBound() {
    return switch (this.delegate.getKind()) {
    case TYPEVAR -> ((TypeVariable)this.delegate).getUpperBound();
    default -> this.elementSource.element("java.lang.Object").asType();
    };
  }

  @Override // ExecutableType
  public final List<? extends TypeMirror> getParameterTypes() {
    return switch (this.delegate.getKind()) {
    case EXECUTABLE -> ((ExecutableType)this.delegate).getParameterTypes();
    default -> List.of();
    };
  }

  @Override // ExecutableType
  public final TypeMirror getReceiverType() {
    return switch (this.delegate.getKind()) {
    case EXECUTABLE -> ((ExecutableType)this.delegate).getReceiverType();
    default -> null;
    };
  }

  @Override // ExecutableType
  public final TypeMirror getReturnType() {
    return switch (this.delegate.getKind()) {
    case EXECUTABLE -> ((ExecutableType)this.delegate).getReturnType();
    default -> null;
    };
  }

  @Override // WildcardType
  public final TypeMirror getSuperBound() {
    return switch (this.delegate.getKind()) {
    case WILDCARD -> ((WildcardType)this.delegate).getSuperBound();
    default -> null;
    };
  }

  @Override // ExecutableType
  public final List<? extends TypeMirror> getThrownTypes() {
    return switch (this.delegate.getKind()) {
    case EXECUTABLE -> ((ExecutableType)this.delegate).getThrownTypes();
    default -> List.of();
    };
  }

  @Override // DeclaredType
  public final List<? extends TypeMirror> getTypeArguments() {
    return switch (this.delegate.getKind()) {
    case DECLARED -> ((DeclaredType)this.delegate).getTypeArguments();
    default -> List.of();
    };
  }

  @Override // ExecutableType
  public final List<? extends TypeVariable> getTypeVariables() {
    return switch (this.delegate.getKind()) {
    case EXECUTABLE -> ((ExecutableType)this.delegate).getTypeVariables();
    default -> List.of();
    };
  }

  @Override // TypeMirror
  public final int hashCode() {
    return this.ehc.hashCode(this.delegate);
  }

  @Override // TypeMirror
  public final boolean equals(final Object other) {
    return this.ehc.equals(this.delegate, other);
  }

  @Override // TypeMirror
  public final String toString() {
    return this.delegate.toString();
  }


  /*
   * Static methods.
   */


  public static final DelegatingTypeMirror of(final TypeMirror t, final ElementSource elementSource) {
    return of(t, elementSource, null);
  }

  public static final DelegatingTypeMirror of(final TypeMirror t, final ElementSource elementSource, final Equality ehc) {
    if (t instanceof DelegatingTypeMirror d) {
      return d;
    }
    return new DelegatingTypeMirror(t, elementSource, ehc);
  }

}
