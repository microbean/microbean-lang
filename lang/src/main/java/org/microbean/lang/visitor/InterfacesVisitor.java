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
package org.microbean.lang.visitor;

import java.util.List;
import java.util.Objects;

import java.util.function.Predicate;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import javax.lang.model.util.SimpleTypeVisitor14;

import org.microbean.lang.ElementSource;
import org.microbean.lang.Equality;

import org.microbean.lang.type.Types;

import static org.microbean.lang.type.Types.allTypeArguments;
import static org.microbean.lang.type.Types.isInterface;

// Basically done
public final class InterfacesVisitor extends SimpleTypeVisitor14<List<? extends TypeMirror>, Void> {

  private final ElementSource elementSource;
  
  private final Equality equality;

  private final Types types;

  private final EraseVisitor eraseVisitor;

  private final SupertypeVisitor supertypeVisitor;

  private final Predicate<? super TypeMirror> filter;

  public InterfacesVisitor(final ElementSource elementSource,
                           final Equality equality,
                           final Types types,
                           final EraseVisitor eraseVisitor,
                           final SupertypeVisitor supertypeVisitor, // used only for substitute visitor implementations
                           final Predicate<? super TypeMirror> filter) {
    super(List.of());
    this.filter = filter == null ? t -> true : filter;
    this.equality = equality == null ? new Equality(true) : equality;
    this.elementSource = Objects.requireNonNull(elementSource, "elementSource");
    this.types = Objects.requireNonNull(types, "types");
    this.eraseVisitor = Objects.requireNonNull(eraseVisitor, "eraseVisitor");
    this.supertypeVisitor = Objects.requireNonNull(supertypeVisitor, "supertypeVisitor");
  }

  public final InterfacesVisitor withFilter(final Predicate<? super TypeMirror> filter) {
    return
      filter == this.filter ? this :
      new InterfacesVisitor(this.elementSource, this.equality, this.types, this.eraseVisitor, this.supertypeVisitor, filter);
  }

  // https://github.com/openjdk/jdk/blob/jdk-20+12/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L2599-L2633
  @Override
  public final List<? extends TypeMirror> visitDeclared(final DeclaredType t, final Void x) {
    assert t.getKind() == TypeKind.DECLARED;
    final List<? extends TypeMirror> returnValue;
    final Element e = t.asElement();
    if (e == null) {
      returnValue = List.of();
    } else {
      returnValue = switch (e.getKind()) {
      case ANNOTATION_TYPE:
      case CLASS:
      case ENUM:
      case INTERFACE:
      case RECORD:
        final List<? extends TypeMirror> interfaces = ((TypeElement)e).getInterfaces();
        if (this.types.raw(t)) {
          yield this.eraseVisitor.visit(interfaces, true);
        }
        @SuppressWarnings("unchecked")
        final List<? extends TypeVariable> formals = (List<? extends TypeVariable>)allTypeArguments(e.asType());
        if (formals.isEmpty()) {
          yield interfaces;
        }
        assert this.supertypeVisitor.interfacesVisitor() == this;
        yield
          new SubstituteVisitor(this.elementSource, this.equality, this.supertypeVisitor, formals, allTypeArguments(t))
        .visit(interfaces, x);
      default:
        yield List.of();
      };
    }
    return returnValue.isEmpty() ? returnValue : returnValue.stream().filter(this.filter).toList();
  }

  @Override
  public final List<? extends TypeMirror> visitIntersection(final IntersectionType t, final Void x) {
    assert t.getKind() == TypeKind.INTERSECTION;
    // Here the porting is a little trickier.  It turns out that an intersection type caches its supertype and its
    // interfaces at construction time, and there's only one place where intersection types are created.  In the lang
    // model, that means that an IntersectionType's bounds are its single non-interface supertype, if any, followed by
    // its interfaces.  So we will hand-tool this.
    final List<? extends TypeMirror> bounds = t.getBounds();
    final int size = bounds.size();
    final List<? extends TypeMirror> returnValue = switch (size) {
    case 0 -> List.of();
    case 1 -> isInterface(bounds.get(0)) ? bounds : List.of();
    default -> isInterface(bounds.get(0)) ? bounds : bounds.subList(1, size);
    };
    return returnValue.isEmpty() ? returnValue : returnValue.stream().filter(this.filter).toList();
  }

  @Override
  public final List<? extends TypeMirror> visitTypeVariable(final TypeVariable t, final Void x) {
    assert t.getKind() == TypeKind.TYPEVAR;
    final TypeMirror upperBound = t.getUpperBound();
    final List<? extends TypeMirror> returnValue = switch (upperBound.getKind()) {
    case DECLARED -> ((DeclaredType)upperBound).asElement().getKind().isInterface() ? List.of(upperBound) : List.of();
    case INTERSECTION -> this.visit(upperBound);
    default -> List.of();
    };
    return returnValue.isEmpty() ? returnValue : returnValue.stream().filter(this.filter).toList();
  }

}
