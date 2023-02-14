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

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.EmptyTypeTarget;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.TypeTarget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

final class TestJandexTypeUseIssue {

  private TestJandexTypeUseIssue() {
    super();
  }

  @D
  private static final class A {

    // This @D is both:
    // * an annotation on the b() element
    // * a type-use annotation on the return type of b() (String)
    @D
    private static final String b() {

      // This @D is never seen
      @D
      class C {};

      return "Hello";
      
    }
    
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.METHOD, ElementType.TYPE, ElementType.TYPE_USE })
  private @interface D {}
  
  @Test
  final void testTypeUse() throws ClassNotFoundException, IOException {
    final Indexer indexer = new Indexer();
    indexer.indexClass(A.class);
    indexer.indexClass(Class.forName(A.class.getName() + "$1C"));
    final IndexView i = indexer.complete();
    final ClassInfo ci = i.getClassByName(A.class.getName());

    // C does not show up as a member class, as it should not.
    assertEquals(0, ci.memberClasses().size());

    // 3 is:
    // * 1 occurrence of @D on A
    // * 2 occurrences of @D:
    //   * 1 occurrence of @D on the usage of the return type of the b() method
    //   * 1 occurrence of @D on the b() method element
    // * 0 occurrences of @D on C (C is never seen by Jandex)
    assertEquals(3, ci.annotations().size());

    int count = 0;
    for (final AnnotationInstance a : ci.annotations()) {
      final AnnotationTarget target = a.target();
      switch (target.kind()) {
      case CLASS:
        if (target.asClass() != ci) {
          fail();
        }
        count++;
        break;
      case METHOD:
        assertEquals("b", target.asMethod().name());
        count++;
        break;
      case TYPE:
        final TypeTarget tt = target.asType();
        switch (tt.usage()) {
        case EMPTY:
          final EmptyTypeTarget ett = tt.asEmpty();
          assertFalse(ett.isReceiver());
          assertEquals("java.lang.String", ett.target().name().toString());
          assertEquals("b", ett.enclosingTarget().asMethod().name());
          count++;
          break;
        default:
          fail();
        }
        break;
      default:
        fail();
      }
    }
    assertEquals(3, count);

  }
}
