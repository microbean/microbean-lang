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
package org.microbean.lang;

import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.microbean.lang.type.Types;

import org.microbean.lang.visitor.AsSuperVisitor;
import org.microbean.lang.visitor.ContainsTypeVisitor;
import org.microbean.lang.visitor.EraseVisitor;
import org.microbean.lang.visitor.IsAssignableVisitor;
import org.microbean.lang.visitor.IsSameTypeVisitor;
import org.microbean.lang.visitor.SubtypeVisitor;
import org.microbean.lang.visitor.SupertypeVisitor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestIsAssignable {

  private ElementSource es;

  private Types types;
  
  private JavaLanguageModel jlm;

  private SubtypeVisitor subtypeVisitor;

  private IsAssignableVisitor isAssignableVisitor;
  
  private TestIsAssignable() {
    super();
  }

  @BeforeEach
  final void setup() {
    this.jlm = new JavaLanguageModel();
    this.es = this.jlm;
    this.types = new Types(this.es);
    final Visitors visitors = new Visitors(this.es);
    this.subtypeVisitor = visitors.subtypeVisitor();
    this.isAssignableVisitor = visitors.isAssignableVisitor();
  }

  @AfterEach
  final void tearDown() {
    this.jlm.close();
  }

  @Test
  final void testStringIsAssignableToObject() {
    ensure(true, this.es.element("java.lang.String").asType(), this.es.element("java.lang.Object").asType());
  }
  
  @Test
  final void testListStringIsAssignableToListQuestionMark() {
    final DeclaredType listString = this.jlm.types().getDeclaredType((TypeElement)this.es.element("java.util.List"), this.es.element("java.lang.String").asType());
    final DeclaredType listQuestionMark = this.jlm.types().getDeclaredType((TypeElement)this.es.element("java.util.List"), this.jlm.types().getWildcardType(null, null));
    ensure(false, listQuestionMark, listString);
    ensure(true, listString, listQuestionMark);
  }

  @Test
  final void testListStringIsAssignableToListString() {
    final DeclaredType listString = this.jlm.types().getDeclaredType((TypeElement)this.es.element("java.util.List"), this.es.element("java.lang.String").asType());
    final DeclaredType listQuestionMark = this.jlm.types().getDeclaredType((TypeElement)this.es.element("java.util.List"), this.jlm.types().getWildcardType(null, null));
    ensure(true, listString, listString);
  }

  @Test
  final void testRawListIsAssignableToListQuestionMark() {
    final DeclaredType rawList = this.jlm.types().getDeclaredType((TypeElement)this.es.element("java.util.List"));
    assertTrue(((com.sun.tools.javac.code.Type)rawList).isRaw());
    assertTrue(this.types.raw(rawList));
    final DeclaredType listQuestionMark = this.jlm.types().getDeclaredType((TypeElement)this.es.element("java.util.List"), this.jlm.types().getWildcardType(null, null));
    // Succeeds:
    assertAssignable(rawList, listQuestionMark);

    assertNotSubtype(rawList, listQuestionMark);
    // Fails:
    // assertSubtype(rawList, listQuestionMark);
    //
    // So we need to really make sure we understand what the differences are between being a subtype and being assignable.
    //
    // javac's Types class really calls isConvertible() from isAssignable()
  }

  private final void assertAssignable(final TypeMirror payload, final TypeMirror receiver) {
    assertTrue(this.jlm.types().isAssignable(payload, receiver));
    assertTrue(this.isAssignableVisitor.visit(payload, receiver).booleanValue());
  }

  private final void assertNotSubtype(final TypeMirror payload, final TypeMirror receiver) {
    assertFalse(this.jlm.types().isSubtype(payload, receiver));
    assertFalse(this.subtypeVisitor.visit(payload, receiver).booleanValue());
  }
  
  private final void assertSubtype(final TypeMirror payload, final TypeMirror receiver) {
    assertTrue(this.jlm.types().isSubtype(payload, receiver));
    assertTrue(this.subtypeVisitor.visit(payload, receiver).booleanValue());
  }

  private final void ensure(final boolean expected, final TypeMirror payload, final TypeMirror receiver) {
    if (expected) {
      assertTrue(this.jlm.types().isSubtype(payload, receiver), "payload (" + payload + ") is not a subtype of receiver (" + receiver + ")");
      assertTrue(this.jlm.types().isAssignable(payload, receiver));
      assertTrue(this.subtypeVisitor.visit(payload, receiver).booleanValue());
    } else {
      assertFalse(this.jlm.types().isSubtype(payload, receiver));
      assertFalse(this.jlm.types().isAssignable(payload, receiver));
      assertFalse(this.subtypeVisitor.visit(payload, receiver).booleanValue());
    }
  }
  
}
