/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2023–2024 microBean™.
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

import java.util.Objects;

import java.util.function.BiPredicate;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import org.microbean.lang.Equality;

// https://github.com/openjdk/jdk/blob/jdk-20+14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Symbol.java#L829-L849
public class PrecedesPredicate implements BiPredicate<Element, Element> {

  private final SupertypeVisitor supertypeVisitor;

  private final SubtypeVisitor subtypeVisitor;

  private final Equality equality;

  public PrecedesPredicate(final SupertypeVisitor supertypeVisitor,
                           final SubtypeVisitor subtypeVisitor) {
    this(new Equality(true), supertypeVisitor, subtypeVisitor);
  }

  public PrecedesPredicate(final Equality equality,
                           final SupertypeVisitor supertypeVisitor,
                           final SubtypeVisitor subtypeVisitor) {
    super();
    this.equality = equality == null ? new Equality(true) : equality;
    this.supertypeVisitor = Objects.requireNonNull(supertypeVisitor, "supertypeVisitor");
    this.subtypeVisitor = Objects.requireNonNull(subtypeVisitor, "subtypeVisitor");
  }

  // Does e precede f?
  @Override
  public final boolean test(final Element e, final Element f) {
    if (e == f) {
      // Optimization
      return false;
    }
    final TypeMirror t = e.asType();
    final TypeMirror s = f.asType();
    switch (t.getKind()) {
    case DECLARED:
      switch (s.getKind()) {
      case DECLARED:
        if (this.equality.equals(e, f)) { // already checked e == f
          // Both are completely interchangeable DeclaredTypes; can't say which comes first.
          return false;
        }
        final int rt = this.rank(t);
        final int rs = this.rank(s);
        // Use JDK 21+ semantics; see https://github.com/openjdk/jdk/commit/426025aab42d485541a899844b96c06570088771
        return rs < rt || (rs == rt && this.disambiguate((TypeElement)e, (TypeElement)f) < 0);
      default:
        // t is not a type variable but s is, so t does not precede s, because "Type variables always precede other
        // kinds of symbols [sic]." s precedes t in fact, but that's not what's being tested.
        return false;
      }
    case TYPEVAR:
      switch (s.getKind()) {
      case TYPEVAR:
        if (this.equality.equals(e, f)) { // already checked e == f
          // Both are completely interchangeable TypeVariables; can't say which comes first.
          return false;
        }
        return this.subtypeVisitor.withCapture(true).visit(t, s);
      default:
        // https://github.com/openjdk/jdk/blob/jdk-20+14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Symbol.java#L833:
        // "Type variables always precede other kinds of symbols."  (Note that a type variable is not a symbol; I think
        // javac means "type variables always precede other kinds of types". Or it could be using "type variable" to
        // mean "type parameter element".)
        //
        // So t is a type variable and s is not, so t precedes s.
        return true;
      }
    default:
      return false;
    }
  }

  // Disambiguate two unequal TypeElements with the same rank/specialization depth.
  protected int disambiguate(final TypeElement e, final TypeElement f) {
    return CharSequence.compare(e.getQualifiedName(), f.getQualifiedName());
  }

  // https://github.com/openjdk/jdk/blob/jdk-20+14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3576-L3621
  @SuppressWarnings("fallthrough")
  final int rank(final TypeMirror t) {
    switch (t.getKind()) {
    case DECLARED:
      if (((TypeElement)((DeclaredType)t).asElement()).getQualifiedName().contentEquals("java.lang.Object")) {
        return 0;
      }
      // fall through
    case INTERSECTION:
    case TYPEVAR:
      int r = this.rank(this.supertypeVisitor.visit(t)); // RECURSIVE
      for (final TypeMirror iface : this.supertypeVisitor.interfacesVisitor().visit(t)) {
        r = Math.max(r, this.rank(iface)); // RECURSIVE
      }
      return r + 1;
    case ERROR:
    case NONE:
      return 0;
    default:
      throw new IllegalArgumentException("t: " + t);
    }
  }

}
