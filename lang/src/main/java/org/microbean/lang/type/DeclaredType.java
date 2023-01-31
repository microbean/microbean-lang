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
package org.microbean.lang.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;

public sealed class DeclaredType extends DefineableType<TypeElement> implements javax.lang.model.type.DeclaredType
  permits ErrorType {


  /*
   * Instance fields.
   */


  private TypeMirror enclosingType;

  // ArrayType, DeclaredType, ErrorType, TypeVariable, WildcardType
  private final List<TypeMirror> typeArguments;

  private final List<TypeMirror> unmodifiableTypeArguments;

  // See
  // https://github.com/openjdk/jdk/blob/jdk-20+11/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Type.java#L1197-L1200
  private Boolean erased;


  /*
   * Constructors.
   */


  public DeclaredType() {
    super(TypeKind.DECLARED);
    this.typeArguments = new ArrayList<>(5);
    this.unmodifiableTypeArguments = Collections.unmodifiableList(this.typeArguments);
  }

  public DeclaredType(final boolean erased) {
    this();
    this.setErased(erased);
  }

  DeclaredType(final TypeKind kind, final boolean erased) {
    super(kind);
    this.typeArguments = new ArrayList<>(5);
    this.unmodifiableTypeArguments = Collections.unmodifiableList(this.typeArguments);
    this.setErased(erased);
  }


  /*
   * Instance methods.
   */


  @Override // TypeMirror
  public <R, P> R accept(final TypeVisitor<R, P> v, P p) {
    return v.visitDeclared(this, p);
  }

  @Override
  protected TypeKind validateKind(final TypeKind kind) {
    switch (kind) {
    case DECLARED:
      return kind;
    default:
      throw new IllegalArgumentException("kind: " + kind);
    }
  }

  @Override // DefineableType<TypeElement>
  protected TypeElement validateDefiningElement(final TypeElement e) {
    switch (e.getKind()) {
    case ANNOTATION_TYPE:
    case CLASS:
    case ENUM:
    case INTERFACE:
    case RECORD:
      final javax.lang.model.type.DeclaredType elementType = (javax.lang.model.type.DeclaredType)e.asType();
      if (elementType == null) {
        // TODO: may be too aggressive. Certain Elements (like TypeElements and TypeVariableElements) and certain
        // TypeMirrors (like DeclaredType and TypeVariable) have circular dependencies.  Logically speaking whether such
        // a type precedes such an element or vice versa is a matter of taste. Given that all Elements have an asType()
        // method, but not all TypeMirrors have an asElement() method, it would seem that to build an Element you need a
        // TypeMirror.
        //
        // Yes, here we would like after all is said and done for the defining TypeElement to have, as its type, this
        // very DeclaredType.
        throw new IllegalArgumentException("e: " + e + "; e.asType() == null");
      } else if (this != elementType) {
        // We are a parameterized type, i.e. a type usage, i.e. the-type-denoted-by-Set<String>
        // vs. the-type-denoted-by-Set<E>
        final int size = this.getTypeArguments().size();
        if (size > 0 && size != elementType.getTypeArguments().size()) {
          // We aren't a raw type (size > 0) and our type arguments aren't of the required size, so someone passed a bad
          // defining element.
          throw new IllegalArgumentException("e: " + e);
        }
      }
      break;
    default:
      throw new IllegalArgumentException("e: " + e);
    }
    return e;
  }

  public final boolean isErased() {
    final Boolean erased = this.erased;
    return erased == null ? false : erased;
  }

  public final void setErased(final boolean b) {
    final Boolean old = this.erased;
    if (old == null) {
      this.erased = b;
    } else if (!old.equals(Boolean.valueOf(b))) {
      throw new IllegalStateException();
    }
  }

  @Override // DeclaredType
  public final TypeMirror getEnclosingType() {
    return this.enclosingType;
  }

  public final void setEnclosingType(final TypeMirror enclosingType) {
    final Object old = this.getEnclosingType();
    if (old == null) {
      if (enclosingType != null) {
        this.enclosingType = this.validateEnclosingType(enclosingType);
      }
    } else if (old != enclosingType) {
      throw new IllegalStateException();
    }
  }

  private final TypeMirror validateEnclosingType(final TypeMirror t) {
    if (t == null) {
      return NoType.NONE;
    } else if (t == this) {
      throw new IllegalArgumentException("t: " + t);
    }
    switch (t.getKind()) {
    case DECLARED:
    case NONE:
      return t;
    default:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  @Override // DeclaredType
  public final List<? extends TypeMirror> getTypeArguments() {
    return this.unmodifiableTypeArguments;
  }

  public final void addTypeArgument(final TypeMirror t) {
    this.typeArguments.add(this.validateTypeArgument(t));
  }

  public final void addTypeArguments(final Iterable<? extends TypeMirror> ts) {
    for (final TypeMirror t : ts) {
      this.addTypeArgument(t);
    }
  }

  private final TypeMirror validateTypeArgument(final TypeMirror t) {
    if (t == this) {
      throw new IllegalArgumentException("t: " + t);
    } else if (this.isErased()) {
      throw new IllegalStateException("this.isErased()");
    }
    /*
    final Parameterizable definingElement = this.asElement();
    if (definingElement != null && definingElement.getTypeParameters().isEmpty()) {
      throw new IllegalArgumentException("No corresponding type parameter for type argument t (" + t + ")");
    }
    */
    /*
    if (definingElement == null) {
      throw new IllegalStateException("this.asElement() == null; no way to divine type parameters");
    } else if (definingElement.getTypeParameters().isEmpty()) {
      throw new IllegalArgumentException("No corresponding type parameter for type argument t (" + t + ")");
    }
    */

    switch (t.getKind()) {
    case ARRAY:
    case DECLARED:
      // case INTERSECTION: // JLS says reference types and wildcards only
    case TYPEVAR:
    case WILDCARD:
      break;
    default:
      throw new IllegalArgumentException("t: " + t);
    }
    return t;
  }

  @Override
  public String toString() {
    final Object element = this.asElement();
    return element == null ? "(undefined type; arguments: " + this.getTypeArguments() + ")" : element.toString();
  }

}
