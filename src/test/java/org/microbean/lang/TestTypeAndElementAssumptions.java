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

import java.lang.reflect.Field;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import com.sun.tools.javac.code.Type.ClassType;

import com.sun.tools.javac.model.JavacTypes;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(AnnotationProcessingInterceptor.class)
final class TestTypeAndElementAssumptions {

  private TestTypeAndElementAssumptions() {
    super();
  }

  @Test
  final void testAssumptions(final ProcessingEnvironment env) {
    final javax.lang.model.util.Elements elements = env.getElementUtils();
    final javax.lang.model.util.Types javacModelTypes = env.getTypeUtils();
    assertTrue(javacModelTypes instanceof JavacTypes);

    com.sun.tools.javac.code.Types javacTypes = null;
    try {
      final Field f = JavacTypes.class.getDeclaredField("types");
      assertTrue(f.trySetAccessible());
      javacTypes = (com.sun.tools.javac.code.Types)f.get(javacModelTypes);
    } catch (final ReflectiveOperationException reflectiveOperationException) {
      fail(reflectiveOperationException);
    }
    assertNotNull(javacTypes);

    // Here we have an element representing a declaration, i.e. "public interface Comparable<T>".
    final TypeElement comparableElement = elements.getTypeElement("java.lang.Comparable");
    assertEquals(ElementKind.INTERFACE, comparableElement.getKind());

    // The element declares a type, of course.
    final DeclaredType elementType = (DeclaredType)comparableElement.asType();
    assertEquals(TypeKind.DECLARED, elementType.getKind());
    assertTrue(elementType instanceof ClassType);

    // More exploring.
    assertSame(comparableElement, ((ClassType)elementType).tsym);
    assertSame(comparableElement, elementType.asElement());

    // The declared type has one type argument.  The sole type argument is definitionally a TypeVariable.
    final List<? extends TypeMirror> typeArguments = elementType.getTypeArguments();
    assertEquals(1, typeArguments.size());
    final TypeVariable soleTypeArgument = (TypeVariable)typeArguments.get(0);
    assertEquals(TypeKind.TYPEVAR, soleTypeArgument.getKind());

    // The element has a type parameter, which is an element itself.
    final List<? extends Element> typeParameters = comparableElement.getTypeParameters();
    assertEquals(1, typeParameters.size());
    final TypeParameterElement soleTypeParameter = (TypeParameterElement)typeParameters.get(0);
    assertSame(soleTypeArgument.asElement(), soleTypeParameter);

    // The sole type parameter element declares a type, which is the type variable we discussed above.
    assertSame(soleTypeArgument, soleTypeParameter.asType());
    assertTrue(soleTypeParameter.getEnclosedElements().isEmpty());
    assertSame(comparableElement, soleTypeParameter.getEnclosingElement());
    assertSame(comparableElement, soleTypeParameter.getGenericElement());

    // Now let's look at a place where a related type is used.
    final TypeElement stringElement = elements.getTypeElement("java.lang.String");
    final DeclaredType stringType = (DeclaredType)stringElement.asType();
    javacTypes.interfaces((com.sun.tools.javac.code.Type)stringType);
    final TypeMirror comparableStringType = stringElement.getInterfaces().stream()
      .filter(i ->
              i.getKind() == TypeKind.DECLARED &&
              i instanceof DeclaredType d &&
              d.asElement().getSimpleName().contentEquals("Comparable"))
      .findFirst()
      .orElseThrow();
    assertNotSame(elementType, comparableStringType);
    final TypeElement comparableStringElement = (TypeElement)((DeclaredType)comparableStringType).asElement();
    assertSame(comparableElement, comparableStringElement);

    // So that's a case where the Comparable<T>-type-declared-by-the-Comparable<T>-element is (clearly) not the same as
    // the Comparable<String>-type-used-by-the-String-element.
    //
    // Important takeaways:
    //
    // * When you have a TypeElement that represents...I don't know how to talk about this.  When you have a TypeElement
    //   that represents the declaration of a type, its asType() method will return a DeclaredType.
    //
    // * The DeclaredType so returned will have type arguments.
    //
    // * The type arguments of that DeclaredType will always be TypeVariables.
    //
    // * Such a TypeVariable will always return the proper TypeParameterElement from its asElement() method (see below).
    //
    // * The TypeElement we're talking about will always have type parameters.
    //
    // * For any given TypeParameterElement, its asType() method will return a TypeVariable, namely a TypeVariable
    //   mentioned above.

  }

}
