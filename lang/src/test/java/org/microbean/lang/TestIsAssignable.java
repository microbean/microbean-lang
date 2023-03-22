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

import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.microbean.lang.type.Types;

import org.microbean.lang.visitor.AsSuperVisitor;
import org.microbean.lang.visitor.ContainsTypeVisitor;
import org.microbean.lang.visitor.EraseVisitor;
import org.microbean.lang.visitor.IsSameTypeVisitor;
import org.microbean.lang.visitor.SubtypeVisitor;
import org.microbean.lang.visitor.SupertypeVisitor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestIsAssignable {

  private JavaLanguageModel jlm;

  private TestIsAssignable() {
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
  final void testIsAssignable() {
    final ElementSource es = this.jlm;
    final Types types = new Types(es);
    final EraseVisitor eraseVisitor = new EraseVisitor(es, types);
    final SupertypeVisitor supertypeVisitor = new SupertypeVisitor(es, types, eraseVisitor);

    // These have cycles.
    final ContainsTypeVisitor containsTypeVisitor = new ContainsTypeVisitor(es, types);
    final IsSameTypeVisitor isSameTypeVisitor = new IsSameTypeVisitor(es, containsTypeVisitor, supertypeVisitor, true);
    final SubtypeVisitor subtypeVisitor = new SubtypeVisitor(es, types, supertypeVisitor, isSameTypeVisitor);
    final AsSuperVisitor asSuperVisitor = new AsSuperVisitor(es, null, types, supertypeVisitor, subtypeVisitor);
    containsTypeVisitor.setSubtypeVisitor(subtypeVisitor);
    subtypeVisitor.setContainsTypeVisitor(containsTypeVisitor);

    assertSame(Boolean.TRUE, subtypeVisitor.visit(es.element("java.lang.String").asType(), es.element("java.lang.Object").asType()));

    final DeclaredType listString = this.jlm.types().getDeclaredType((TypeElement)es.element("java.util.List"), es.element("java.lang.String").asType());
    final DeclaredType listQuestionMark = this.jlm.types().getDeclaredType((TypeElement)es.element("java.util.List"), this.jlm.types().getWildcardType(null, null));

    assertFalse(this.jlm.types().isAssignable(listQuestionMark, listString));
    assertSame(Boolean.FALSE, subtypeVisitor.visit(listQuestionMark, listString));

    assertTrue(this.jlm.types().isAssignable(listString, listQuestionMark));
    assertSame(Boolean.TRUE, subtypeVisitor.visit(listString, listQuestionMark));

  }
  
}
