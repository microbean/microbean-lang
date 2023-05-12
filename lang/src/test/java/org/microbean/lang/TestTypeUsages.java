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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestTypeUsages {

  private JavaLanguageModel jlm;
  
  private TestTypeUsages() {
    super();
  }

  @BeforeEach
  final void setup() {
    this.jlm = new JavaLanguageModel();
  }

  @AfterEach
  final void tearDown() {

  }

  @Test
  final void testClassDeclarationDeclaredTypeHasNoAnnotations() {
    final Element e = this.jlm.typeElement("org.microbean.lang.TestTypeUsages.B");
    assertEquals(1, e.getAnnotationMirrors().size()); // @A applies to B-the-element
    assertEquals(0, e.asType().getAnnotationMirrors().size()); // @A does NOT apply to B-the-type-declaration
  }

  @Disabled // see https://bugs.openjdk.org/browse/JDK-8225377
  @Test
  final void testMethodParameterDeclaredTypeHasNoAnnotations() {
    final Element e = jlm.typeElement("org.microbean.lang.TestTypeUsages.B");

    final ExecutableElement c = (ExecutableElement)e.getEnclosedElements().get(1);

    // The c method in the B class.
    assertTrue(c.getSimpleName().contentEquals("c"));

    // There aren't any annotations on it.
    assertEquals(0, c.getAnnotationMirrors().size());

    // The s parameter of type String.
    final VariableElement s = (VariableElement)c.getParameters().get(0);
    assertEquals("s", s.getSimpleName().toString());

    // Prove that @A, which doesn't have PARAMETER in its @Target, really and truly does not annotate s-as-element.
    assertEquals(0, s.getAnnotationMirrors().size());

    // The type of s (the DeclaredType backing the s parameter).
    final DeclaredType sAsType = (DeclaredType)s.asType();

    // Perhaps surprisingly, @A doesn't annotate s-as-type. But that's a javac bug. See
    // https://bugs.openjdk.org/browse/JDK-8225377.
    assertFalse(sAsType.getAnnotationMirrors().isEmpty());
  }

  @Disabled // see https://docs.oracle.com/javase/specs/jls/se19/html/jls-9.html#jls-9.7.4 and https://bugs.openjdk.org/browse/JDK-8225377
  @Test
  final void testArrayTypeUse() {
    final Element e = jlm.typeElement("org.microbean.lang.TestTypeUsages.B");
    final ExecutableElement d = (ExecutableElement)e.getEnclosedElements().get(2);

    // The d method in the B class.
    assertTrue(d.getSimpleName().contentEquals("d"));

    final ArrayType returnType = (ArrayType)d.getReturnType();
    final AnnotationMirror a = returnType.getAnnotationMirrors().get(0); // fails
  }

  @Disabled // see https://bugs.openjdk.org/browse/JDK-8225377
  @Test
  final void testTypeArgument() {
    final TypeElement f = jlm.typeElement("org.microbean.lang.TestTypeUsages.F");
    final List<? extends TypeParameterElement> tps = f.getTypeParameters();
    assertEquals(1, tps.size());
    final TypeElement g = jlm.typeElement("org.microbean.lang.TestTypeUsages.G");
    final DeclaredType supertype = (DeclaredType)g.getSuperclass();
    final List<?> typeArguments = supertype.getTypeArguments();
    assertEquals(1, typeArguments.size());
    final DeclaredType string = (DeclaredType)typeArguments.get(0);
    assertEquals(1, string.getAnnotationMirrors().size());
  }


  /*
   * Inner and nested classes.
   */

  
  @Retention(RUNTIME)
  // Note as an interesting curiosity that TYPE_USE implies ANNOTATION_TYPE, TYPE and TYPE_PARAMETER as well.  See
  // https://mail.openjdk.org/pipermail/compiler-dev/2023-February/022200.html.
  @Target({ TYPE, TYPE_USE })
  public @interface A {}

  @Retention(RUNTIME)
  // Note as an interesting curiosity that TYPE_USE implies ANNOTATION_TYPE, TYPE and TYPE_PARAMETER as well.  See
  // https://mail.openjdk.org/pipermail/compiler-dev/2023-February/022200.html.
  @Target({ TYPE_USE })
  public @interface E {}
  
  @A
  private static final class B {

    private B() {
      super();
    }
    
    public static final void c(@A String s) {}

    // Yow. @A annotates the array type (of the type denoted by String[][]). @E annotaes the component type (denoted by
    // String[]).
    public static final String @A [] @E [] d() {
      return null;
    }
    
  }

  private static class F<T> {}

  private static class G extends F<@E String> {}
  
  
  
}
