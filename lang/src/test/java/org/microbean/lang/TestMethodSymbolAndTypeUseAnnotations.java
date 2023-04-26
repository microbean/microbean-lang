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

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.SymbolMetadata;
import com.sun.tools.javac.code.TargetType;
import com.sun.tools.javac.code.TypeAnnotationPosition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;

import javax.lang.model.util.Elements;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestMethodSymbolAndTypeUseAnnotations {

  private TestMethodSymbolAndTypeUseAnnotations() {
    super();
  }

  @Test
  final void testJavac() {
    final JavaLanguageModel jlm = new JavaLanguageModel();
    final Elements elements = jlm.elements();
    final Element e = elements.getTypeElement("org.microbean.lang.TestMethodSymbolAndTypeUseAnnotations.B");
    final ExecutableElement c = (ExecutableElement)e.getEnclosedElements().get(1);
    assertTrue(c.getSimpleName().contentEquals("c"));
    final VariableElement s = (VariableElement)c.getParameters().get(0);
    // final List<? extends AnnotationMirror> typeAttributes = ((MethodSymbol)c).getRawTypeAttributes();
    final List<? extends AnnotationMirror> typeAttributes = ((VarSymbol)s).owner.getRawTypeAttributes();
    assertEquals(1, typeAttributes.size());
    final Attribute.TypeCompound a = (Attribute.TypeCompound)typeAttributes.get(0);
    final TypeAnnotationPosition position = a.getPosition();
    final TargetType targetType = position.type;
    assertSame(TargetType.METHOD_FORMAL_PARAMETER, targetType);
  }

  @Test
  final void testReflection() throws NoSuchMethodException {
    final java.lang.reflect.Method c = B.class.getDeclaredMethod("c", String.class);
    final java.lang.reflect.AnnotatedType sType = c.getAnnotatedParameterTypes()[0];
    assertSame(A.class, sType.getAnnotations()[0].annotationType());
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.TYPE_USE })
  public @interface A {}

  @A
  public static final class B {

    public B() {
      super();
    }
    
    public static final void c(@A String s) {}
    
  }
  
}
