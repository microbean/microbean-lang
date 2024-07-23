/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2022–2024 microBean™.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
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
import javax.lang.model.element.QualifiedNameable;

import javax.lang.model.type.TypeMirror;

import org.microbean.lang.AnnotatedConstruct;

// NOT thread safe
public abstract sealed class Element
  extends AnnotatedConstruct
  implements javax.lang.model.element.Element, Encloseable, Encloser
  permits ModuleElement,
          PackageElement,
          Parameterizable,
          RecordComponentElement,
          TypeParameterElement,
          VariableElement {

  private final List<javax.lang.model.element.Element> enclosedElements;

  // Treat as effectively final, please.
  private List<javax.lang.model.element.Element> unmodifiableEnclosedElements;

  private javax.lang.model.element.Element enclosingElement;

  private Runnable enclosedElementsGenerator;

  private final ElementKind kind;

  private final Set<Modifier> modifiers;

  private final Set<Modifier> unmodifiableModifiers;

  private Name simpleName;

  private TypeMirror type;

  protected Element(final ElementKind kind) {
    super();
    this.kind = this.validateKind(kind);
    this.modifiers = new LinkedHashSet<>();
    this.unmodifiableModifiers = Collections.unmodifiableSet(this.modifiers);
    this.enclosedElements = new ArrayList<>();
  }

  @Override // Element
  public <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
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
    return this.type;
  }

  public final void setType(final TypeMirror type) {
    final TypeMirror old = this.asType();
    if (old == null) {
      if (type != null) {
        this.type = this.validateType(type);
      }
    } else if (old != type) {
      throw new IllegalStateException("Type already set; element: " + this + "; old type: " + old + "; new type: " + type + "; old.equals(new)? " + old.equals(type));
    }
  }

  protected TypeMirror validateType(final TypeMirror type) {
    return type;
  }

  @Override // Element
  public final List<? extends javax.lang.model.element.Element> getEnclosedElements() {
    if (this.unmodifiableEnclosedElements == null) {
      this.unmodifiableEnclosedElements = Collections.unmodifiableList(this.enclosedElements);
      if (this.enclosedElementsGenerator != null) {
        final List<? extends javax.lang.model.element.Element> existing = List.copyOf(this.unmodifiableEnclosedElements);
        this.enclosedElementsGenerator.run();
        this.enclosedElements.removeIf(existing::contains);
        this.enclosedElements.addAll(existing);
      }
    }
    return this.unmodifiableEnclosedElements;
  }

  public final void setEnclosedElementsGenerator(final Runnable f) {
    if (this.enclosedElementsGenerator == null) {
      this.enclosedElementsGenerator = Objects.requireNonNull(f, "f");
    } else if (this.enclosedElementsGenerator != f) {
      throw new IllegalStateException();
    }
  }

  // Deliberately not final to permit subclasses to override to throw UnsupportedOperationException.
  @Override // Encloser
  public <E extends javax.lang.model.element.Element & Encloseable> void addEnclosedElement(final E e) {
    this.validateEnclosedElement(e).setEnclosingElement(this);
    this.enclosedElements.add(e);
  }

  public final <E extends javax.lang.model.element.Element & Encloseable> void addEnclosedElements(final Iterable<? extends E> es) {
    for (final E e : es) {
      this.addEnclosedElement(e);
    }
  }

  protected <E extends javax.lang.model.element.Element> E validateEnclosedElement(final E e) {
    if (!canEnclose(this.getKind(), e.getKind())) {
      throw new IllegalArgumentException(this.getKind() + " cannot enclose " + e.getKind());
    }
    return e;
  }

  @Override // Element
  public final ElementKind getKind() {
    return this.kind;
  }

  protected ElementKind validateKind(final ElementKind kind) {
    return Objects.requireNonNull(kind, "kind");
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
    final int length = n.length();
    switch (length) {
    case 0:
      // Anonymous class. Should we even be defining an element here?
      break;
    default:
      if (n.charAt(0) == '.' || n.charAt(n.length() - 1) == '.') {
        throw new IllegalArgumentException("n: " + n);
      }
      break;
    }
    return n;
  }

  public boolean isUnnamed() {
    return this.getSimpleName().length() <= 0;
  }

  // Deliberately not final; ModuleElement and TypeParameterElement need to override this.
  @Override // Element
  public javax.lang.model.element.Element getEnclosingElement() {
    return this.enclosingElement;
  }

  // Deliberately not final; ModuleElement and TypeParameterElement need to override this.
  @Override // Encloseable
  public void setEnclosingElement(final javax.lang.model.element.Element enclosingElement) {
    final Object old = this.getEnclosingElement();
    if (old == null) {
      if (enclosingElement != null) {
        if (enclosingElement != this) {
          if (canEnclose(enclosingElement.getKind(), this.getKind())) {
            this.enclosingElement = enclosingElement;
            if (enclosingElement instanceof Encloser e) {
              e.addEnclosedElement(this);
            }
          } else {
            throw new IllegalArgumentException(enclosingElement.getKind() + " cannot enclose " + this.getKind());
          }
        } else {
          throw new IllegalArgumentException("enclosingElement: " + enclosingElement);
        }
      }
    } else if (old != enclosingElement) {
      throw new IllegalStateException("old != enclosingElement: " + old + " != " + enclosingElement);
    }
  }

  @Override
  public String toString() {
    final CharSequence n = this instanceof QualifiedNameable q ? q.getQualifiedName() : this.getSimpleName();
    return this.isUnnamed() ? "<unnamed>" : n == null ? "<unknown>" : n.length() <= 0 ? "<unnamed>" : n.toString();
  }


  /*
   * Static methods.
   */


  public static final boolean canEnclose(final ElementKind k1, final ElementKind k2) {
    return switch (k1) {
    case MODULE -> k2 == ElementKind.PACKAGE;
    case PACKAGE -> switch (k2) { case ANNOTATION_TYPE, CLASS, ENUM, INTERFACE, RECORD -> true; default -> false; };
    case ANNOTATION_TYPE, CLASS, INTERFACE -> switch (k2) { case ANNOTATION_TYPE, CLASS, CONSTRUCTOR, ENUM, FIELD, INSTANCE_INIT, INTERFACE, METHOD, RECORD, STATIC_INIT -> true; default -> false; };
    case ENUM -> switch (k2) { case ANNOTATION_TYPE, CLASS, CONSTRUCTOR, ENUM, ENUM_CONSTANT, FIELD, INSTANCE_INIT, INTERFACE, METHOD, RECORD, STATIC_INIT -> true; default -> false; };
    case RECORD -> switch (k2) { case ANNOTATION_TYPE, CLASS, CONSTRUCTOR, ENUM, FIELD, INSTANCE_INIT, INTERFACE, METHOD, RECORD, RECORD_COMPONENT, STATIC_INIT -> true; default -> false; };
    default -> false;
    };
  }

}
