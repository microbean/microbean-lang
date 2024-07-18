/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2022–2024 microBean™.
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
import java.util.Set;

import java.util.function.Supplier;

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
import org.microbean.lang.TypeAndElementSource;
import org.microbean.lang.Equality;

import org.microbean.lang.type.DelegatingTypeMirror;
import org.microbean.lang.type.NoType;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;
import static java.lang.constant.DirectMethodHandleDesc.Kind.STATIC;

import static org.microbean.lang.ConstantDescs.CD_DelegatingElement;
import static org.microbean.lang.ConstantDescs.CD_Equality;
import static org.microbean.lang.ConstantDescs.CD_TypeAndElementSource;
import static org.microbean.lang.ConstantDescs.CD_Element;

/**
 * An {@link Element} that implements all known {@link Element} subinterfaces and delegates to an underlying {@link
 * Element} for all operations.
 *
 * <p>This class is safe for concurrent use by multiple threads.</p>
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
public final class DelegatingElement
  implements Constable,
             ExecutableElement,
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


  private Supplier<Element> delegateSupplier;

  private final TypeAndElementSource tes;

  private final Equality ehc;


  /*
   * Constructors.
   */


  private DelegatingElement(final Element delegate,
                            final TypeAndElementSource tes,
                            final Equality ehc) {
    super();
    Objects.requireNonNull(delegate, "delegate");
    this.tes = Objects.requireNonNull(tes, "tes");
    this.ehc = ehc == null ? new Equality(true) : ehc;
    this.delegateSupplier = () -> {
      final Element unwrappedDelegate = unwrap(delegate);
      CompletionLock.acquire();
      try {
        // Eagerly complete
        unwrappedDelegate.getKind();
        this.delegateSupplier = () -> unwrappedDelegate;
      } finally {
        CompletionLock.release();
      }
      return unwrappedDelegate;
    };
  }


  /*
   * Instance methods.
   */


  @Override // Element
  public final <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    return switch (this.getKind()) {
    case
      ANNOTATION_TYPE,
      CLASS,
      ENUM,
      INTERFACE,
      RECORD                 -> v.visitType(this, p);
    case TYPE_PARAMETER      -> v.visitTypeParameter(this, p);
    case
      BINDING_VARIABLE,
      ENUM_CONSTANT,
      EXCEPTION_PARAMETER,
      FIELD, LOCAL_VARIABLE,
      PARAMETER,
      RESOURCE_VARIABLE      -> v.visitVariable(this, p);
    case RECORD_COMPONENT    -> v.visitRecordComponent(this, p);
    case
      CONSTRUCTOR,
      INSTANCE_INIT,
      METHOD,
      STATIC_INIT            -> v.visitExecutable(this, p);
    case PACKAGE             -> v.visitPackage(this, p);
    case MODULE              -> v.visitModule(this, p);
    case OTHER               -> v.visitUnknown(this, p);
    };
  }

  @Override // Element
  public final TypeMirror asType() {
    return DelegatingTypeMirror.of(this.delegate().asType(), this.tes, this.ehc);
  }

  public final Element delegate() {
    return this.delegateSupplier.get();
  }

  @Override // Constable
  public final Optional<? extends ConstantDesc> describeConstable() {
    return this.ehc.describeConstable()
      .flatMap(ehcDesc -> (this.tes instanceof Constable c ? c.describeConstable() : Optional.<ConstantDesc>empty())
               .flatMap(tesDesc -> this.tes.describeConstable(this.delegate())
                        .map(delegateDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                                    MethodHandleDesc.ofMethod(STATIC,
                                                                                              CD_DelegatingElement,
                                                                                              "of",
                                                                                              MethodTypeDesc.of(CD_DelegatingElement,
                                                                                                                CD_Element,
                                                                                                                CD_TypeAndElementSource,
                                                                                                                CD_Equality)),
                                                                    delegateDesc,
                                                                    tesDesc,
                                                                    ehcDesc))));
  }

  @Override // RecordComponentElement
  public final ExecutableElement getAccessor() {
    return switch (this.getKind()) {
    case RECORD_COMPONENT -> this.wrap(((RecordComponentElement)this.delegate()).getAccessor());
    default               -> null;
    };
  }

  @Override // Element
  public final <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
    return this.delegate().getAnnotation(annotationType); // TODO: wrap?
  }

  @Override // Element
  public final List<? extends AnnotationMirror> getAnnotationMirrors() {
    return this.delegate().getAnnotationMirrors(); // TOOD: wrap?
  }

  @Override // Element
  public final <A extends Annotation> A[] getAnnotationsByType(final Class<A> annotationType) {
    return this.delegate().getAnnotationsByType(annotationType); // TODO: wrap?
  }

  @Override // TypeParameterElement
  public final List<? extends TypeMirror> getBounds() {
    return switch (this.getKind()) {
    case TYPE_PARAMETER ->
      DelegatingTypeMirror.of(((TypeParameterElement)this.delegate()).getBounds(), this.tes, this.ehc);
    default -> List.of();
    };
  }

  @Override // VariableElement
  public final Object getConstantValue() {
    return switch (this.getKind()) {
    case BINDING_VARIABLE, ENUM_CONSTANT, EXCEPTION_PARAMETER, FIELD, LOCAL_VARIABLE, PARAMETER, RESOURCE_VARIABLE ->
      ((VariableElement)this.delegate()).getConstantValue();
    default -> null;
    };
  }

  @Override // ExecutableElement
  public final AnnotationValue getDefaultValue() {
    return switch (this.getKind()) {
    case ANNOTATION_TYPE ->
      // TODO: delegating annotation value? could be a type mirror after all
      ((ExecutableElement)this.delegate()).getDefaultValue();
    default -> null;
    };
  }

  @Override // ModuleElement
  public final List<? extends Directive> getDirectives() {
    return switch (this.getKind()) {
    case MODULE -> ((ModuleElement)this.delegate()).getDirectives(); // TODO: wrap? Probably not necessary
    default     -> List.of();
    };
  }

  @Override // Element
  public final List<? extends Element> getEnclosedElements() {
    return this.wrap(this.delegate().getEnclosedElements());
  }

  @Override // Element
  public final Element getEnclosingElement() {
    return this.wrap(this.delegate().getEnclosingElement());
  }

  @Override // TypeParameterElement
  public final Element getGenericElement() {
    return switch (this.getKind()) {
    case TYPE_PARAMETER -> this.wrap(((TypeParameterElement)this.delegate()).getGenericElement());
    default             -> null; // illegal state
    };
  }

  @Override // TypeElement
  public final List<? extends TypeMirror> getInterfaces() {
    return switch (this.getKind()) {
    case ANNOTATION_TYPE, CLASS, ENUM, INTERFACE, RECORD ->
      DelegatingTypeMirror.of(((TypeElement)this.delegate()).getInterfaces(), this.tes, this.ehc);
    default -> List.of();
    };
  }

  @Override // Element
  public final ElementKind getKind() {
    return this.delegate().getKind();
  }

  @Override // Element
  public final Set<Modifier> getModifiers() {
    return this.delegate().getModifiers();
  }

  @Override // TypeElement
  public final NestingKind getNestingKind() {
    return switch (this.getKind()) {
    case ANNOTATION_TYPE, CLASS, ENUM, INTERFACE, RECORD -> ((TypeElement)this.delegate()).getNestingKind();
    default                                              -> null;
    };
  }

  @Override // ExecutableElement
  public final List<? extends VariableElement> getParameters() {
    return switch (this.getKind()) {
    case CONSTRUCTOR, METHOD -> this.wrap(((ExecutableElement)this.delegate()).getParameters());
    default                  -> List.of();
    };
  }

  @Override // ModuleElement, PackageElement, TypeElement
  public final Name getQualifiedName() {
    return switch (this.getKind()) {
    case ANNOTATION_TYPE, CLASS, ENUM, INTERFACE, MODULE, PACKAGE, RECORD ->
      org.microbean.lang.element.Name.of(((QualifiedNameable)this.delegate()).getQualifiedName());
    default -> org.microbean.lang.element.Name.of();
    };
  }

  @Override // ExecutableElement
  public final TypeMirror getReceiverType() {
    return
      this.getKind().isExecutable() ?
      DelegatingTypeMirror.of(this.asType(), this.tes, this.ehc).getReceiverType() :
      null;
  }

  @Override // TypeElement
  public final List<? extends RecordComponentElement> getRecordComponents() {
    return switch (this.getKind()) {
    case RECORD -> this.wrap(((TypeElement)this.delegate()).getRecordComponents());
    default     -> List.of();
    };
  }

  @Override // ExecutableElement
  public final TypeMirror getReturnType() {
    return DelegatingTypeMirror.of(this.asType(), this.tes, this.ehc).getReturnType();
  }

  @Override // Element
  public final Name getSimpleName() {
    return org.microbean.lang.element.Name.of(this.delegate().getSimpleName());
  }

  @Override // TypeElement
  public final TypeMirror getSuperclass() {
    return switch (this.getKind()) {
    case ANNOTATION_TYPE, CLASS, ENUM, INTERFACE, RECORD ->
      DelegatingTypeMirror.of(((TypeElement)this.delegate()).getSuperclass(), this.tes, this.ehc);
    default -> NoType.NONE;
    };
  }

  @Override // ExecutableElement
  public final List<? extends TypeMirror> getThrownTypes() {
    return
      this.getKind().isExecutable() ?
      DelegatingTypeMirror.of(((ExecutableElement)this.delegate()).getThrownTypes(), this.tes, this.ehc) :
      List.of();
  }

  @Override // ExecutableElement
  public final List<? extends TypeParameterElement> getTypeParameters() {
    return switch (this.getKind()) {
    case CLASS, CONSTRUCTOR, ENUM, INTERFACE, RECORD, METHOD -> this.wrap(((Parameterizable)this.delegate()).getTypeParameters());
    default                                                  -> List.of();
    };
  }

  @Override // ExecutableElement
  public final boolean isDefault() {
    return switch (this.getKind()) {
    case METHOD -> ((ExecutableElement)this.delegate()).isDefault();
    default     -> false;
    };
  }

  @Override // ModuleElement
  public final boolean isOpen() {
    return switch (this.getKind()) {
    case MODULE -> ((ModuleElement)this.delegate()).isOpen();
    default     -> false;
    };
  }

  @Override // ModuleElement, PackageElement
  public final boolean isUnnamed() {
    return switch (this.getKind()) {
    case MODULE  -> ((ModuleElement)this.delegate()).isUnnamed();
    case PACKAGE -> ((PackageElement)this.delegate()).isUnnamed();
    default      -> this.getSimpleName().isEmpty();
    };
  }

  @Override // ExecutableElement
  public final boolean isVarArgs() {
    return switch (this.getKind()) {
    case CONSTRUCTOR, METHOD -> ((ExecutableElement)this.delegate()).isVarArgs();
    default                  -> false;
    };
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
    return this.delegate().toString();
  }

  private final DelegatingElement wrap(final Element e) {
    return of(e, this.tes, this.ehc);
  }

  private final List<DelegatingElement> wrap(final Collection<? extends Element> es) {
    return of(es, this.tes, this.ehc);
  }


  /*
   * Static methods.
   */


  public static final List<DelegatingElement> of(final Collection<? extends Element> es, final TypeAndElementSource tes) {
    return of(es, tes, null);
  }

  public static final List<DelegatingElement> of(final Collection<? extends Element> es, final TypeAndElementSource tes, final Equality ehc) {
    final List<DelegatingElement> newEs = new ArrayList<>(es.size());
    for (final Element e : es) {
      newEs.add(of(e, tes, ehc));
    }
    return Collections.unmodifiableList(newEs);
  }

  public static final DelegatingElement of(final Element e, final TypeAndElementSource tes) {
    return of(e, tes, null);
  }

  public static final DelegatingElement of(final Element e,
                                           final TypeAndElementSource tes,
                                           final Equality ehc) {
    return
      e == null ? null :
      e instanceof DelegatingElement d ? d :
      new DelegatingElement(e, tes, ehc);
  }

  @SuppressWarnings("unchecked")
  public static final <E extends Element> E unwrap(E e) {
    while (e instanceof DelegatingElement de) {
      e = (E)de.delegate();
    }
    return e;
  }

  private static final void doNothing() {}

}
