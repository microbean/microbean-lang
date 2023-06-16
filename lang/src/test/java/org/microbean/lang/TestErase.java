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

import java.lang.reflect.Field;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;

import javax.lang.model.element.TypeElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import com.sun.tools.javac.code.Type;

import com.sun.tools.javac.model.JavacTypes;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.microbean.lang.type.Types;

import org.microbean.lang.visitor.EraseVisitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(AnnotationProcessingInterceptor.class)
final class TestErase {

  private TestErase() {
    super();
  }

  @Test
  final void testErase(final ProcessingEnvironment env) {
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

    final TypeElement integerElement = elements.getTypeElement("java.lang.Integer");

    final DeclaredType integerElementType = (DeclaredType)integerElement.asType();
    assertSame(TypeKind.DECLARED, integerElementType.getKind());

    // Get the type denoted by the type expression
    // Comparable<Integer>.  Interfaces must be returned in
    // declaration order, and Comparable<Integer> is the first one
    // declared.
    final DeclaredType comparableIntegerType = (DeclaredType)integerElement.getInterfaces().get(0);
    assertSame(TypeKind.DECLARED, comparableIntegerType.getKind()); // ...which is kind of weird when you think about it

    List<? extends TypeMirror> typeArguments = comparableIntegerType.getTypeArguments();

    // Cannot rely on identity for some reason:
    // assertSame(integerElementType, typeArguments.get(0));

    // ...but the type representing java.lang.Integer best be equal to
    // the type representing java.lang.Integer!
    assertTrue(Equality.equalsIncludingAnnotations(integerElementType, typeArguments.get(0)));

    // Now test "official" erasure, via javax.lang.model.util.Types.
    DeclaredType erasure = (DeclaredType)javacModelTypes.erasure(comparableIntegerType);
    typeArguments = erasure.getTypeArguments();
    assertEquals(0, typeArguments.size());
    assertTrue(((Type)erasure).isRaw());

    // Now do it with our stuff.

    final TypeAndElementSource tes = new TypeAndElementSource() {
        @Override
        public final ArrayType arrayTypeOf(final TypeMirror componentType) {
          return javacModelTypes.getArrayType(componentType);
        }
        @Override
        public boolean assignable(final TypeMirror payload, final TypeMirror receiver) {
          return javacModelTypes.isAssignable(payload, receiver);
        }
        @Override
        public final TypeElement boxedClass(final PrimitiveType t) {
          return javacModelTypes.boxedClass(t);
        }
        @Override
        public final DeclaredType declaredType(final TypeElement typeElement, final TypeMirror... arguments) {
          return javacModelTypes.getDeclaredType(typeElement, arguments);
        }
        @Override
        public final DeclaredType declaredType(final DeclaredType enclosingType, final TypeElement typeElement, final TypeMirror... arguments) {
          return javacModelTypes.getDeclaredType(enclosingType, typeElement, arguments);
        }
        @Override
        @SuppressWarnings("unchecked")
        public final <T extends TypeMirror> T erasure(final T t) {
          return (T)javacModelTypes.erasure(t);
        }
        @Override
        public final NoType noType(final TypeKind k) {
          return javacModelTypes.getNoType(k);
        }
        @Override
        public final PrimitiveType primitiveType(final TypeKind k) {
          return javacModelTypes.getPrimitiveType(k);
        }
        @Override
        public boolean sameType(final TypeMirror t, final TypeMirror s) {
          return javacModelTypes.isSameType(t, s);
        }
        @Override
        public final TypeElement typeElement(final CharSequence m, final CharSequence n) {
          return elements.getTypeElement(elements.getModuleElement(m), n);
        }
        @Override
        public final TypeVariable typeVariable(final java.lang.reflect.TypeVariable<?> t) {
          throw new UnsupportedOperationException(); // NOTE
        }
        @Override
        public final WildcardType wildcardType(final TypeMirror extendsBound, final TypeMirror superBound) {
          return javacModelTypes.getWildcardType(extendsBound, superBound);
        }
      };

    final Types types = new Types(tes);

    // Make sure our stuff thinks the javac erasure is raw.
    assertTrue(types.raw(erasure));

    final EraseVisitor eraseVisitor = new EraseVisitor(tes, types);
    erasure = (DeclaredType)eraseVisitor.visit(comparableIntegerType);
    typeArguments = erasure.getTypeArguments();
    assertEquals(0, typeArguments.size());
    assertTrue(types.raw(erasure));

    // Check that we do erasure of primitive types properly.
    final TypeMirror intType = javacModelTypes.getPrimitiveType(TypeKind.INT);
    assertSame(intType, eraseVisitor.visit(intType));

  }

}
