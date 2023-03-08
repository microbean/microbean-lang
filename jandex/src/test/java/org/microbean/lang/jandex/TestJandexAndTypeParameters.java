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
package org.microbean.lang.jandex;

import java.io.IOException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeTarget;
import org.jboss.jandex.TypeVariable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

final class TestJandexAndTypeParameters {

  private TestJandexAndTypeParameters() {
    super();
  }

  @Test
  final void testTypeParameters() throws IOException {
    final Indexer indexer = new Indexer();
    indexer.indexClass(A.class);
    indexer.indexClass(C.class);
    final IndexView i = indexer.complete();
    final ClassInfo a = i.getClassByName(A.class.getName());
    assertEquals("B", a.typeParameters().get(0).identifier());
    assertEquals(1, a.annotations().size()); // remember that annotations() "descends into" the class, so indexes all of its "contained" annotations

    // Jandex doesn't have the notion of a type parameter
    // element/declaration. javax.lang.model.element.TypeParameterElement does.

    // This sucks. The annotation is clearly labeled as TYPE_PARAMETER and NOT TYPE_USE, but Jandex sees it as TYPE_USE
    // anyway (!).  However, annotations intended for type-variable-types-underlying-type-parameters are not a thing in the
    // Java language, apparently, though they show up in the javax.lang.model.* hierarchy.  The proper thing to do seems
    // to be to treat all cases that fall under this case as element/declaration annotations on the type parameter
    // elements, so this is "expected" according to Jandex's odd interpretation.
    //
    // Probably there will be a Jandex bug and/or refactoring in this area later.
    assertSame(TypeTarget.Usage.TYPE_PARAMETER, a.annotations().get(0).target().asType().usage());
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE_PARAMETER })
  private @interface C{}
  
  private static final class A<@C B> {

  }
  
}
