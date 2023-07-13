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

import java.lang.reflect.Method;

import java.util.EnumSet;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.microbean.lang.element.DelegatingElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.microbean.lang.Lang.unwrap;

final class TestJavaLanguageModel {

  private TestJavaLanguageModel() {
    super();
  }

  @Test
  final void testGetTypeElementCanonicalName() {
    final TypeElement e = Lang.typeElement(Lang.moduleElement("java.base"), "java.util.AbstractMap.SimpleEntry");
    assertNotNull(e);
  }

  @Test
  final void testJavaLanguageModel() {
    final TypeElement s = Lang.typeElement("java.lang.String");
    assertNotNull(s);
    final TypeElement x = Lang.typeElement("java.util.logging.Level");
    assertNotNull(x);
    final TypeElement r = Lang.typeElement("org.microbean.lang.TestJavaLanguageModel.Gloop");
    assertNotNull(r);
    assertTrue(r instanceof DelegatingElement);
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
    final TypeElement flob = Lang.typeElement("org.microbean.lang.TestJavaLanguageModel.Flob");
    final TypeParameterElement tp = (TypeParameterElement)flob.getTypeParameters().get(0);
    assertEquals("T", tp.getSimpleName().toString());
  }

  @Test
  final void testReturnTypeOfTopLevelClassConstructor() {
    final TypeElement object = Lang.typeElement("java.lang.Object");
    ExecutableElement c = null;
    ENCLOSED_ELEMENTS:
    for (final Element e : object.getEnclosedElements()) {
      switch (e.getKind()) {
      case CONSTRUCTOR:
        final ExecutableElement ee = (ExecutableElement)e;
        if (ee.getParameters().isEmpty()) {
          c = ee;
          break ENCLOSED_ELEMENTS;
        }
        break;
      default:
        break;
      }
    }
    assertSame(TypeKind.VOID, ((ExecutableType)c.asType()).getReturnType().getKind());
  }

  @Test
  final void testReflectionBridge() throws NoSuchMethodException {
    final TypeElement e = Lang.typeElement(List.class);
    assertTrue(e.getQualifiedName().contentEquals("java.util.List"));
    final Method m = this.getClass().getDeclaredMethod("listString");
    final ExecutableElement ee = Lang.executableElement(m);
    assertTrue(ee.getSimpleName().contentEquals("listString"));
    final java.lang.reflect.TypeVariable<?> tv = List.class.getTypeParameters()[0];
    assertEquals("E", tv.getName());
    assertSame(List.class, tv.getGenericDeclaration());
    final TypeParameterElement tpe = Lang.typeParameterElement(tv);
    assertTrue(tpe.getSimpleName().contentEquals("E"));
    assertSame(unwrap(tpe.asType()), unwrap(Lang.typeVariable(tv)));
  }

  private static List<String> listString() {
    return List.of();
  }

  @Test
  final void testAnnotationsOnClassesInMethods() throws ClassNotFoundException {
    final TypeElement e = Lang.typeElement(this.getClass().getName());
    assertNotNull(e);
    ExecutableElement baker = null;
    EES:
    for (final Element ee : e.getEnclosedElements()) {
      switch (ee.getKind()) {
      case METHOD:
        baker = (ExecutableElement)ee;
        break EES;
      }
    }

    // As it turns out, there is no way in the javax.lang.model.* hierarchy to actually get a local class.

    // Local classes are not enclosed by their defining methods.
    assertEquals(0, baker.getEnclosedElements().size());

    for (final Element ee : e.getEnclosedElements()) {
      if (ee.getKind() == ElementKind.CLASS) {
        // Possibly surprising! Local classes are not present as enclosed elements of their enclosing class either.
        assertNotEquals(NestingKind.LOCAL, ((TypeElement)ee).getNestingKind());
      }
    }

    // You can't get Charlie directly:
    assertNull(Lang.typeElement(this.getClass().getName() + "$1Charlie"));

    // But it does exist, and in the reflection model you can find out all sorts of things about it.
    final Class<?> c = Class.forName(this.getClass().getName() + "$1Charlie");
    assertSame(this.getClass(), c.getEnclosingClass());
    assertEquals("baker", c.getEnclosingMethod().getName());
  }

  @Test
  final void testStrangeJUnitTestAnnotationCase1() {
    assertSame(ElementKind.ANNOTATION_TYPE, Lang.typeElement("org.junit.jupiter.api.Test").getKind());
    assertSame(ElementKind.ANNOTATION_TYPE, Lang.typeElement(Lang.moduleElement("org.junit.jupiter.api"), "org.junit.jupiter.api.Test").getKind());
    assertSame(TypeKind.DECLARED, Lang.typeElement("org.junit.jupiter.api.Test").asType().getKind());
  }

  @Disabled // see https://mail.openjdk.org/pipermail/compiler-dev/2023-July/023750.html
  @Test
  final void testStrangeJUnitTestAnnotationCase2() throws ReflectiveOperationException {
    final ExecutableElement ee = Lang.executableElement(this.getClass().getDeclaredMethod("testStrangeJUnitTestAnnotationCase2"));
    final AnnotationMirror am = ee.getAnnotationMirrors().get(0);
    final DeclaredType t = am.getAnnotationType();
    assertSame(TypeKind.DECLARED, t.getKind()); // fails
    assertSame(ElementKind.ANNOTATION_TYPE, t.asElement().getKind()); // fails
  }


  /*
   * Members for introspecting/analyzing in tests.
   */


  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  private @interface Able {}

  private static void baker() {
    @Able
    class Charlie {};
  }

  private static record Gloop(String name) {

    // Note final here; accessor is otherwise stock
    public final String name() {
      return this.name;
    }

  }

  private static class Flob<@Borf T> {}

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE_PARAMETER)
  private @interface Borf {}

}
