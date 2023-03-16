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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import java.util.function.Function;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.QualifiedNameable;

import javax.lang.model.type.TypeMirror;

import org.microbean.lang.AnnotatedConstruct;

import org.microbean.lang.type.NoType;

import org.microbean.lang.type.Types;

// NOT thread safe
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

  // Treat as effectively final, please.
  private List<javax.lang.model.element.Element> enclosedElements;

  // Treat as effectively final, please.
  private List<javax.lang.model.element.Element> unmodifiableEnclosedElements;

  private javax.lang.model.element.Element enclosingElement;

  private EnclosedElementsGenerator enclosedElementsFunction;
  
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
    if (type == null) {
      return NoType.NONE;
    }
    type.getKind(); // null check
    return type;
  }

  @Override // Element
  public final List<? extends javax.lang.model.element.Element> getEnclosedElements() {
    if (this.unmodifiableEnclosedElements == null) {
      // First time through.
      if (this.enclosedElements == null) {
        // No one has added any enclosed element by hand yet.
        if (this.enclosedElementsFunction == null) {
          // There's no generator.
          this.enclosedElements = new ArrayList<>();
          this.unmodifiableEnclosedElements = Collections.unmodifiableList(this.enclosedElements);
        } else {
          // There's a generator.
          final List<? extends javax.lang.model.element.Element> generatedElements = this.enclosedElementsFunction.generate(List.of());
          final List<javax.lang.model.element.Element> es = generatedElements == null ? List.of() : List.copyOf(generatedElements);
          for (final javax.lang.model.element.Element e : es) {
            ((Encloseable)this.validateEnclosedElement(e)).setEnclosingElement(this);
          }
          this.unmodifiableEnclosedElements = es;
        }
      } else if (this.enclosedElementsFunction == null) {
        // There are manually added enclosed elements, and no generator.
        this.unmodifiableEnclosedElements = Collections.unmodifiableList(this.enclosedElements);
      } else {
        // There are both manually added enclosed elements and a generator.
        final List<javax.lang.model.element.Element> unmodifiableEnclosedElements = Collections.unmodifiableList(this.enclosedElements);
        final List<? extends javax.lang.model.element.Element> generatedElements = this.enclosedElementsFunction.generate(unmodifiableEnclosedElements);
        if (generatedElements == null || generatedElements.isEmpty()) {
          this.unmodifiableEnclosedElements = unmodifiableEnclosedElements;
        } else {
          final List<javax.lang.model.element.Element> es = new ArrayList<>(this.enclosedElements.size() + generatedElements.size());
          final Iterator<? extends javax.lang.model.element.Element> i = this.enclosedElements.iterator();
          while (i.hasNext()) {
            es.add(i.next());
            i.remove();
          }
          es.addAll(generatedElements);
          this.unmodifiableEnclosedElements = Collections.unmodifiableList(es);
        }
      }
    }
    return this.unmodifiableEnclosedElements;
  }

  public final void setEnclosedElementsFunction(final EnclosedElementsGenerator f) {
    if (this.enclosedElementsFunction == null) {
      this.enclosedElementsFunction = Objects.requireNonNull(f, "f");
    } else if (this.enclosedElementsFunction != f) {
      throw new IllegalStateException();
    }
  }
  
  public final <E extends javax.lang.model.element.Element & Encloseable> void addEnclosedElement(final E e) {
    if (this.enclosedElementsFunction != null) {
      throw new IllegalStateException();
    }
    this.validateEnclosedElement(e).setEnclosingElement(this);
    if (this.enclosedElements == null) {      
      this.enclosedElements = new ArrayList<>();
    }
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
        if (enclosingElement != this) {
          if (canEnclose(enclosingElement.getKind(), this.getKind())) {
            this.enclosingElement = enclosingElement;
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


  public static final boolean canEnclose(ElementKind k1, ElementKind k2) {
    switch (k1) {
    case MODULE:
      switch (k2) {
      case PACKAGE:
        return true;
      default:
        break;
      }
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
    case ENUM:
      switch (k2) {
      case ANNOTATION_TYPE:
      case CLASS:
      case CONSTRUCTOR:
      case ENUM:
      case ENUM_CONSTANT:
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

  /**
   * A generator of <em>enclosed elements</em>, where an enclosed element is one described by the {@linkplain
   * javax.lang.model.element.Element#getEnclosedElements() documentation of the
   * <code>javax.lang.model.element.Element#getEnclosedElements()</code> method}.
   *
   * @author <a href="https://about.me/lairdnelson" target="_parent">Laird Nelson</a>
   *
   * @see #generate(List)
   */
  @FunctionalInterface
  public static interface EnclosedElementsGenerator {

    /**
     * Generates a determinate, unchanging and unmodifiable {@link List} of {@linkplain
     * javax.lang.model.element.Element#getEnclosedElements() enclosed elements}.
     *
     * @param es elements that have already been enclosed and should not appear in the returned {@link List}; must not
     * be {@code null}
     *
     * @return a determinate, unchanging and unmodifiable {@link List} of {@linkplain
     * javax.lang.model.element.Element#getEnclosedElements() enclosed elements}; never {@code null}
     *
     * @exception NullPointerException if {@code es} is {@code null}
     */
    public <E extends javax.lang.model.element.Element & Encloseable> List<? extends E> generate(final List<? extends javax.lang.model.element.Element> es);
    
  }

}
