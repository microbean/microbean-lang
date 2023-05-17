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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.List;

import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;

import com.sun.tools.javac.code.Type;

import com.sun.tools.javac.model.JavacTypes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.microbean.lang.visitor.Visitors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestBoundingClassVisitor {

  private Visitors visitors;
  
  private com.sun.tools.javac.code.Types javacCodeTypes;
  
  private TestBoundingClassVisitor() {
    super();
  }

  @BeforeEach
  final void setup() throws IllegalAccessException, NoSuchFieldException {
    this.visitors = new Visitors((m, n) -> Lang.typeElement(Lang.moduleElement(m), n));
    final Field f = JavacTypes.class.getDeclaredField("types");
    assertTrue(f.trySetAccessible());
    this.javacCodeTypes = (com.sun.tools.javac.code.Types)f.get(Lang.pe().getTypeUtils());
  }

  @Test
  final void testSimple() {
    final DeclaredType t = Lang.declaredType(String.class);
    assertSame(Lang.noType(TypeKind.NONE), t.getEnclosingType());
    // classBound() doesn't do anything here, obviously:
    assertSame(t, this.javacCodeTypes.classBound((Type)t));
    assertSame(t, this.visitors.boundingClassVisitor().visit(t));
  }
  
  @Test
  final void testMildlyComplicated() {
    final DataHolder<String>.Inner inner = new DataHolder<>("hello").data();
    final DeclaredType outerDeclaredType = Lang.declaredType(Lang.typeElement(DataHolder.class), Lang.declaredType(String.class));
    final DeclaredType innerDeclaredType = Lang.declaredType(outerDeclaredType, Lang.typeElement(inner.getClass()));
    assertSame(outerDeclaredType, innerDeclaredType.getEnclosingType());
    // classBound() doesn't do anything here:
    assertSame(outerDeclaredType, this.javacCodeTypes.classBound((Type)outerDeclaredType));
    assertSame(outerDeclaredType, this.visitors.boundingClassVisitor().visit(outerDeclaredType));
    // classBound() doesn't do anything here either:
    assertSame(innerDeclaredType, this.javacCodeTypes.classBound((Type)innerDeclaredType));
    assertSame(innerDeclaredType, this.visitors.boundingClassVisitor().visit(innerDeclaredType));
  }

  @Test
  final <X extends DataHolder<String>> void testReallyComplicated() throws NoSuchMethodException {
    final DataHolder<String> d = new DataHolder<>("hello");
    this.testReallyComplicated0(d);
  }

  private final <X extends DataHolder<String>> void testReallyComplicated0(final X x) throws NoSuchMethodException {
    final Object inner = x.data();
    final DeclaredType outerDeclaredType = Lang.declaredType(Lang.typeElement(DataHolder.class), Lang.declaredType(String.class));
    final DeclaredType innerDeclaredType = Lang.declaredType(outerDeclaredType, Lang.typeElement(inner.getClass()));
    // classBound() doesn't do anything here either:
    assertSame(innerDeclaredType, this.javacCodeTypes.classBound((Type)innerDeclaredType));
    assertSame(innerDeclaredType, this.visitors.boundingClassVisitor().visit(innerDeclaredType));

    final Method thisMethod = TestBoundingClassVisitor.class.getDeclaredMethod("testReallyComplicated0", DataHolder.class);
    java.lang.reflect.TypeVariable<Method> xx = null;
    for (final java.lang.reflect.TypeVariable<Method> tv : thisMethod.getTypeParameters()) {
      if (tv.getName().equals("X")) {
        xx = tv;
        break;
      }
    }
    final TypeVariable xxx = Lang.typeVariable(xx);
    // Here's where classBound() does something:
    assertNotSame(xxx, this.javacCodeTypes.classBound((Type)xxx));
    assertNotSame(xxx, this.visitors.boundingClassVisitor().visit(xxx));
  }

  private static class DataHolder<T> {

    private final Inner data;
    
    private DataHolder(final T data) {
      super();
      this.data = new Inner(data);
    }

    Inner data() {
      return this.data;
    }

    class Inner {

      private final T data;
      
      private Inner(final T data) {
        super();
        this.data = data;
      }

      final T data() {
        return this.data;
      }
      
    }
    
  }
  
}
