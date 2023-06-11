/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2023 microBean™.
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

import java.lang.annotation.Annotation;

import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

import org.microbean.lang.CompletionLock;
import org.microbean.lang.Lang;
import org.microbean.lang.TypeAndElementSource;
import org.microbean.lang.Equality;

import org.microbean.lang.element.DelegatingElement;

import org.microbean.lang.type.NoType;
import org.microbean.lang.type.Types;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;
import static java.lang.constant.DirectMethodHandleDesc.Kind.STATIC;

public final class DelegatingTypeMirror
  implements ArrayType,
             Constable,
             ErrorType,
             ExecutableType,
             IntersectionType,
             javax.lang.model.type.NoType,
             NullType,
             PrimitiveType,
             TypeVariable,
             UnionType,
             WildcardType {

  private static final ClassDesc CD_DelegatingTypeMirror = ClassDesc.of("org.microbean.lang.type.DelegatingTypeMirror");

  private static final ClassDesc CD_TypeAndElementSource = ClassDesc.of("org.microbean.lang.TypeAndElementSource");

  private static final ClassDesc CD_Equality = ClassDesc.of("org.microbean.lang.Equality");

  private static final ClassDesc CD_TypeMirror = ClassDesc.of("javax.lang.model.type.TypeMirror");

  private final TypeAndElementSource elementSource;

  private final TypeMirror delegate;

  private final Equality ehc;

  private DelegatingTypeMirror(final TypeMirror delegate, final TypeAndElementSource elementSource, final Equality ehc) {
    super();
    this.delegate = unwrap(Objects.requireNonNull(delegate, "delegate"));
    this.elementSource = elementSource == null ? Lang.typeAndElementSource() : elementSource;
    this.ehc = ehc == null ? new Equality(true) : ehc;
  }

  @Override // TypeMirror
  public final <R, P> R accept(final TypeVisitor<R, P> v, final P p) {
    return switch (this.getKind()) {
    case ARRAY -> v.visitArray(this, p);
    case DECLARED -> v.visitDeclared(this, p);
    case ERROR -> v.visitError(this, p);
    case EXECUTABLE -> v.visitExecutable(this, p);
    case INTERSECTION -> v.visitIntersection(this, p);
    case MODULE, NONE, PACKAGE, VOID -> v.visitNoType(this, p);
    case NULL -> v.visitNull(this, p);
    case BOOLEAN, BYTE, CHAR, DOUBLE, FLOAT, INT, LONG, SHORT -> v.visitPrimitive(this, p);
    case TYPEVAR -> v.visitTypeVariable(this, p);
    case UNION -> v.visitUnion(this, p);
    case WILDCARD -> v.visitWildcard(this, p);
    case OTHER -> v.visitUnknown(this, p);
    };
  }

  @Override // Various
  public final Element asElement() {
    return switch (this.getKind()) {
    case DECLARED -> DelegatingElement.of(((DeclaredType)this.delegate).asElement(), this.elementSource, this.ehc);
    case TYPEVAR -> DelegatingElement.of(((TypeVariable)this.delegate).asElement(), this.elementSource, this.ehc);
    default -> null;
    };
  }

  public final TypeMirror delegate() {
    return this.delegate;
  }

  @Override // UnionType
  public final List<? extends TypeMirror> getAlternatives() {
    return switch (this.getKind()) {
    case UNION -> this.wrap(((UnionType)this.delegate).getAlternatives());
    default -> List.of();
    };
  }

  @Override // TypeMirror
  public final <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
    return this.delegate.getAnnotation(annotationType);
  }

  @Override // TypeMirror
  public final List<? extends AnnotationMirror> getAnnotationMirrors() {
    // TODO: delegating annotation mirror?
    return this.delegate.getAnnotationMirrors();
  }

  @Override // TypeMirror
  public final <A extends Annotation> A[] getAnnotationsByType(final Class<A> annotationType) {
    return this.delegate.getAnnotationsByType(annotationType);
  }

  @Override // IntersectionType
  public final List<? extends TypeMirror> getBounds() {
    return switch (this.getKind()) {
    case INTERSECTION -> this.wrap(((IntersectionType)this.delegate).getBounds());
    default -> List.of();
    };
  }

  @Override // ArrayType
  public final TypeMirror getComponentType() {
    return switch (this.getKind()) {
    case ARRAY -> this.wrap(((ArrayType)this.delegate).getComponentType());
    default -> NoType.NONE;
    };
  }

  @Override // DeclaredType
  public final TypeMirror getEnclosingType() {
    return switch(this.getKind()) {
    case DECLARED -> this.wrap(((DeclaredType)this.delegate).getEnclosingType());
    default -> NoType.NONE;
    };
  }

  @Override // WildcardType
  public final TypeMirror getExtendsBound() {
    return switch (this.getKind()) {
    case WILDCARD -> this.wrap(((WildcardType)this.delegate).getExtendsBound());
    default -> null;
    };
  }

  @Override // TypeMirror
  public final TypeKind getKind() {
    synchronized (CompletionLock.monitor()) { // CRITICAL!
      return this.delegate.getKind();
    }
  }

  @Override // TypeVariable
  public final TypeMirror getLowerBound() {
    return switch (this.getKind()) {
    case TYPEVAR -> this.wrap(((TypeVariable)this.delegate).getLowerBound());
    default -> org.microbean.lang.type.NullType.INSTANCE; // bottom type, not NONE type
    };
  }

  @Override // TypeVariable
  public final TypeMirror getUpperBound() {
    return switch (this.getKind()) {
    case TYPEVAR -> this.wrap(((TypeVariable)this.delegate).getUpperBound());
    default -> this.wrap(this.elementSource.typeElement("java.lang.Object").asType());
    };
  }

  @Override // ExecutableType
  public final List<? extends TypeMirror> getParameterTypes() {
    return switch (this.getKind()) {
    case EXECUTABLE -> this.wrap(((ExecutableType)this.delegate).getParameterTypes());
    default -> List.of();
    };
  }

  @Override // ExecutableType
  public final TypeMirror getReceiverType() {
    return switch (this.getKind()) {
    case EXECUTABLE -> this.wrap(((ExecutableType)this.delegate).getReceiverType());
    default -> null;
    };
  }

  @Override // ExecutableType
  public final TypeMirror getReturnType() {
    return switch (this.getKind()) {
    case EXECUTABLE -> this.wrap(((ExecutableType)this.delegate).getReturnType());
    default -> null;
    };
  }

  @Override // WildcardType
  public final TypeMirror getSuperBound() {
    return switch (this.getKind()) {
    case WILDCARD -> this.wrap(((WildcardType)this.delegate).getSuperBound());
    default -> null;
    };
  }

  @Override // ExecutableType
  public final List<? extends TypeMirror> getThrownTypes() {
    return switch (this.getKind()) {
    case EXECUTABLE -> this.wrap(((ExecutableType)this.delegate).getThrownTypes());
    default -> List.of();
    };
  }

  @Override // DeclaredType
  public final List<? extends TypeMirror> getTypeArguments() {
    return switch (this.getKind()) {
    case DECLARED -> this.wrap(((DeclaredType)this.delegate).getTypeArguments());
    default -> List.of();
    };
  }

  @Override // ExecutableType
  public final List<? extends TypeVariable> getTypeVariables() {
    return switch (this.getKind()) {
    case EXECUTABLE -> this.wrap(((ExecutableType)this.delegate).getTypeVariables());
    default -> List.of();
    };
  }

  @Override // TypeMirror
  public final int hashCode() {
    return this.ehc.hashCode(this);
  }

  @Override // TypeMirror
  public final boolean equals(final Object other) {
    return this.ehc.equals(this, other);
  }

  @Override // TypeMirror
  public final String toString() {
    return this.delegate.toString();
  }

  @Override // Constable
  public final Optional<? extends ConstantDesc> describeConstable() {
    return Lang.describeConstable(this.delegate)
      .flatMap(delegateDesc -> (this.elementSource instanceof Constable c ? c.describeConstable() : Optional.<ConstantDesc>empty())
               .flatMap(elementSourceDesc -> this.ehc.describeConstable()
                        .map(equalityDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                                    MethodHandleDesc.ofMethod(STATIC,
                                                                                              CD_DelegatingTypeMirror,
                                                                                              "of",
                                                                                              MethodTypeDesc.of(CD_DelegatingTypeMirror,
                                                                                                                CD_TypeMirror,
                                                                                                                CD_TypeAndElementSource,
                                                                                                                CD_Equality)),
                                                                    delegateDesc,
                                                                    elementSourceDesc,
                                                                    equalityDesc))));
  }

  private final DelegatingTypeMirror wrap(final TypeMirror t) {
    return of(t, this.elementSource, this.ehc);
  }

  private final List<DelegatingTypeMirror> wrap(final Collection<? extends TypeMirror> ts) {
    return of(ts, this.elementSource, this.ehc);
  }


  /*
   * Static methods.
   */


  public static final List<DelegatingTypeMirror> of(final Collection<? extends TypeMirror> ts, final TypeAndElementSource elementSource) {
    return of(ts, elementSource, null);
  }

  public static final List<DelegatingTypeMirror> of(final Collection<? extends TypeMirror> ts, final TypeAndElementSource elementSource, final Equality ehc) {
    final List<DelegatingTypeMirror> newTs = new ArrayList<>(ts.size());
    for (final TypeMirror t : ts) {
      newTs.add(of(t, elementSource, ehc));
    }
    return Collections.unmodifiableList(newTs);
  }

  public static final DelegatingTypeMirror of(final TypeMirror t, final TypeAndElementSource elementSource) {
    return of(t, elementSource, null);
  }

  // Called by describeConstable
  public static final DelegatingTypeMirror of(final TypeMirror t, final TypeAndElementSource elementSource, final Equality ehc) {
    return
      t == null ? null :
      t instanceof DelegatingTypeMirror d ? d :
      new DelegatingTypeMirror(t, elementSource, ehc);
  }

  public static final TypeMirror unwrap(TypeMirror t) {
    while (t instanceof DelegatingTypeMirror dt) {
      t = dt.delegate();
    }
    return t;
  }

  public static final TypeMirror[] unwrap(final TypeMirror[] ts) {
    final TypeMirror[] rv = new TypeMirror[ts.length];
    for (int i = 0; i < ts.length; i++) {
      rv[i] = unwrap(ts[i]);
    }
    return rv;
  }

}
