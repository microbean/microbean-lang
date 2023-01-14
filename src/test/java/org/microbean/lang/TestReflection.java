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
package org.microbean.lang;

import java.lang.annotation.Documented;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import com.sun.tools.javac.model.JavacTypes;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(AnnotationProcessingInterceptor.class)
final class TestReflection {

  private Reflection reflection;

  private ClassLoader cl;

  private TestReflection() {
    super();
  }

  @BeforeEach
  final void setup() {
    this.reflection = new Reflection();
    this.cl = Thread.currentThread().getContextClassLoader();
  }

  @AfterEach
  final void teardown() {
    this.reflection.clear();
  }

  @Test
  final void testCompilerViewOfDocumented(final ProcessingEnvironment env) {
    // This is all a little batty. java.lang.annotation.Documented
    // annotates itself.
    final TypeElement documentedElement = env.getElementUtils().getTypeElement("java.lang.annotation.Documented");
    assertSame(documentedElement,
               documentedElement.getAnnotationMirrors().get(0).getAnnotationType().asElement());
  }

  // @Disabled
  @Test
  final void testDocumented() throws ReflectiveOperationException {
    // Nice edge case: java.lang.annotation.Documented annotates
    // itself.
    final Element documented = this.reflection.element(Documented.class, this.cl);
  }

  // @Disabled
  @Test
  final void testEnclosedElements() throws ReflectiveOperationException {
    final Element string = this.reflection.element(String.class, this.cl);
    final List<? extends Element> elements = string.getEnclosedElements();
    System.out.println("*** elements.size(): " + elements.size());
    System.out.println("*** elements: " + elements);
    for (final Element e : elements) {
      assertSame(string, e.getEnclosingElement());
    }
  }

  @Test
  final void testEnclosedElementsCompilerViewpoint(final ProcessingEnvironment env) throws ReflectiveOperationException {
    final TypeElement string = env.getElementUtils().getTypeElement("java.lang.String");
    final List<? extends Element> elements = string.getEnclosedElements();
    System.out.println("*** elements.size(): " + elements.size());
    System.out.println("*** elements: " + elements);
    for (final Element e : elements) {
      assertSame(string, e.getEnclosingElement());
    }
  }

  @Test
  final void testSimpleStringFailure() throws ReflectiveOperationException {
    final Element string = this.reflection.element(String.class, this.cl);
    assertEquals("java.lang.String", ((QualifiedNameable)string).getQualifiedName().toString());
  }
  
  @Test
  final void testReflection() throws ReflectiveOperationException {
    final Element string = this.reflection.element(String.class, this.cl);
    assertTrue(((QualifiedNameable)string).getQualifiedName().contentEquals("java.lang.String"));
    assertSame(string, this.reflection.element(String.class, this.cl));
    assertSame(string.asType(), this.reflection.type(String.class, this.cl));
  }

  @Test
  final void testAddInterfaceFailure() throws ReflectiveOperationException {
    final Element myComparableElement = this.reflection.element(Comparable.class, this.cl);
  }

  @Test
  final void testEquality(final ProcessingEnvironment env) throws ReflectiveOperationException {
    final javax.lang.model.util.Elements elements = env.getElementUtils();
    final javax.lang.model.util.Types javacModelTypes = env.getTypeUtils();

    final Element comparableElement = elements.getTypeElement("java.lang.Comparable");
    final Element myComparableElement = this.reflection.element(Comparable.class, this.cl);
    assertTrue(Equality.equalsIncludingAnnotations(comparableElement, myComparableElement));

    final DeclaredType comparableRawType = javacModelTypes.getDeclaredType((TypeElement)comparableElement);
    final org.microbean.lang.type.DeclaredType myComparableRawType = new org.microbean.lang.type.DeclaredType();
    myComparableRawType.setDefiningElement((TypeElement)myComparableElement);
    assertTrue(Equality.equalsIncludingAnnotations(comparableRawType, myComparableRawType));
    
    final TypeMirror comparableElementAsType = comparableElement.asType();
    final TypeMirror myComparableElementAsType = myComparableElement.asType();
    assertTrue(Equality.equalsIncludingAnnotations(comparableElementAsType, myComparableElementAsType)); // fails

  }

}
