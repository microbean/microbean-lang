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
import org.jboss.jandex.MethodParameterTypeTarget;
import org.jboss.jandex.TypeTarget;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

final class TestTypeUseAnnotationOnMethodParameterType {

  private TestTypeUseAnnotationOnMethodParameterType() {
    super();
  }

  /**
   * This test exists to show that Jandex does something properly that javac does not.
   *
   * <p>See the {@code org.microbean.lang.TestTypeUsages} test class for the {@code javac}-based analog, which does not
   * handle a type use annotation on a method parameter. I am still not sure if Jandex is wrong, or {@code javac} is
   * wrong.</p>
   *
   * @exception IOException if there was a problem with the {@link Indexer}
   */
  @Test
  final void testTypeUseAnnotationOnMethodParameterType() throws IOException {
    final Indexer indexer = new Indexer();
    indexer.indexClass(B.class);
    final IndexView i = indexer.complete();
    final ClassInfo ci = i.getClassByName(B.class.getName());
    for (final AnnotationInstance a : ci.annotations()) {
      final AnnotationTarget target = a.target();
      switch (target.kind()) {
      case TYPE:
        final TypeTarget tt = target.asType();
        switch (tt.usage()) {
        case METHOD_PARAMETER:
          final MethodParameterTypeTarget mptt = tt.asMethodParameterType();
          assertEquals(0, mptt.position());
          assertEquals("c", mptt.enclosingTarget().name());
          assertEquals("A", a.name().local());
          break;
        default:
          fail();
        }
        break;
      default:
        fail();
      }
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE_USE })
  public @interface A {}

  public static final class B {

    public B() {
      super();
    }
    
    public static final void c(@A String s) {}
    
  }
  
}
