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

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.microbean.lang.type.DelegatingTypeMirror;
import org.microbean.lang.type.Types;

import org.microbean.lang.visitor.AssignableVisitor;
import org.microbean.lang.visitor.SubtypeVisitor;
import org.microbean.lang.visitor.Visitors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.microbean.lang.Lang.unwrap;

final class TestAssignable {

  private ElementSource es;

  private Types types;
  
  private SubtypeVisitor subtypeVisitor;

  private AssignableVisitor assignableVisitor;
  
  private TestAssignable() {
    super();
  }

  @BeforeEach
  final void setup() {
    this.es = Lang.elementSource();
    this.types = new Types(this.es);
    final Visitors visitors = new Visitors(Lang.elementSource());
    this.subtypeVisitor = visitors.subtypeVisitor();
    this.assignableVisitor = visitors.assignableVisitor();
  }

  @AfterEach
  final void tearDown() {

  }

  @Test
  final void testStringAssignableToObject() {
    final DeclaredType string = (DeclaredType)this.es.element("java.base", "java.lang.String").asType();
    final DeclaredType object = (DeclaredType)this.es.element("java.base", "java.lang.Object").asType();
    assertSubtype(string, object);
    assertAssignable(string, object);
    assertNotSubtype(object, string);
    assertNotAssignable(object, string);
  }
  
  @Test
  final void testListStringAssignableToListQuestionMark() {
    final DeclaredType listString = Lang.declaredType(null, (TypeElement)this.es.element("java.base", "java.util.List"), this.es.element("java.base", "java.lang.String").asType());
    assertTrue(listString instanceof DelegatingTypeMirror);
    final DeclaredType listQuestionMark = Lang.declaredType(null, (TypeElement)this.es.element("java.base", "java.util.List"), Lang.wildcardType());
    assertTrue(listQuestionMark instanceof DelegatingTypeMirror);
    assertSubtype(listString, listQuestionMark);
    assertAssignable(listString, listQuestionMark);
    assertNotSubtype(listQuestionMark, listString);
    assertNotAssignable(listQuestionMark, listString);
  }

  @Test
  final void testListStringAssignableToListString() {
    final DeclaredType listString = Lang.declaredType(null, (TypeElement)this.es.element("java.base", "java.util.List"), this.es.element("java.base", "java.lang.String").asType());
    assertSubtype(listString, listString);
    assertAssignable(listString, listString);
  }

  @Test
  final void testCompilationAssignability() {
    final List<?> listQuestionMark1 = new ArrayList<String>();
    @SuppressWarnings("rawtypes")
    final List rawList = listQuestionMark1; // ok
    List<?> listQuestionMark2 = rawList; // also ok
    listQuestionMark2 = listQuestionMark1; // also ok
  }

  @Test
  final void testRawListAssignableToListQuestionMark() {
    final DeclaredType rawList = Lang.declaredType((TypeElement)this.es.element("java.base", "java.util.List"));
    assertTrue(((com.sun.tools.javac.code.Type)unwrap(rawList)).isRaw());
    assertTrue(this.types.raw(rawList));

    final DeclaredType listQuestionMark = Lang.declaredType(null, (TypeElement)this.es.element("java.base", "java.util.List"), Lang.wildcardType());
    assertFalse(((com.sun.tools.javac.code.Type)unwrap(listQuestionMark)).isRaw());
    assertFalse(this.types.raw(listQuestionMark));

    // List<?> is a subtype of List
    assertSubtype(listQuestionMark, rawList);

    // List<?> is assignable to List, i.e. List x = (List<?>)y;
    assertAssignable(listQuestionMark, rawList);
    
    // List is NOT a subtype of List<?>
    assertNotSubtype(rawList, listQuestionMark);

    // NOTE:
    // List IS assignable to List<?>, i.e. List<?> x = (List)y;
    assertAssignable(rawList, listQuestionMark);
  }

  private final void assertAssignable(final TypeMirror payload, final TypeMirror receiver) {
    assertTrue(Lang.assignable(payload, receiver));
    assertTrue(this.assignableVisitor.visit(payload, receiver).booleanValue());
  }

  private final void assertNotAssignable(final TypeMirror payload, final TypeMirror receiver) {
    assertFalse(Lang.assignable(payload, receiver));
    assertFalse(this.assignableVisitor.visit(payload, receiver).booleanValue());
  }

  private final void assertSubtype(final TypeMirror payload, final TypeMirror receiver) {
    assertTrue(Lang.subtype(payload, receiver));
    assertTrue(this.subtypeVisitor.visit(payload, receiver).booleanValue());
  }
  
  private final void assertNotSubtype(final TypeMirror payload, final TypeMirror receiver) {
    assertFalse(Lang.subtype(payload, receiver));
    assertFalse(this.subtypeVisitor.visit(payload, receiver).booleanValue());
  }
  
}
