/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2023 microBean™.
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
package org.microbean.lang.visitor;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import java.util.function.Predicate;

import javax.lang.model.element.Element;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import javax.lang.model.util.SimpleTypeVisitor14;

import org.microbean.lang.TypeAndElementSource;
import org.microbean.lang.Equality;

import org.microbean.lang.element.DelegatingElement;

import org.microbean.lang.type.NoType;
import org.microbean.lang.type.Types;

import static org.microbean.lang.type.Types.asElement;

/**
 * Given a {@link TypeMirror} and an {@link Element}, attempts to find a supertype of that {@link TypeMirror} whose
 * defining element is equal to (normally is identical to) the supplied {@link Element}.
 *
 * <p>For example, given a type denoted by {@code List<String>}, and a {@link javax.lang.model.element.TypeElement}
 * denoted by {@code Collection}, the result of visitation will be the type denoted by {@code Collection<String>}.</p>
 *
 * <p>{@code javac} does odd things with this and arrays and it is not clear that its documentation matches its
 * code. Consequently I don't have a lot of faith in the {@link visitArray(ArrayType, Element)} method as of this
 * writing.</p>
 *
 * <p>The compiler's {@code asSuper} method documentation says, in part:</p>
 *
 * <blockquote>Return the (most specific) base type of {@code t} that starts with the given symbol.  If none exists,
 * return null.</blockquote>
 */
/* <pre>Some examples:
 *
 * (Enum<E>, Comparable) => Comparable<E>
 * (c.s.s.d.AttributeTree.ValueKind, Enum) => Enum<c.s.s.d.AttributeTree.ValueKind>
 * (c.s.s.t.ExpressionTree, c.s.s.t.Tree) => c.s.s.t.Tree
 * (j.u.List<capture#160 of ? extends c.s.s.d.DocTree>, Iterable) =>
 *     Iterable<capture#160 of ? extends c.s.s.d.DocTree>
 */
// Basically done
//
// https://github.com/openjdk/jdk/blob/jdk-20+13/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L2165-L2221
public final class AsSuperVisitor extends SimpleTypeVisitor14<TypeMirror, Element> {

  private final Set<DelegatingElement> seenTypes; // in the compiler, the field is called seenTypes but stores Symbols (Elements).

  private final TypeAndElementSource elementSource;

  private final Equality equality;

  private final Types types;

  private final SupertypeVisitor supertypeVisitor;

  private SubtypeVisitor subtypeVisitor;

  public AsSuperVisitor(final TypeAndElementSource elementSource,
                        final Equality equality,
                        final Types types,
                        final SupertypeVisitor supertypeVisitor) {
    super();
    this.seenTypes = new HashSet<>();
    this.elementSource = Objects.requireNonNull(elementSource, "elementSource");
    this.equality = equality == null ? new Equality(true) : equality;
    this.types = Objects.requireNonNull(types, "types");
    this.supertypeVisitor = Objects.requireNonNull(supertypeVisitor, "supertypeVisitor");
  }

  public final AsSuperVisitor withSupertypeVisitor(final SupertypeVisitor supertypeVisitor) {
    if (supertypeVisitor == this.supertypeVisitor) {
      return this;
    }
    return new AsSuperVisitor(this.elementSource, this.equality, this.types, supertypeVisitor);
  }

  public final AsSuperVisitor withSubtypeVisitor(final SubtypeVisitor subtypeVisitor) {
    if (subtypeVisitor == this.subtypeVisitor) {
      return this;
    }
    final AsSuperVisitor v = new AsSuperVisitor(this.elementSource, this.equality, this.types, this.supertypeVisitor);
    v.setSubtypeVisitor(subtypeVisitor);
    return v;
  }
  
  final void setSubtypeVisitor(final SubtypeVisitor subtypeVisitor) {
    if (subtypeVisitor.asSuperVisitor() != this) {
      throw new IllegalArgumentException("subtypeVisitor");
    } else if (subtypeVisitor != this.subtypeVisitor) {
      this.subtypeVisitor = subtypeVisitor.withCapture(true);
    }
  }

  // This is extraordinarily weird. In javac, all (non-generic? all?) array types have, as their synthetic element, one
  // synthetic element that is a ClassSymbol named "Array". So the type denoted by, say, byte[] and the type denoted by
  // Object[] both return exactly the same ClassSymbol reference from their asElement() method/sym field. See
  // Symtab.java line 483ish. (I guess that ClassSymbol corresponds in some way to "[]".)
  //
  // Now, what type does that ClassSymbol have?  It is built like this:
  //
  //   arrayClass = new ClassSymbol(PUBLIC|ACYCLIC, names.Array, noSymbol);
  //
  // The first argument is a bunch of flags. OK. The second is, as we've seen, equal to, simply, "Array".  The third is
  // its owner/enclosing element, which is nothing.
  //
  // This three-argument constructor delegates to the canonical four-argument constructor. What's missing? The type.  So
  // when you build a ClassSymbol from the three-argument constructor, you effectively supply "new
  // ClassType(Type.noType, null, null)" as its type argument.  If I'm reading this right and translating properly to
  // the javax.lang.model hierarchy, that Element's asType() method will return a ClassType/DeclaredType (!) with no
  // enclosing type and no type arguments.
  @Override
  public final TypeMirror visitArray(final ArrayType t, final Element element) {
    assert t.getKind() == TypeKind.ARRAY;
    final TypeMirror s = element.asType();
    return this.subtypeVisitor.visit(t, s) ? s : null;
  }

  @Override
  public final TypeMirror visitDeclared(final DeclaredType t, final Element element) {
    assert t.getKind() == TypeKind.DECLARED;
    return this.visitDeclaredOrIntersection(t, element);
  }

  private final TypeMirror visitDeclaredOrIntersection(final TypeMirror t, final Element element) {
    assert t.getKind() == TypeKind.DECLARED || t.getKind() == TypeKind.INTERSECTION;
    Objects.requireNonNull(element, "element");
    final Element te = asElement(t, true /* yes, generate synthetic elements a la javac */);
    if (te == null) {
      return null;
    } else if (this.equality.equals(te, element)) {
      return t;
    }
    // TODO: may be able to get away with identity instead of DelegatingElement
    final DelegatingElement c = DelegatingElement.of(te, this.elementSource);
    if (!this.seenTypes.add(c)) { // javac calls it seenTypes but it stores Symbols/Elements
      return null;
    }
    try {
      final TypeMirror st = this.supertypeVisitor.visit(t);
      switch (st.getKind()) {
      case DECLARED:
      case INTERSECTION:
      case TYPEVAR:
        final TypeMirror x = this.visit(st, element);
        if (x != null) {
          return x;
        }
        break;
      default:
        break;
      }
      if (element.getKind().isInterface()) {
        for (final TypeMirror iface : this.supertypeVisitor.interfacesVisitor().visit(t)) {
          final TypeMirror x = this.visit(iface, element);
          if (x != null) {
            return x;
          }
        }
      }
    } finally {
      this.seenTypes.remove(c);
    }
    return null;
  }

  @Override
  public final TypeMirror visitError(final ErrorType t, final Element element) {
    assert t.getKind() == TypeKind.ERROR;
    return t;
  }

  @Override
  public final TypeMirror visitIntersection(final IntersectionType t, final Element element) {
    assert t.getKind() == TypeKind.INTERSECTION;
    return this.visitDeclaredOrIntersection(t, element);
  }

  @Override
  public final TypeMirror visitTypeVariable(final TypeVariable t, final Element element) {
    assert t.getKind() == TypeKind.TYPEVAR;
    return this.equality.equals(t.asElement(), element) ? t : this.visit(t.getUpperBound(), element);
  }

}
