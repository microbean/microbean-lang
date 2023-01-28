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

import java.lang.annotation.Annotation;

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
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.TypeMirror;

import org.microbean.lang.Equality;

import org.microbean.lang.type.DelegatingTypeMirror;
import org.microbean.lang.type.NoType;

public final class DelegatingElement implements ExecutableElement, ModuleElement, PackageElement, RecordComponentElement, TypeElement, TypeParameterElement, VariableElement {


  /*
   * Instance fields.
   */


  private final Element delegate;

  private final Equality ehc;
  
  
  /*
   * Constructors.
   */


  private DelegatingElement(final Element delegate, final Equality ehc) {
    super();
    this.delegate = Objects.requireNonNull(delegate, "delegate");
    this.ehc = ehc == null ? new Equality(true) : ehc;
  }


  /*
   * Instance methods.
   */


  @Override // Element
  public final <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    return this.delegate.accept(v, p);
  }

  @Override // Element
  public final TypeMirror asType() {
    return this.delegate.asType(); // TODO: maybe wrap with DelegatingTypeMirror?
  }

  public final Element delegate() {
    return this.delegate;
  }

  @Override // RecordComponentElement
  public final ExecutableElement getAccessor() {
    switch (this.delegate.getKind()) {
    case RECORD_COMPONENT:
      return ((RecordComponentElement)this.delegate).getAccessor();
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
    return this.delegate.getAnnotationMirrors();
  }

  @Override // Element
  public final <A extends Annotation> A[] getAnnotationsByType(final Class<A> annotationType) {
    return this.delegate.getAnnotationsByType(annotationType);
  }

  @Override // TypeParameterElement
  public final List<? extends TypeMirror> getBounds() {
    switch (this.delegate.getKind()) {
    case TYPE_PARAMETER:
      return ((TypeParameterElement)this.delegate).getBounds();
    default:
      return List.of();
    }
  }

  @Override // VariableElement
  public final Object getConstantValue() {
    switch (this.delegate.getKind()) {
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
    switch (this.delegate.getKind()) {
    case ANNOTATION_TYPE:
      return ((ExecutableElement)this.delegate).getDefaultValue();
    default:
      return null;
    }
  }

  @Override // ModuleElement
  public final List<? extends Directive> getDirectives() {
    switch (this.delegate.getKind()) {
    case MODULE:
      return ((ModuleElement)this.delegate).getDirectives();
    default:
      return List.of();
    }
  }

  @Override // Element
  public final List<? extends Element> getEnclosedElements() {
    return this.delegate.getEnclosedElements();
  }

  @Override // Element
  public final Element getEnclosingElement() {
    return this.delegate.getEnclosingElement();
  }

  @Override // TypeParameterElement
  public final Element getGenericElement() {
    switch (this.delegate.getKind()) {
    case TYPE_PARAMETER:
      return ((TypeParameterElement)this.delegate).getGenericElement();
    default:
      return null; // illegal state
    }
  }

  @Override // TypeElement
  public final List<? extends TypeMirror> getInterfaces() {
    switch (this.delegate.getKind()) {
    case ANNOTATION_TYPE:
    case CLASS:
    case ENUM:
    case INTERFACE:
    case RECORD:
      return ((TypeElement)this.delegate).getInterfaces();
    default:
      return List.of();
    }
  }

  @Override // Element
  public final ElementKind getKind() {
    return this.delegate.getKind();
  }

  @Override // Element
  public final Set<Modifier> getModifiers() {
    return this.delegate.getModifiers();
  }

  @Override // TypeElement
  public final NestingKind getNestingKind() {
    switch (this.delegate.getKind()) {
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
    switch (this.delegate.getKind()) {
    case CONSTRUCTOR:
    case METHOD:
      return ((ExecutableElement)this.delegate).getParameters();
    default:
      return List.of();
    }
  }

  @Override // ModuleElement, PackageElement, TypeElement
  public final Name getQualifiedName() {
    switch (this.delegate.getKind()) {
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
    return DelegatingTypeMirror.of(this.asType()).getReceiverType();
  }

  @Override // ExecutableElement
  public final TypeMirror getReturnType() {
    return DelegatingTypeMirror.of(this.asType()).getReturnType();
  }

  @Override // Element
  public final Name getSimpleName() {
    return this.delegate.getSimpleName();
  }

  @Override // TypeElement
  public final TypeMirror getSuperclass() {
    switch (this.delegate.getKind()) {
    case ANNOTATION_TYPE:
    case CLASS:
    case ENUM:
    case INTERFACE:
    case RECORD:
      return ((TypeElement)this.delegate).getSuperclass();
    default:
      return NoType.NONE;
    }
  }

  @Override // ExecutableElement
  public final List<? extends TypeMirror> getThrownTypes() {
    return DelegatingTypeMirror.of(this.asType()).getThrownTypes();
  }

  @Override // ExecutableElement
  public final List<? extends TypeParameterElement> getTypeParameters() {
    switch (this.delegate.getKind()) {
    case CONSTRUCTOR:
    case METHOD:
      return ((ExecutableElement)this.delegate).getTypeParameters();
    default:
      return List.of();
    }
  }

  @Override // ExecutableElement
  public final boolean isDefault() {
    switch (this.delegate.getKind()) {
    case METHOD:
      return ((ExecutableElement)this.delegate).isDefault();
    default:
      return false;
    }
  }

  @Override // ModuleElement
  public final boolean isOpen() {
    switch (this.delegate.getKind()) {
    case MODULE:
      return ((ModuleElement)this.delegate).isOpen();
    default:
      return false;
    }
  }

  @Override // ModuleElement, PackageElement
  public final boolean isUnnamed() {
    switch (this.delegate.getKind()) {
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
    switch (this.delegate.getKind()) {
    case CONSTRUCTOR:
    case METHOD:
      return ((ExecutableElement)this.delegate).isVarArgs();
    default:
      return false;
    }
  }

  @Override // Element
  public final int hashCode() {
    return this.ehc.hashCode(this.delegate);
  }

  @Override // Element
  public final boolean equals(final Object other) {
    if (this == other) {
      return true;
    } else if (other instanceof Element e) { // instanceof on purpose
      return this.ehc.equals(this.delegate, e);
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


  public static final DelegatingElement of(final Element e) {
    return of(e, null);
  }

  public static final DelegatingElement of(final Element e, final Equality ehc) {
    return e instanceof DelegatingElement d ? d : new DelegatingElement(e, ehc);
  }

}
