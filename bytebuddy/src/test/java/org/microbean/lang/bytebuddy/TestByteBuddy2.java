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
package org.microbean.lang.bytebuddy;

import java.util.List;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.microbean.lang.Lang.arrayType;
import static org.microbean.lang.Lang.arrayTypeOf;
import static org.microbean.lang.Lang.declaredType;
import static org.microbean.lang.Lang.primitiveType;
import static org.microbean.lang.Lang.typeElement;
import static org.microbean.lang.Lang.wildcardType;

final class TestByteBuddy2 {

  private ByteBuddy2 bb;

  private TestByteBuddy2() {
    super();
  }

  @BeforeEach
  final void setup() {
    this.bb = new ByteBuddy2(new TypeElementTypePool());
  }

  @Test
  final void testBoolean() {
    final TypeDescription.Generic tdg = this.bb.typeDescriptionGeneric(primitiveType(TypeKind.BOOLEAN));
    assertSame(TypeDefinition.Sort.NON_GENERIC, tdg.getSort());
    assertTrue(tdg.represents(boolean.class));
  }

  @Test
  final void testThisClass() { // significant for not being cached anywhere
    final TypeMirror t = declaredType(this.getClass());
    final TypeDescription.Generic tdg = this.bb.typeDescriptionGeneric(t);
    assertSame(TypeDefinition.Sort.NON_GENERIC, tdg.getSort());
    assertTrue(tdg.represents(this.getClass()));
    final TypeDescription td = this.bb.typeDescription(t);
    assertSame(td, tdg.asErasure());
  }

  @Test
  final void testListThisClass() { // significant for not being cached and for being generic
    final TypeMirror t = declaredType(null, typeElement(List.class), declaredType(this.getClass()));
    final TypeDescription.Generic tdg = this.bb.typeDescriptionGeneric(t);
    assertSame(TypeDefinition.Sort.PARAMETERIZED, tdg.getSort());
  }

  @Test
  final void testListThisClassArray() {
    final TypeMirror t = arrayTypeOf(declaredType(null, typeElement(List.class), declaredType(this.getClass())));
    final TypeDescription.Generic tdg = this.bb.typeDescriptionGeneric(t);
    assertSame(TypeDefinition.Sort.GENERIC_ARRAY, tdg.getSort());
  }

  @Test
  final void testListUnboundedWildcard() {
    final TypeMirror t = declaredType(null, typeElement(List.class), wildcardType());
    final TypeDescription.Generic tdg = this.bb.typeDescriptionGeneric(t);
    assertSame(TypeDefinition.Sort.PARAMETERIZED, tdg.getSort());
  }

  @Test
  final void testRawList() {
    final TypeMirror t = declaredType(null, typeElement(List.class));
    final TypeDescription.Generic tdg = this.bb.typeDescriptionGeneric(t);
    assertSame(TypeDefinition.Sort.PARAMETERIZED, tdg.getSort());
  }

  @Test
  final void testObjectArray() {
    final TypeMirror t = arrayType(Object[].class);
    final TypeDescription.Generic tdg = this.bb.typeDescriptionGeneric(t);
    assertSame(TypeDefinition.Sort.NON_GENERIC, tdg.getSort());
    assertTrue(tdg.isArray());
    final TypeDescription td = this.bb.typeDescription(t);
    assertTrue(td.isArray());
    assertEquals(td, tdg.asErasure()); // interesting; not the same
  }

}
