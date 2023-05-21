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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.List;

import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import com.sun.tools.javac.model.JavacTypes;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.microbean.lang.Equality;
import org.microbean.lang.Lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

final class TestTypeClosureVisitor {

  private static com.sun.tools.javac.code.Types javacCodeTypes;

  private Visitors visitors;

  private TestTypeClosureVisitor() {
    super();
  }

  @BeforeAll
  static void staticSetup() throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, NoSuchMethodException {
    final Field f = JavacTypes.class.getDeclaredField("types");
    assertTrue(f.trySetAccessible());
    final Method pe = Lang.class.getDeclaredMethod("pe");
    assertTrue(pe.trySetAccessible());
    javacCodeTypes = (com.sun.tools.javac.code.Types)f.get(((javax.annotation.processing.ProcessingEnvironment)pe.invoke(null)).getTypeUtils());
  }

  @BeforeEach
  final void setup() {
    this.visitors = new Visitors(Lang.elementSource());
  }

  @Test
  final void testClosure() {
    final TypeMirror s = Lang.declaredType("java.lang.String");
    final List<? extends TypeMirror> closure = javacCodeTypes.closure((Type)s);
    final List<? extends TypeMirror> visitorClosure = this.visitors.typeClosureVisitor().visit(s).toList();
    assertEquals(closure.size(), visitorClosure.size());
    assertTrue(sameType(Lang.declaredType("java.lang.Object"), closure.get(closure.size() - 1)));
    assertTrue(sameType(Lang.declaredType("java.lang.Object"), closure.get(visitorClosure.size() - 1)));
    assertTrue(contentsEqual(closure, visitorClosure));
  }

  private static final boolean contentsEqual(final Iterable<? extends TypeMirror> t, final Iterable<? extends TypeMirror> s) {
    boolean ok = false;
    LOOP:
    for (final TypeMirror c : t) {
      ok = false;
      for (final TypeMirror vc : s) {
        if (sameType(c, vc)) {
          ok = true;
          continue LOOP;
        }
      }
    }
    if (ok) {
      ok = false;
      LOOP:
      for (final TypeMirror vc : s) {
        ok = false;
        for (final TypeMirror c : t) {
          if (sameType(vc, c)) {
            ok = true;
            continue LOOP;
          }
        }
      }
    }
    return ok;
  }

  private static final boolean sameType(final TypeMirror t, final TypeMirror s) {
    return Lang.sameType(t, s);
  }

  @Test
  final void testUnion() {
    final TypeElement serializableElement = Lang.typeElement("java.io.Serializable");
    final DeclaredType serializable = (DeclaredType)serializableElement.asType();

    final TypeElement constantDescElement = Lang.typeElement("java.lang.constant.ConstantDesc");
    final DeclaredType constantDesc = (DeclaredType)constantDescElement.asType();

    final List<? extends TypeMirror> union = javacCodeTypes.union(com.sun.tools.javac.util.List.of((Type)serializable),
                                                                  com.sun.tools.javac.util.List.of((Type)constantDesc));

    final PrecedesPredicate precedesPredicate = this.visitors.precedesPredicate();
    final TypeClosure typeClosure = new TypeClosure(Lang.elementSource(), precedesPredicate);
    typeClosure.union(serializable);
    typeClosure.union(constantDesc);
    assertTrue(contentsEqual(union, typeClosure.toList()));
  }

  @Test
  final void testCharSequenceAndComparablePrecedence() {
    final TypeElement charSequenceElement = Lang.typeElement("java.lang.CharSequence");
    final DeclaredType charSequence = (DeclaredType)charSequenceElement.asType();

    final TypeElement comparableElement = Lang.typeElement("java.lang.Comparable");
    final DeclaredType comparableString = Lang.declaredType(null, comparableElement, Lang.declaredType("java.lang.String"));
    assertSame(comparableElement, comparableString.asElement());

    final PrecedesPredicate precedesPredicate = this.visitors.precedesPredicate();

    // This is wild.  The ranks are the same across the board:
    final int rank = javacCodeTypes.rank((Type)charSequence);
    assertEquals(1, rank);
    assertEquals(1, javacCodeTypes.rank((Type)comparableString));
    assertEquals(1, precedesPredicate.rank(charSequence));
    assertEquals(1, precedesPredicate.rank(comparableString));

    // The type tags are the same:
    assertSame(TypeTag.CLASS, ((Type)charSequence).getTag());
    assertSame(TypeTag.CLASS, ((Type)comparableString).getTag());

    // charSequenceName precedes comparableName:
    final com.sun.tools.javac.util.Name charSequenceName = (com.sun.tools.javac.util.Name)charSequenceElement.getQualifiedName();
    final com.sun.tools.javac.util.Name comparableName = (com.sun.tools.javac.util.Name)comparableElement.getQualifiedName();
    assertTrue(charSequenceName.compareTo(comparableName) < 0);
    assertTrue(CharSequence.compare(charSequenceName, comparableName) < 0);

    // ...but precedes doesn't work like this in JDK 20 and earlier.
    //
    // In JDK 20 precedes() does this after ensuring tags are both CLASS:
    //
    // return
    //     types.rank(that.type) < types.rank(this.type) ||
    //     types.rank(that.type) == types.rank(this.type) &&
    //     that.getQualifiedName().compareTo(this.getQualifiedName()) < 0; // <-- NOTE
    //
    // In JDK 21 it does this:
    //
    // return
    //     types.rank(that.type) < types.rank(this.type) ||
    //     (types.rank(that.type) == types.rank(this.type) &&
    //     this.getQualifiedName().compareTo(that.getQualifiedName()) < 0); // <-- NOTE

    final boolean precedes = ((TypeSymbol)charSequenceElement).precedes((TypeSymbol)comparableElement, javacCodeTypes);
    if (Runtime.version().feature() < 21) {
      assertFalse(precedes); // See https://github.com/openjdk/jdk/commit/426025aab42d485541a899844b96c06570088771
    } else {
      assertTrue(precedes);
    }

    // We follow the JDK 21 approach:
    assertTrue(precedesPredicate.test(charSequenceElement, comparableElement));

  }

  @Test
  final void testSerializableAndConstantDescPrecedence() {
    final TypeElement serializableElement = Lang.typeElement("java.io.Serializable");
    final DeclaredType serializable = (DeclaredType)serializableElement.asType();

    final TypeElement constantDescElement = Lang.typeElement("java.lang.constant.ConstantDesc");
    final DeclaredType constantDesc = (DeclaredType)constantDescElement.asType();

    final PrecedesPredicate precedesPredicate = this.visitors.precedesPredicate();

    // Ranks are equal.
    assertEquals(1, javacCodeTypes.rank((Type)serializable));
    assertEquals(1, javacCodeTypes.rank((Type)constantDesc));
    assertEquals(1, precedesPredicate.rank(serializable));
    assertEquals(1, precedesPredicate.rank(constantDesc));

    // Both types have CLASS tag internally.
    assertSame(TypeTag.CLASS, ((Type)serializable).getTag());
    assertSame(TypeTag.CLASS, ((Type)constantDesc).getTag());

    final com.sun.tools.javac.util.Name serializableName = (com.sun.tools.javac.util.Name)serializableElement.getQualifiedName();
    final com.sun.tools.javac.util.Name constantDescName = (com.sun.tools.javac.util.Name)constantDescElement.getQualifiedName();

    // Using standard CharSequence semantics, the CharSequence "java.io.Serializable" precedes the CharSequence
    // "java.lang.constant.ConstantDesc".
    assertTrue(CharSequence.compare(serializableName, constantDescName) < 0);

    if (Runtime.version().feature() < 21) {
      // In JDK 20, the Name denoting "java.io.Serializable" DOES NOT precede the Name denoting
      // "java.lang.constant.ConstantDesc".
      assertTrue(serializableName.compareTo(constantDescName) > 0); // This is wild. See https://github.com/openjdk/jdk/commit/426025aab42d485541a899844b96c06570088771
      // And yet, thanks to the reverse comparison order in JDK 20:
      assertTrue(((TypeSymbol)serializableElement).precedes((TypeSymbol)constantDescElement, javacCodeTypes));
    } else {
      // In JDK 21+, they fixed this.
      assertTrue(serializableName.compareTo(constantDescName) < 0);
      assertTrue(((TypeSymbol)serializableElement).precedes((TypeSymbol)constantDescElement, javacCodeTypes));
    }

    // We follow the JDK 21 approach.
    assertTrue(precedesPredicate.test(serializableElement, constantDescElement));
  }

}
