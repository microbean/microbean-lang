/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2022–2023 microBean™.
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
package org.microbean.lang.element;

import java.lang.annotation.Annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.Directive;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.TypeMirror;

import org.microbean.lang.CompletionLock;
import org.microbean.lang.ElementSource;
import org.microbean.lang.Equality;

import org.microbean.lang.type.DelegatingTypeMirror;
import org.microbean.lang.type.NoType;

public final class DelegatingElement
  implements ExecutableElement,
             ModuleElement,
             PackageElement,
             Parameterizable,
             RecordComponentElement,
             TypeElement,
             TypeParameterElement,
             VariableElement {


  /*
   * Instance fields.
   */


  private final Element delegate;

  private final ElementSource elementSource;

  private final Equality ehc;


  /*
   * Constructors.
   */


  private DelegatingElement(final Element delegate,
                            final ElementSource elementSource,
                            final Equality ehc) {
    super();
    this.delegate = unwrap(Objects.requireNonNull(delegate, "delegate"));
    this.elementSource = Objects.requireNonNull(elementSource, "elementSource");
    this.ehc = ehc == null ? new Equality(true) : ehc;
  }


  /*
   * Instance methods.
   */


  @Override // Element
  public final <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    return switch (this.getKind()) {
    case ANNOTATION_TYPE, CLASS, ENUM, INTERFACE, RECORD -> v.visitType((javax.lang.model.element.TypeElement)this, p);
    case TYPE_PARAMETER -> v.visitTypeParameter((javax.lang.model.element.TypeParameterElement)this, p);
    case BINDING_VARIABLE, ENUM_CONSTANT, EXCEPTION_PARAMETER, FIELD, LOCAL_VARIABLE, PARAMETER, RESOURCE_VARIABLE -> v.visitVariable((javax.lang.model.element.VariableElement)this, p);
    case RECORD_COMPONENT -> v.visitRecordComponent((javax.lang.model.element.RecordComponentElement)this, p);
    case CONSTRUCTOR, INSTANCE_INIT, METHOD, STATIC_INIT -> v.visitExecutable((javax.lang.model.element.ExecutableElement)this, p);
    case PACKAGE -> v.visitPackage((javax.lang.model.element.PackageElement)this, p);
    case MODULE -> v.visitModule((javax.lang.model.element.ModuleElement)this, p);
    case OTHER -> v.visitUnknown(this, p);
    };
  }

  @Override // Element
  public final TypeMirror asType() {
    return DelegatingTypeMirror.of(this.delegate.asType(), this.elementSource, this.ehc);
  }

  public final Element delegate() {
    return this.delegate;
  }

  @Override // RecordComponentElement
  public final ExecutableElement getAccessor() {
    switch (this.getKind()) {
    case RECORD_COMPONENT:
      return of(((RecordComponentElement)this.delegate).getAccessor(), this.elementSource, this.ehc);
    default:
      return null;
    }
  }

  @Override // Element
  public final <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
    return this.delegate.getAnnotation(annotationType);
  }

  @Override // Element
  public final List<? extends AnnotationMirror> getAnnotationMirrors() {
    synchronized (CompletionLock.class) {
      // TODO: delegating annotation mirror?
      return this.delegate.getAnnotationMirrors();
    }
  }

  @Override // Element
  public final <A extends Annotation> A[] getAnnotationsByType(final Class<A> annotationType) {
    return this.delegate.getAnnotationsByType(annotationType);
  }

  @Override // TypeParameterElement
  public final List<? extends TypeMirror> getBounds() {
    switch (this.getKind()) {
    case TYPE_PARAMETER:
      return DelegatingTypeMirror.of(((TypeParameterElement)this.delegate).getBounds(), this.elementSource, this.ehc);
    default:
      return List.of();
    }
  }

  @Override // VariableElement
  public final Object getConstantValue() {
    switch (this.getKind()) {
    case BINDING_VARIABLE:
    case ENUM_CONSTANT:
    case EXCEPTION_PARAMETER:
    case FIELD:
    case LOCAL_VARIABLE:
    case PARAMETER:
    case RESOURCE_VARIABLE:
      return ((VariableElement)this.delegate).getConstantValue();
    default:
      return null;
    }
  }

  @Override // ExecutableElement
  public final AnnotationValue getDefaultValue() {
    switch (this.getKind()) {
    case ANNOTATION_TYPE:
      // TODO: delegating annotation value? could be a type mirror after all
      return ((ExecutableElement)this.delegate).getDefaultValue();
    default:
      return null;
    }
  }

  @Override // ModuleElement
  public final List<? extends Directive> getDirectives() {
    switch (this.getKind()) {
    case MODULE:
      return ((ModuleElement)this.delegate).getDirectives();
    default:
      return List.of();
    }
  }

  @Override // Element
  public final List<? extends Element> getEnclosedElements() {
    final List<? extends Element> ee;
    synchronized (CompletionLock.class) {
      ee = this.delegate.getEnclosedElements();
    }
    return of(ee, this.elementSource, this.ehc);
  }

  @Override // Element
  public final Element getEnclosingElement() {
    return of(this.delegate.getEnclosingElement(), this.elementSource, this.ehc);
  }

  @Override // TypeParameterElement
  public final Element getGenericElement() {
    switch (this.getKind()) {
    case TYPE_PARAMETER:
      return of(((TypeParameterElement)this.delegate).getGenericElement(), this.elementSource, this.ehc);
    default:
      return null; // illegal state
    }
  }

  @Override // TypeElement
  public final List<? extends TypeMirror> getInterfaces() {
    switch (this.getKind()) {
    case ANNOTATION_TYPE:
    case CLASS:
    case ENUM:
    case INTERFACE:
    case RECORD:
      return DelegatingTypeMirror.of(((TypeElement)this.delegate).getInterfaces(), this.elementSource, this.ehc);
    default:
      return List.of();
    }
  }

  @Override // Element
  public final ElementKind getKind() {
    synchronized (CompletionLock.class) {
      return this.delegate.getKind();
    }
  }

  @Override // Element
  public final Set<Modifier> getModifiers() {
    synchronized (CompletionLock.class) {
      return this.delegate.getModifiers();
    }
  }

  @Override // TypeElement
  public final NestingKind getNestingKind() {
    switch (this.getKind()) {
    case ANNOTATION_TYPE:
    case CLASS:
    case ENUM:
    case INTERFACE:
    case RECORD:
      return ((TypeElement)this.delegate).getNestingKind();
    default:
      return null;
    }
  }

  @Override // ExecutableElement
  public final List<? extends VariableElement> getParameters() {
    switch (this.getKind()) {
    case CONSTRUCTOR:
    case METHOD:
      return of(((ExecutableElement)this.delegate).getParameters(), this.elementSource, this.ehc);
    default:
      return List.of();
    }
  }

  @Override // ModuleElement, PackageElement, TypeElement
  public final Name getQualifiedName() {
    switch (this.getKind()) {
    case ANNOTATION_TYPE:
    case CLASS:
    case ENUM:
    case INTERFACE:
    case RECORD:
    case MODULE:
    case PACKAGE:
      return ((QualifiedNameable)this.delegate).getQualifiedName();
    default:
      return org.microbean.lang.element.Name.of();
    }
  }

  @Override // ExecutableElement
  public final TypeMirror getReceiverType() {
    return DelegatingTypeMirror.of(this.asType(), this.elementSource, this.ehc).getReceiverType();
  }

  @Override // TypeElement
  public final List<? extends RecordComponentElement> getRecordComponents() {
    switch (this.getKind()) {
    case RECORD:
      return ((TypeElement)this.delegate).getRecordComponents();
    default:
      return List.of();
    }
  }

  @Override // ExecutableElement
  public final TypeMirror getReturnType() {
    return DelegatingTypeMirror.of(this.asType(), this.elementSource, this.ehc).getReturnType();
  }

  @Override // Element
  public final Name getSimpleName() {
    return this.delegate.getSimpleName();
  }

  @Override // TypeElement
  public final TypeMirror getSuperclass() {
    switch (this.getKind()) {
    case ANNOTATION_TYPE:
    case CLASS:
    case ENUM:
    case INTERFACE:
    case RECORD:
      return DelegatingTypeMirror.of(((TypeElement)this.delegate).getSuperclass(), this.elementSource, this.ehc);
    default:
      return NoType.NONE;
    }
  }

  @Override // ExecutableElement
  public final List<? extends TypeMirror> getThrownTypes() {
    if (this.getKind().isExecutable()) {
      return DelegatingTypeMirror.of(((ExecutableElement)this.delegate).getThrownTypes(), this.elementSource, this.ehc);
    }
    return List.of();
  }

  @Override // ExecutableElement
  public final List<? extends TypeParameterElement> getTypeParameters() {
    switch (this.getKind()) {
    case CLASS:
    case CONSTRUCTOR:
    case ENUM:
    case INTERFACE:
    case RECORD:
    case METHOD:
      return of(((Parameterizable)this.delegate).getTypeParameters(), this.elementSource, this.ehc);
    default:
      return List.of();
    }
  }

  @Override // ExecutableElement
  public final boolean isDefault() {
    switch (this.getKind()) {
    case METHOD:
      return ((ExecutableElement)this.delegate).isDefault();
    default:
      return false;
    }
  }

  @Override // ModuleElement
  public final boolean isOpen() {
    switch (this.getKind()) {
    case MODULE:
      return ((ModuleElement)this.delegate).isOpen();
    default:
      return false;
    }
  }

  @Override // ModuleElement, PackageElement
  public final boolean isUnnamed() {
    switch (this.getKind()) {
    case MODULE:
      return ((ModuleElement)this.delegate).isUnnamed();
    case PACKAGE:
      return ((PackageElement)this.delegate).isUnnamed();
    default:
      return false;
    }
  }

  @Override // ExecutableElement
  public final boolean isVarArgs() {
    switch (this.getKind()) {
    case CONSTRUCTOR:
    case METHOD:
      return ((ExecutableElement)this.delegate).isVarArgs();
    default:
      return false;
    }
  }

  @Override // Element
  public final int hashCode() {
    return this.ehc.hashCode(this);
  }

  @Override // Element
  public final boolean equals(final Object other) {
    if (this == other) {
      return true;
    } else if (other instanceof Element e) { // instanceof on purpose
      return this.ehc.equals(this, e);
    } else {
      return false;
    }
  }

  @Override // Element
  public final String toString() {
    return this.delegate.toString();
  }


  /*
   * Static methods.
   */


  public static final List<DelegatingElement> of(final Collection<? extends Element> es, final ElementSource elementSource) {
    return of(es, elementSource, null);
  }

  public static final List<DelegatingElement> of(final Collection<? extends Element> es, final ElementSource elementSource, final Equality ehc) {
    final List<DelegatingElement> newEs = new ArrayList<>(es.size());
    for (final Element e : es) {
      newEs.add(of(e, elementSource, ehc));
    }
    return Collections.unmodifiableList(newEs);
  }

  public static final DelegatingElement of(final Element e, final ElementSource elementSource) {
    return of(e, elementSource, null);
  }

  public static final DelegatingElement of(final Element e,
                                           final ElementSource elementSource,
                                           final Equality ehc) {
    return
      e == null ? null :
      e instanceof DelegatingElement d ? d :
      new DelegatingElement(e, elementSource, ehc);
  }

  public static final Element unwrap(Element e) {
    while (e instanceof DelegatingElement de) {
      e = de.delegate();
    }
    return e;
  }

  private static final void doNothing() {}

}
