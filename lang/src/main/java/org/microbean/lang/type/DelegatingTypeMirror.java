/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2023–2024 microBean™.
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

import java.util.function.Supplier;

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

import static java.lang.constant.ConstantDescs.BSM_INVOKE;
import static java.lang.constant.DirectMethodHandleDesc.Kind.STATIC;

import static org.microbean.lang.ConstantDescs.CD_DelegatingTypeMirror;
import static org.microbean.lang.ConstantDescs.CD_Equality;
import static org.microbean.lang.ConstantDescs.CD_TypeAndElementSource;
import static org.microbean.lang.ConstantDescs.CD_TypeMirror;

/**
 * A {@link TypeMirror} that implements all known {@link TypeMirror} subinterfaces and delegates to an underlying {@link
 * TypeMirror} for all operations.
 *
 * <p>This class is safe for concurrent use by multiple threads.</p>
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
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


  /*
   * Instance fields.
   */


  private final TypeAndElementSource tes;

  private final Equality ehc;

  private Supplier<TypeMirror> delegateSupplier;


  /*
   * Constructors.
   */


  private DelegatingTypeMirror(final TypeMirror delegate, final TypeAndElementSource tes, final Equality ehc) {
    super();
    Objects.requireNonNull(delegate, "delegate");
    this.tes = tes == null ? Lang.typeAndElementSource() : tes;
    this.ehc = ehc == null ? new Equality(true) : ehc;
    this.delegateSupplier = () -> {
      final TypeMirror unwrappedDelegate = unwrap(delegate);
      CompletionLock.acquire();
      try {
        // Eagerly complete
        unwrappedDelegate.getKind();
        this.delegateSupplier = () -> unwrappedDelegate;
        return unwrappedDelegate;
      } finally {
        CompletionLock.release();
      }
    };
  }


  /*
   * Instance methods.
   */


  @Override // TypeMirror
  public final <R, P> R accept(final TypeVisitor<R, P> v, final P p) {
    return switch (this.getKind()) {
    case ARRAY        -> v.visitArray(this, p);
    case DECLARED     -> v.visitDeclared(this, p);
    case ERROR        -> v.visitError(this, p);
    case EXECUTABLE   -> v.visitExecutable(this, p);
    case INTERSECTION -> v.visitIntersection(this, p);
    case
      MODULE,
      NONE,
      PACKAGE,
      VOID            -> v.visitNoType(this, p);
    case NULL         -> v.visitNull(this, p);
    case
      BOOLEAN,
      BYTE,
      CHAR,
      DOUBLE,
      FLOAT,
      INT,
      LONG,
      SHORT           -> v.visitPrimitive(this, p);
    case TYPEVAR      -> v.visitTypeVariable(this, p);
    case UNION        -> v.visitUnion(this, p);
    case WILDCARD     -> v.visitWildcard(this, p);
    case OTHER        -> v.visitUnknown(this, p);
    };
  }

  @Override // Various
  public final DelegatingElement asElement() {
    return switch (this.getKind()) {
    case DECLARED -> DelegatingElement.of(((DeclaredType)this.delegate()).asElement(), this.tes, this.ehc);
    case TYPEVAR  -> DelegatingElement.of(((TypeVariable)this.delegate()).asElement(), this.tes, this.ehc);
    default       -> null;
    };
  }

  public final TypeMirror delegate() {
    return this.delegateSupplier.get();
  }

  @Override // UnionType
  public final List<? extends DelegatingTypeMirror> getAlternatives() {
    return switch (this.getKind()) {
    case UNION -> this.wrap(((UnionType)this.delegate()).getAlternatives());
    default    -> List.of();
    };
  }

  @Override // TypeMirror
  public final <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
    return this.delegate().getAnnotation(annotationType);
  }

  @Override // TypeMirror
  public final List<? extends AnnotationMirror> getAnnotationMirrors() {
    // TODO: delegating annotation mirror?
    return this.delegate().getAnnotationMirrors();
  }

  @Override // TypeMirror
  public final <A extends Annotation> A[] getAnnotationsByType(final Class<A> annotationType) {
    return this.delegate().getAnnotationsByType(annotationType);
  }

  @Override // IntersectionType
  public final List<? extends DelegatingTypeMirror> getBounds() {
    return switch (this.getKind()) {
    case INTERSECTION -> this.wrap(((IntersectionType)this.delegate()).getBounds());
    default           -> List.of();
    };
  }

  @Override // ArrayType
  public final DelegatingTypeMirror getComponentType() {
    return switch (this.getKind()) {
    case ARRAY -> this.wrap(((ArrayType)this.delegate()).getComponentType());
    default    -> this.wrap(this.tes.noType(TypeKind.NONE));
    };
  }

  @Override // DeclaredType
  public final DelegatingTypeMirror getEnclosingType() {
    return switch(this.getKind()) {
    case DECLARED -> this.wrap(((DeclaredType)this.delegate()).getEnclosingType());
    default       -> this.wrap(this.tes.noType(TypeKind.NONE));
    };
  }

  @Override // WildcardType
  public final DelegatingTypeMirror getExtendsBound() {
    return switch (this.getKind()) {
    case WILDCARD -> this.wrap(((WildcardType)this.delegate()).getExtendsBound());
    default       -> null;
    };
  }

  @Override // TypeMirror
  public final TypeKind getKind() {
    return this.delegate().getKind();
  }

  @Override // TypeVariable
  public final DelegatingTypeMirror getLowerBound() {
    return switch (this.getKind()) {
    case TYPEVAR -> this.wrap(((TypeVariable)this.delegate()).getLowerBound());
    default      -> this.wrap(this.tes.nullType()); // bottom (null) type, not NONE type
    };
  }

  @Override // TypeVariable
  public final DelegatingTypeMirror getUpperBound() {
    return switch (this.getKind()) {
    case TYPEVAR -> this.wrap(((TypeVariable)this.delegate()).getUpperBound());
    default      -> this.wrap(this.tes.typeElement("java.lang.Object").asType());
    };
  }

  @Override // ExecutableType
  public final List<? extends DelegatingTypeMirror> getParameterTypes() {
    return switch (this.getKind()) {
    case EXECUTABLE -> this.wrap(((ExecutableType)this.delegate()).getParameterTypes());
    default         -> List.of();
    };
  }

  @Override // ExecutableType
  public final DelegatingTypeMirror getReceiverType() {
    return switch (this.getKind()) {
    case EXECUTABLE -> this.wrap(((ExecutableType)this.delegate()).getReceiverType());
    default         -> this.wrap(this.tes.noType(TypeKind.NONE));
    };
  }

  @Override // ExecutableType
  public final DelegatingTypeMirror getReturnType() {
    return switch (this.getKind()) {
    case EXECUTABLE -> this.wrap(((ExecutableType)this.delegate()).getReturnType());
    default         -> this.wrap(this.tes.noType(TypeKind.VOID));
    };
  }

  @Override // WildcardType
  public final DelegatingTypeMirror getSuperBound() {
    return switch (this.getKind()) {
    case WILDCARD -> this.wrap(((WildcardType)this.delegate()).getSuperBound());
    default       -> null;
    };
  }

  @Override // ExecutableType
  public final List<? extends DelegatingTypeMirror> getThrownTypes() {
    return switch (this.getKind()) {
    case EXECUTABLE -> this.wrap(((ExecutableType)this.delegate()).getThrownTypes());
    default         -> List.of();
    };
  }

  @Override // DeclaredType
  public final List<? extends DelegatingTypeMirror> getTypeArguments() {
    return switch (this.getKind()) {
    case DECLARED -> this.wrap(((DeclaredType)this.delegate()).getTypeArguments());
    default       -> List.of();
    };
  }

  @Override // ExecutableType
  public final List<? extends DelegatingTypeMirror> getTypeVariables() {
    return switch (this.getKind()) {
    case EXECUTABLE -> this.wrap(((ExecutableType)this.delegate()).getTypeVariables());
    default         -> List.of();
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
    return this.delegate().toString();
  }

  @Override // Constable
  public final Optional<? extends ConstantDesc> describeConstable() {
    return this.ehc.describeConstable()
      .flatMap(equalityDesc -> (this.tes instanceof Constable c ? c.describeConstable() : Optional.<ConstantDesc>empty())
               .flatMap(tesDesc -> this.tes.describeConstable(this.delegate())
                        .map(delegateDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                                    MethodHandleDesc.ofMethod(STATIC,
                                                                                              CD_DelegatingTypeMirror,
                                                                                              "of",
                                                                                              MethodTypeDesc.of(CD_DelegatingTypeMirror,
                                                                                                                CD_TypeMirror,
                                                                                                                CD_TypeAndElementSource,
                                                                                                                CD_Equality)),
                                                                    delegateDesc,
                                                                    tesDesc,
                                                                    equalityDesc))));
  }

  private final DelegatingTypeMirror wrap(final TypeMirror t) {
    return of(t, this.tes, this.ehc);
  }

  private final List<DelegatingTypeMirror> wrap(final Collection<? extends TypeMirror> ts) {
    return of(ts, this.tes, this.ehc);
  }


  /*
   * Static methods.
   */


  public static final List<DelegatingTypeMirror> of(final Collection<? extends TypeMirror> ts, final TypeAndElementSource tes) {
    return of(ts, tes, null);
  }

  public static final List<DelegatingTypeMirror> of(final Collection<? extends TypeMirror> ts, final TypeAndElementSource tes, final Equality ehc) {
    final List<DelegatingTypeMirror> newTs = new ArrayList<>(ts.size());
    for (final TypeMirror t : ts) {
      newTs.add(of(t, tes, ehc));
    }
    return Collections.unmodifiableList(newTs);
  }

  public static final DelegatingTypeMirror of(final TypeMirror t, final TypeAndElementSource tes) {
    return of(t, tes, null);
  }

  // Called by describeConstable
  public static final DelegatingTypeMirror of(final TypeMirror t, final TypeAndElementSource tes, final Equality ehc) {
    return
      t == null ? null :
      t instanceof DelegatingTypeMirror d ? d :
      new DelegatingTypeMirror(t, tes, ehc);
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
