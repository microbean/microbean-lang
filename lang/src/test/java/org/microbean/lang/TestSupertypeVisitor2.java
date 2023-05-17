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

import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import com.sun.tools.javac.code.Type;

import com.sun.tools.javac.model.JavacTypes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.microbean.lang.type.Types;

import org.microbean.lang.visitor.EraseVisitor;
import org.microbean.lang.visitor.SupertypeVisitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

final class TestSupertypeVisitor2 {

  private com.sun.tools.javac.code.Types javacCodeTypes;

  private TestSupertypeVisitor2() {
    super();
  }

  @BeforeEach
  final void setup() throws IllegalAccessException, NoSuchFieldException {
    final Field f = JavacTypes.class.getDeclaredField("types");
    assertTrue(f.trySetAccessible());
    this.javacCodeTypes = (com.sun.tools.javac.code.Types)f.get(Lang.pe().getTypeUtils());
  }

  @Test
  final void testInteger() {
    final TypeElement integerElement = Lang.typeElement("java.lang.Integer");
    final DeclaredType integerElementType = (DeclaredType)integerElement.asType();
    assertSame(TypeKind.DECLARED, integerElementType.getKind());

    final Type javacSupertype = this.javacCodeTypes.supertype((Type)integerElementType);

    // Let's try it with our visitor.

    // Set up the fundamentals.
    final ElementSource es = (m, n) -> Lang.typeElement(Lang.moduleElement(m), n);
    final Types types = new Types(es);
    final EraseVisitor eraseVisitor = new EraseVisitor(es, types);
    final SupertypeVisitor supertypeVisitor = new SupertypeVisitor(es, types, eraseVisitor);

    final TypeMirror supertype = supertypeVisitor.visit(integerElementType);

    assertSame(javacSupertype, supertype);

    // How about superinterfaces?

    final List<Type> javacInterfaces = this.javacCodeTypes.interfaces((Type)integerElementType);
    final List<? extends TypeMirror> interfaces = supertypeVisitor.interfacesVisitor().visit(integerElementType);
    assertSame(javacInterfaces, interfaces);

  }

  @Test
  final void testSupertypeOfInterface() {
    final TypeElement serializableElement = Lang.typeElement("java.io.Serializable");
    final DeclaredType serializableElementType = (DeclaredType)serializableElement.asType();
    assertSame(TypeKind.DECLARED, serializableElementType.getKind());
    final Type javacSerializableSupertype = this.javacCodeTypes.supertype((Type)serializableElementType);
    assertSame(Lang.declaredType("java.lang.Object"), javacSerializableSupertype);
  }

  @Test
  final void testSupertypeOfObject() {
    assertSame(Lang.noType(TypeKind.NONE), this.javacCodeTypes.supertype((Type)Lang.declaredType("java.lang.Object")));
  }

}
