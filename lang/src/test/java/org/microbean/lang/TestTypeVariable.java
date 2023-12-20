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
package org.microbean.lang;

import java.lang.reflect.TypeVariable;

import java.util.Arrays;
import java.util.Map;

import java.util.concurrent.ConcurrentMap;

import javax.lang.model.type.TypeMirror;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.microbean.lang.Lang.declaredType;
import static org.microbean.lang.Lang.subtype;
import static org.microbean.lang.Lang.typeVariable;

final class TestTypeVariable {

  private TestTypeVariable() {
    super();
  }

  @Test
  final <T extends Cloneable & ConcurrentMap<String, String>> void testTypeVariable() throws ReflectiveOperationException {
    final TypeVariable<?> tv = this.getClass().getDeclaredMethod("testTypeVariable").getTypeParameters()[0];
    // Note that in all three cases the bounds are as listed, not sorted according to specificity.
    assertEquals("[interface java.lang.Cloneable, java.util.concurrent.ConcurrentMap<java.lang.String, java.lang.String>]", Arrays.asList(tv.getBounds()).toString());
    assertEquals("[java.lang.Cloneable, java.util.concurrent.ConcurrentMap<java.lang.String,java.lang.String>]", Lang.typeParameterElement(tv).getBounds().toString());
    assertEquals("java.lang.Object&java.lang.Cloneable&java.util.concurrent.ConcurrentMap<java.lang.String,java.lang.String>", Lang.typeVariable(tv).getUpperBound().toString());
  }

  // JLS §4.10.2: "The direct supertypes of a type variable are the types listed in its bound."
  //
  // So a type variable T and a type variable S may only participate in the subtype relationship if T extends S or S
  // extends T; bounds have no effect.
  @Test
  final
    <T extends Map<String, String>,
     S extends ConcurrentMap<String, String>,
     R extends T>
    void testTypeVariableSubtyping() throws ReflectiveOperationException {
    final TypeMirror t = typeVariable(this.getClass().getDeclaredMethod("testTypeVariableSubtyping"), "T");
    assertNotNull(t);
    final TypeMirror s = typeVariable(this.getClass().getDeclaredMethod("testTypeVariableSubtyping"), "S");
    assertNotNull(s);
    assertFalse(subtype(t, s));
    assertFalse(subtype(s, t));

    final TypeMirror r = typeVariable(this.getClass().getDeclaredMethod("testTypeVariableSubtyping"), "R");
    assertNotNull(r);
    assertTrue(subtype(r, t));
  }

  @Test
  final <T extends String, S extends CharSequence> void testSimpleTypeVariableSubtyping() throws ReflectiveOperationException {
    final TypeMirror t = typeVariable(this.getClass().getDeclaredMethod("testSimpleTypeVariableSubtyping"), "T");
    assertNotNull(t);
    final TypeMirror s = typeVariable(this.getClass().getDeclaredMethod("testSimpleTypeVariableSubtyping"), "S");
    assertNotNull(s);
    assertFalse(subtype(t, s));
    assertFalse(subtype(s, t));

    assertTrue(subtype(t, declaredType("java.lang.CharSequence")));
  }

  @Test
  final <T> void testUnadornedTypeVariable() throws ReflectiveOperationException {
    final javax.lang.model.type.TypeVariable t = typeVariable(this.getClass().getDeclaredMethod("testUnadornedTypeVariable"), "T");
    assertNotNull(t);
    assertNotNull(t.getUpperBound());
    assertNotNull(t.getLowerBound());
  }

}
