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

import com.sun.tools.javac.model.JavacTypes;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.microbean.lang.visitor.Visitors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(AnnotationProcessingInterceptor.class)
final class TestSameTypeVisitor {

  private TestSameTypeVisitor() {
    super();
  }

  @Test
  final void testSameTypeVisitor(final ProcessingEnvironment env) {
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

    // Set up the fundamentals.
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
    final Visitors visitors = new Visitors(tes);

    // Should be ready to go.

  }

}
