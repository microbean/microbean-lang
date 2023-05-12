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

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;

import com.sun.tools.javac.model.JavacTypes;

import java.lang.reflect.Field;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.microbean.lang.visitor.AsSuperVisitor;
import org.microbean.lang.visitor.ContainsTypeVisitor;
import org.microbean.lang.visitor.EraseVisitor;
import org.microbean.lang.visitor.SameTypeVisitor;
import org.microbean.lang.visitor.SubtypeVisitor;
import org.microbean.lang.visitor.SupertypeVisitor;
import org.microbean.lang.visitor.Visitors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestAsSuper {

  private JavaLanguageModel jlm;
  
  private com.sun.tools.javac.code.Types javacCodeTypes;

  private Visitors visitors;
  
  private SameTypeVisitor sameTypeVisitor;

  private AsSuperVisitor asSuperVisitor;
  
  private TestAsSuper() {
    super();
  }

  @BeforeEach
  final void setup() throws IllegalAccessException, NoSuchFieldException {
    this.jlm = new JavaLanguageModel();
    final Field f = JavacTypes.class.getDeclaredField("types");
    assertTrue(f.trySetAccessible());
    this.javacCodeTypes = (com.sun.tools.javac.code.Types)f.get(Lang.pe().getTypeUtils());
    this.visitors = new Visitors(this.jlm);
    /*
    final org.microbean.lang.type.Types types = new org.microbean.lang.type.Types(this.jlm);
    final EraseVisitor eraseVisitor = new EraseVisitor(this.jlm, types);
    final SupertypeVisitor supertypeVisitor = new SupertypeVisitor(this.jlm, types, eraseVisitor);

    // These have cycles.
    final ContainsTypeVisitor containsTypeVisitor = new ContainsTypeVisitor(this.jlm, types);
    this.sameTypeVisitor = new SameTypeVisitor(this.jlm, containsTypeVisitor, supertypeVisitor, true);
    final SubtypeVisitor subtypeVisitor = new SubtypeVisitor(this.jlm, types, supertypeVisitor, this.sameTypeVisitor);
    this.asSuperVisitor = new AsSuperVisitor(this.jlm, null, types, supertypeVisitor, subtypeVisitor);
    containsTypeVisitor.setSubtypeVisitor(subtypeVisitor);
    subtypeVisitor.setContainsTypeVisitor(containsTypeVisitor);
    */
  }

  @AfterEach
  final void tearDown() {

  }

  @Test
  final void testAsSuperStringObject() {
    final TypeElement stringElement = this.jlm.typeElement("java.lang.String");
    final DeclaredType stringTypeDeclaration = (DeclaredType)stringElement.asType();
    final Element objectElement = this.jlm.typeElement("java.lang.Object");
    final DeclaredType objectTypeDeclaration = (DeclaredType)objectElement.asType();
    assertSame(objectElement, objectTypeDeclaration.asElement());
    assertSame(objectElement, org.microbean.lang.type.Types.asElement(objectTypeDeclaration, true));
    final DeclaredType stringTypeSuperclass = (DeclaredType)stringElement.getSuperclass();
    final Element stringTypeSuperclassElement = stringTypeSuperclass.asElement();
    assertSame(objectElement, stringTypeSuperclassElement);
    // WTF; this fails:
    // assertSame(objectTypeDeclaration, stringTypeSuperclass);
    assertTrue(this.javacCodeTypes.isSameType((Type)objectTypeDeclaration, (Type)stringTypeSuperclass));
    assertTrue(this.visitors.sameTypeVisitor().visit(objectTypeDeclaration, stringTypeSuperclass));
    assertAsSuper(objectTypeDeclaration, stringTypeDeclaration, objectElement);
  }

  @Test
  final void testGorp() {
    // The element denoted by java.util.List.  Its underlying type declaration is the type denoted by java.util.List<E>.
    final TypeElement listElement = this.jlm.typeElement("java.util.List");

    // The type denoted by java.util.List<E>
    final DeclaredType listTypeDeclaration = (DeclaredType)listElement.asType();

    // The type denoted by java.util.List, i.e. a raw type.
    final DeclaredType rawListType = this.jlm.declaredType(listElement); // note: no varargs type arguments supplied
    assertTrue(((Type)rawListType).isRaw());

    // The raw List type is not the same as the List<E> type declaration.
    assertNotSame(rawListType, listTypeDeclaration);

    // The type denoted by java.util.List<?>.
    final DeclaredType listQuestionMarkType =
      this.jlm.declaredType(listElement, this.jlm.wildcardType());
    assertFalse(((Type)listQuestionMarkType).isRaw());
    
    assertSame(listElement, listQuestionMarkType.asElement());
    assertSame(listElement, rawListType.asElement());

    assertSame(listElement, org.microbean.lang.type.Types.asElement(rawListType, true));
    
    // Why is the expected type rawListType? Because the asSuper visitor's visitClassType() method, line 2175 or so,
    // says:
    //
    //   if (t.tsym == sym)
    //       return t;
    //
    // So here if rawListType.asElement() == listElement, which it does, as we've proven above, asSuper() will return
    // rawListType.
    //
    // So no matter what parameterization the supplied type represents, if the type it parameterizes (which is the type
    // declared by the element) "belongs to" the supplied element, then the supplied parameterized type is simply
    // returned.
    assertAsSuper(rawListType, rawListType, listElement);
  }

  private final void assertAsSuper(final TypeMirror expected, final TypeMirror t, final Element e) {
    assertSameType(expected, this.javacCodeTypes.asSuper((Type)t, (Symbol)e));
    assertSameType(expected, this.visitors.asSuperVisitor().visit(t, e));
  }

  private final void assertSameType(final TypeMirror t, final TypeMirror s) {
    assertTrue(this.javacCodeTypes.isSameType((Type)t, (Type)s));
    assertTrue(this.visitors.sameTypeVisitor().visit(t, s));
  }

  
}
