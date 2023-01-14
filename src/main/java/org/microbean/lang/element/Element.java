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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;

import javax.lang.model.type.TypeMirror;

import org.microbean.lang.AnnotatedConstruct;

import org.microbean.lang.type.NoType;

import org.microbean.lang.type.Types;

public abstract sealed class Element
  extends AnnotatedConstruct
  implements javax.lang.model.element.Element, Encloseable
  permits ModuleElement,
          PackageElement,
          Parameterizable,
          RecordComponentElement,
          TypeParameterElement,
          VariableElement
{

  private final List<javax.lang.model.element.Element> enclosedElements;

  private final List<javax.lang.model.element.Element> unmodifiableEnclosedElements;

  private javax.lang.model.element.Element enclosingElement;

  private final ElementKind kind;

  private final Set<Modifier> modifiers;

  private final Set<Modifier> unmodifiableModifiers;

  private Name simpleName;

  private TypeMirror type;

  protected Element(final Element enclosingElement, final ElementKind kind, final Name simpleName, final TypeMirror type) {
    this(kind, simpleName, type);
    this.setEnclosingElement(enclosingElement);
  }

  protected Element(final ElementKind kind, final Name simpleName, final TypeMirror type) {
    this(kind, simpleName);
    this.setType(type);
  }

  protected Element(final ElementKind kind, final Name simpleName) {
    this(kind);
    this.setSimpleName(simpleName);
  }

  protected Element(final ElementKind kind) {
    super();
    this.kind = this.validateKind(kind);
    this.modifiers = new LinkedHashSet<>();
    this.unmodifiableModifiers = Collections.unmodifiableSet(this.modifiers);
    this.enclosedElements = new ArrayList<>();
    this.unmodifiableEnclosedElements = Collections.unmodifiableList(this.enclosedElements);
  }

  @Override // Element
  public <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    switch (this.getKind()) {

    case ANNOTATION_TYPE:
    case CLASS:
    case ENUM:
    case INTERFACE:
    case RECORD:
      return v.visitType((javax.lang.model.element.TypeElement)this, p);

    case TYPE_PARAMETER:
      return v.visitTypeParameter((javax.lang.model.element.TypeParameterElement)this, p);

    case BINDING_VARIABLE:
    case ENUM_CONSTANT:
    case EXCEPTION_PARAMETER:
    case FIELD:
    case LOCAL_VARIABLE:
    case PARAMETER:
    case RESOURCE_VARIABLE:
      return v.visitVariable((javax.lang.model.element.VariableElement)this, p);

    case RECORD_COMPONENT:
      return v.visitRecordComponent((javax.lang.model.element.RecordComponentElement)this, p);

    case CONSTRUCTOR:
    case INSTANCE_INIT:
    case METHOD:
    case STATIC_INIT:
      return v.visitExecutable((javax.lang.model.element.ExecutableElement)this, p);

    case PACKAGE:
      return v.visitPackage((javax.lang.model.element.PackageElement)this, p);

    case MODULE:
      return v.visitModule((javax.lang.model.element.ModuleElement)this, p);

    default:
      return v.visitUnknown(this, p);

    }
  }

  @Override // Element
  public final TypeMirror asType() {
    return this.type;
  }

  public final void setType(final TypeMirror type) {
    final TypeMirror old = this.asType();
    if (old == null) {
      if (type != null) {
        this.type = this.validateType(type);
      }
    } else if (old != type) {
      throw new IllegalStateException();
    }
  }

  protected TypeMirror validateType(final TypeMirror type) {
    if (type == null) {
      return NoType.NONE;
    }
    switch (type.getKind()) {
    case ARRAY:
    case BOOLEAN:
    case BYTE:
    case CHAR:
    case DECLARED:
    case DOUBLE:
    case ERROR:
    case EXECUTABLE:
    case FLOAT:
    case INT:
    case INTERSECTION:
    case LONG:
    case MODULE:
    case NONE:
    case NULL:
    case OTHER:
    case PACKAGE:
    case SHORT:
    case TYPEVAR:
    case UNION:
    case VOID:
    case WILDCARD:
      return type;
    default:
      throw new IllegalArgumentException("type: " + type);
    }
  }

  @Override // Element
  public final List<? extends javax.lang.model.element.Element> getEnclosedElements() {
    return this.unmodifiableEnclosedElements;
  }

  public final <E extends javax.lang.model.element.Element & Encloseable> void addEnclosedElement(final E e) {
    if (!canEnclose(this.getKind(), e.getKind())) {
      throw new IllegalArgumentException("e: " + e);
    }
    e.setEnclosingElement(this);
    this.enclosedElements.add(e);
  }

  public final <E extends javax.lang.model.element.Element & Encloseable> void addEnclosedElements(final Iterable<? extends E> es) {
    for (final E e : es) {
      this.addEnclosedElement(e);
    }
  }

  @Override // Element
  public final ElementKind getKind() {
    return this.kind;
  }

  protected ElementKind validateKind(final ElementKind kind) {
    switch (kind) {
    case ANNOTATION_TYPE:
    case BINDING_VARIABLE:
    case CLASS:
    case CONSTRUCTOR:
    case ENUM:
    case ENUM_CONSTANT:
    case EXCEPTION_PARAMETER:
    case FIELD:
    case INSTANCE_INIT:
    case INTERFACE:
    case LOCAL_VARIABLE:
    case METHOD:
    case MODULE:
    case OTHER:
    case PACKAGE:
    case PARAMETER:
    case RECORD:
    case RECORD_COMPONENT:
    case RESOURCE_VARIABLE:
    case STATIC_INIT:
    case TYPE_PARAMETER:
      return kind;
    default:
      throw new IllegalArgumentException("kind: " + kind);
    }
  }

  @Override // Element
  public final Set<Modifier> getModifiers() {
    return this.unmodifiableModifiers;
  }

  public final boolean addModifier(final Modifier modifier) {
    return this.modifiers.add(this.validateModifier(modifier));
  }

  public final void addModifiers(final Iterable<? extends Modifier> modifiers) {
    for (final Modifier modifier : modifiers) {
      this.addModifier(modifier);
    }
  }

  protected Modifier validateModifier(final Modifier modifier) {
    return Objects.requireNonNull(modifier, "modifier");
  }

  @Override // Element
  public final Name getSimpleName() {
    return this.simpleName;
  }

  public final void setSimpleName(final Name simpleName) {
    final Name old = this.getSimpleName();
    if (old == null) {
      if (simpleName != null) {
        this.simpleName = this.validateSimpleName(simpleName);
      }
    } else if (old != simpleName) {
      throw new IllegalStateException();
    }
  }

  public final void setSimpleName(final String simpleName) {
    this.setSimpleName(org.microbean.lang.element.Name.of(simpleName));
  }

  protected Name validateSimpleName(final Name n) {
    if (n.charAt(0) == '.' || n.charAt(n.length() - 1) == '.') {
      throw new IllegalArgumentException("n: " + n);
    }
    return n;
  }

  public boolean isUnnamed() {
    return this.getSimpleName().length() <= 0;
  }

  // Deliberately not final; ModuleElement adn TypeParameterElement need to override this.
  @Override // Element, Encloseable
  public javax.lang.model.element.Element getEnclosingElement() {
    return this.enclosingElement;
  }

  // Deliberately not final; ModuleElement and TypeParameterElement need to override this.
  @Override // Encloseable
  public void setEnclosingElement(final javax.lang.model.element.Element enclosingElement) {
    final Object old = this.getEnclosingElement();
    if (old == null) {
      if (enclosingElement != null) {
        if (enclosingElement != this && canEnclose(enclosingElement.getKind(), this.getKind())) {
          this.enclosingElement = enclosingElement;
        } else {
          throw new IllegalArgumentException("enclosingElement: " + enclosingElement);
        }
      }
    } else if (old != enclosingElement) {
      throw new IllegalStateException();
    }
  }


  /*
   * Static methods.
   */


  public static final boolean canEnclose(ElementKind k1, ElementKind k2) {
    switch (k1) {
    case MODULE:
      return k2 == ElementKind.PACKAGE;
    case PACKAGE:
      switch (k2) {
      case ANNOTATION_TYPE:
      case CLASS:
      case ENUM:
      case INTERFACE:
      case RECORD:
        return true;
      default:
        break;
      }
    case ANNOTATION_TYPE:
    case CLASS:
    case ENUM:
    case INTERFACE:
      switch (k2) {
      case ANNOTATION_TYPE:
      case CLASS:
      case CONSTRUCTOR:
      case ENUM:
      case FIELD:
      case INSTANCE_INIT:
      case INTERFACE:
      case METHOD:
      case RECORD:
      case STATIC_INIT:
        return true;
      default:
        break;
      }
    case RECORD:
      switch (k2) {
      case ANNOTATION_TYPE:
      case CLASS:
      case CONSTRUCTOR:
      case ENUM:
      case FIELD:
      case INSTANCE_INIT:
      case INTERFACE:
      case METHOD:
      case RECORD:
      case RECORD_COMPONENT:
      case STATIC_INIT:
        return true;
      default:
        break;
      }
    default:
      break;
    }
    return false;
  }

}
