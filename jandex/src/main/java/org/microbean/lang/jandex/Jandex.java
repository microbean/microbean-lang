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
package org.microbean.lang.jandex;

import java.util.List;
import java.util.Objects;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.TypeMirror;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassInfo.NestingType;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import org.microbean.lang.Modeler;

public final class Jandex extends Modeler {

  private final IndexView i;

  public Jandex(final IndexView i) {
    super();
    this.i = Objects.requireNonNull(i, "i");
  }

  public final TypeElement typeElement(final DotName n) {
    return (TypeElement)element(this.i.getClassByName(n));
  }

  public final TypeElement typeElement(final String n) {
    return (TypeElement)element(this.i.getClassByName(n));
  }

  public final Element element(final Object k) {
    Element r = this.elements.get(k);
    if (r == null) {
      return switch (k) {

      case ClassInfo ci when ci.kind() == AnnotationTarget.Kind.CLASS && !ci.isModule() -> element(ci, () -> new org.microbean.lang.element.TypeElement(kind(ci), nestingKind(ci)), this.elements::putIfAbsent, this::build);
      case ClassType c when c.kind() == Type.Kind.CLASS -> this.element(classInfoFor(c)); // RECURSIVE

      case org.jboss.jandex.TypeVariable t when t.kind() == Type.Kind.TYPE_VARIABLE -> element(t, org.microbean.lang.element.TypeParameterElement::new, this.elements::putIfAbsent, this::build);

      default -> throw new IllegalArgumentException("k: " + k + "; k.getClass(): " + k.getClass());
      };
    }
    return r;
  }

  public final TypeMirror type(final Object k) {
    TypeMirror r = this.types.get(k);
    if (r == null) {
      return switch (k) {

      case ArrayType a when a.kind() == Type.Kind.ARRAY -> type(a, org.microbean.lang.type.ArrayType::new, this.types::putIfAbsent, this::build);

      case ClassType c when c.kind() == Type.Kind.CLASS -> type(c, org.microbean.lang.type.DeclaredType::new, this.types::putIfAbsent, this::build);
      case ClassInfo ci when ci.kind() == AnnotationTarget.Kind.CLASS && !ci.isModule() -> type(ci, org.microbean.lang.type.DeclaredType::new, this.types::putIfAbsent, this::build);

      case org.jboss.jandex.TypeVariable t when t.kind() == Type.Kind.TYPE_VARIABLE -> type(t, org.microbean.lang.type.TypeVariable::new, this.types::putIfAbsent, this::build);

      default -> throw new IllegalArgumentException("k: " + k + "; k.getClass(): " + k.getClass());
      };
    }
    return r;
  }


  /*
   * Element builders.
   */


  private final void build(final ClassInfo ci, final org.microbean.lang.element.TypeElement e) {
    e.setSimpleName(ci.name().local());
    final org.microbean.lang.type.DeclaredType t = (org.microbean.lang.type.DeclaredType)this.type(ci);
    e.setType(t);
    t.setDefiningElement(e);
    throw new UnsupportedOperationException("TODO: finish implementing");
  }

  private final void build(final org.jboss.jandex.TypeVariable tp, final org.microbean.lang.element.TypeParameterElement e) {
    e.setSimpleName(tp.identifier());
    final org.microbean.lang.type.TypeVariable t = (org.microbean.lang.type.TypeVariable)this.type(tp);
    e.setType(t);
    t.setDefiningElement(e);
  }


  /*
   * Type builders.
   */


  private final void build(final ArrayType a, final org.microbean.lang.type.ArrayType t) {
    t.setComponentType(this.type(a));
  }

  private final void build(final ClassType c, final org.microbean.lang.type.DeclaredType t) {
    this.build(classInfoFor(c), t);
  }

  private final void build(final ClassInfo ci, final org.microbean.lang.type.DeclaredType t) {
    final org.microbean.lang.element.TypeElement e = (org.microbean.lang.element.TypeElement)element(ci);
    e.setType(t);
    t.setDefiningElement(e);

    // Need to do enclosing type, if there is one
    t.setEnclosingType(enclosingTypeFor(ci));

    // Now type arguments (which will be type variables), if there are any.
    for (final org.jboss.jandex.TypeVariable tp : ci.typeParameters()) {
      t.addTypeArgument(this.type(tp));
    }

    // TODO: *possibly* annotations, since a ClassInfo sort of represents both an element and a type.
  }

  private final void build(final org.jboss.jandex.TypeVariable tv, final org.microbean.lang.type.TypeVariable t) {
    final org.microbean.lang.element.TypeParameterElement e = (org.microbean.lang.element.TypeParameterElement)this.element(tv);
    e.setType(t);
    t.setDefiningElement(e);

    final List<? extends Type> bounds = tv.bounds();
    switch (bounds.size()) {
    case 0:
      break;
    case 1:
      t.setUpperBound(this.type(bounds.get(0)));
      break;
    default:
      final org.microbean.lang.type.IntersectionType upperBound = new org.microbean.lang.type.IntersectionType();
      for (final Type bound : bounds) {
        upperBound.addBound(this.type(bound));
      }
      t.setUpperBound(upperBound);
      break;
    }
    // TODO: annotations, maybe? since org.jboss.jandex.TypeVariable is both an element and a type (!) we need to make
    // sure we're considering only type use annotations
  }


  /*
   * Housekeeping.
   */


  private final ClassInfo classInfoFor(final ClassType t) {
    return classInfoFor(t.name());
  }

  private final ClassInfo classInfoFor(final DotName n) {
    return this.i.getClassByName(n);
  }

  private final ClassInfo classInfoFor(final String n) {
    return this.i.getClassByName(n);
  }

  private final org.microbean.lang.type.DeclaredType enclosingTypeFor(final ClassInfo ci) {
    final DotName ecn = ci.enclosingClass(); // its id
    if (ecn != null) {
      final ClassInfo enclosingClassInfo = this.i.getClassByName(ecn);
      if (enclosingClassInfo != null) {
        return (org.microbean.lang.type.DeclaredType)this.type(enclosingClassInfo);
      }
    }
    return null;
  }


  /*
   * Static methods.
   */



  private static final ElementKind kind(final ClassInfo ci) {
    if (ci.isAnnotation()) {
      return ElementKind.ANNOTATION_TYPE;
    } else if (ci.isEnum()) {
      return ElementKind.ENUM;
    } else if (ci.isInterface()) {
      return ElementKind.INTERFACE;
    } else if (ci.isModule()) {
      return ElementKind.MODULE;
    } else if (ci.isRecord()) {
      return ElementKind.RECORD;
    } else {
      return ElementKind.CLASS;
    }
  }

  private static final NestingKind nestingKind(final ClassInfo ci) {
    return nestingKind(ci.nestingType());
  }

  private static final NestingKind nestingKind(final NestingType n) {
    return switch (n) {
    case ANONYMOUS -> NestingKind.ANONYMOUS;
    case INNER -> NestingKind.MEMBER;
    case LOCAL -> NestingKind.LOCAL;
    case TOP_LEVEL -> NestingKind.TOP_LEVEL;
    };
  }
}
