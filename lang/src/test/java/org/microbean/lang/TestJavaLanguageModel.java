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
import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import java.util.EnumSet;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

final class TestJavaLanguageModel {

  private JavaLanguageModel jlm;
  
  private TestJavaLanguageModel() {
    super();
  }

  @BeforeEach
  final void setup() {
    this.jlm = new JavaLanguageModel();
  }

  @AfterEach
  final void tearDown() {
    this.jlm.close();
  }

  @Test
  final void testJavaLanguageModel() {
    final TypeElement s = jlm.elements().getTypeElement("java.lang.String");
    assertNotNull(s);
    final TypeElement x = jlm.elements().getTypeElement("java.util.logging.Level");
    assertNotNull(x);
    final TypeElement r = jlm.elements().getTypeElement("org.microbean.lang.TestJavaLanguageModel.Gloop");
    assertNotNull(r);
    assertSame(ElementKind.RECORD, r.getKind());
    assertSame(TypeKind.DECLARED, r.asType().getKind());
    assertEquals(1, r.getRecordComponents().size());
    final RecordComponentElement name = r.getRecordComponents().get(0);
    final DeclaredType t = (DeclaredType)name.asType();
    assertSame(TypeKind.DECLARED, t.getKind());
    final TypeElement e = (TypeElement)t.asElement();
    assertEquals("String", e.getSimpleName().toString());
    final ExecutableElement accessor = name.getAccessor();
    assertSame(ElementKind.METHOD, accessor.getKind());
    assertSame(TypeKind.EXECUTABLE, accessor.asType().getKind());
    assertEquals(EnumSet.of(Modifier.PUBLIC), name.getModifiers());
    assertEquals(EnumSet.of(Modifier.PUBLIC, Modifier.FINAL), accessor.getModifiers());
  }

  @Test
  final void testTypeParameters() {
    final TypeElement flob = jlm.elements().getTypeElement("org.microbean.lang.TestJavaLanguageModel.Flob");
    assertNotNull(flob);
    final TypeParameterElement tp = (TypeParameterElement)flob.getTypeParameters().get(0);
    assertEquals("T", tp.getSimpleName().toString());
  }

  private static record Gloop(String name) {

    // Note final here; accessor is otherwise stock
    public final String name() {
      return this.name;
    }
    
  }

  private static class Flob<@Borf T> {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE_PARAMETER)
  private @interface Borf {

  }
  
}
