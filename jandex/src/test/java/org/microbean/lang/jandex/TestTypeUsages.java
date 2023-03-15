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
import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassExtendsTypeTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.EmptyTypeTarget;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodParameterTypeTarget;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeTarget;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

final class TestTypeUsages {

  private IndexView i;

  private TestTypeUsages() {
    super();
  }

  @BeforeEach
  final void setup() throws IOException {
    final Indexer indexer = new Indexer();
    indexer.indexClass(A.class);
    indexer.indexClass(B.class);
    indexer.indexClass(E.class);
    indexer.indexClass(F.class);
    indexer.indexClass(G.class);
    indexer.indexClass(H.class);
    indexer.indexClass(String.class);
    this.i = indexer.complete();
  }

  @AfterEach
  final void tearDown() {

  }

  @Test
  final void testB() {
    final ClassInfo ci = this.i.getClassByName(B.class.getName());

    for (final AnnotationInstance ai : ci.annotations()) {
      final AnnotationTarget target = ai.target();
      switch (target.kind()) {
      case CLASS:
        // @A on the class itself
        assertEquals("A", ai.name().local());
        assertSame(ci, target);
        break;
      case TYPE:
        final TypeTarget tt = target.asType();
        switch (tt.usage()) {
        case METHOD_PARAMETER:
          final MethodParameterTypeTarget mptt = tt.asMethodParameterType();
          assertEquals(0, mptt.position());
          assertEquals("c", mptt.enclosingTarget().name());
          assertEquals("A", ai.name().local());
          break;
        case EMPTY:
          final EmptyTypeTarget ett = tt.asEmpty();
          assertFalse(ett.isReceiver());
          final ArrayType at = ett.target().asArrayType();
          switch (ai.name().local()) {
          case "A":
            assertEquals(1, at.dimensions()); // surprising
            assertEquals(1, at.component().asArrayType().dimensions());
            break;
          case "E":
            assertEquals(1, at.dimensions());
            assertEquals("String", at.component().name().local());
            break;
          default:
            fail();
          }
          break;
        default:
          fail(tt.usage().toString());
        }
        break;
      default:
        fail();
      }
    }

  }

  @Test
  final void testG() {
    final ClassInfo ci = this.i.getClassByName(G.class.getName());

    final List<AnnotationInstance> ais = ci.annotations();
    assertEquals(3, ais.size());
    for (final AnnotationInstance ai : ais) {
      final AnnotationTarget annotationTarget = ai.target();
      switch (annotationTarget.kind()) {
      case TYPE:
        final TypeTarget tt = annotationTarget.asType();
        switch (tt.usage()) {
        case CLASS_EXTENDS: // really?!
          final ClassExtendsTypeTarget cett = tt.asClassExtends();

          final ClassInfo enclosingTarget = cett.enclosingTarget().asClass();
          assertSame(ci, enclosingTarget);
          
          assertEquals(65535, cett.position()); // 65535 means superclass; see javadoc
          // How do we know this? How do we know that it isn't, say, F?
          //
          // Obviously for this test we can just assert that the target is the type named by "java.lang.String".  But in a
          // generic library we'll need a recipe, so we'll do the recipe here.
          final Type relevantSupertype = cett.position() == 65535 ? enclosingTarget.superClassType() : enclosingTarget.interfaceTypes().get(cett.position());

          final Type type = cett.target();
          if (type == relevantSupertype) {
            // The annotation in question is a type use annotation on the supertype itself (in this test, it's @H-on-F<@E String, @E String>).
            assertEquals("H", ai.name().local());
          } else {
            // The annotation in question is a type use annotation on the supertype's type arguments, which implies that
            // the supertype is a parameterized type.
            assertSame(Type.Kind.PARAMETERIZED_TYPE, relevantSupertype.kind());

            for (final Type typeArgument : relevantSupertype.asParameterizedType().arguments()) {
              // In this test, there are two type arguments, both of type String, both annotated with @E. What is
              // particularly interesting is that Jandex stores them with one object.
              assertSame(type, typeArgument);

              // Anyway, the point is there is no way to distinguish them at all.
            }
          }
          
          switch (relevantSupertype.kind()) {
          case CLASS:
            // (In this test we know this to be false. In the recipe, this would tell us unambiguously the type usage
            // annotation is on the actual thing being extended/implemented.)
            fail();
          case PARAMETERIZED_TYPE:
            // (In this test we know this to be true.)
            
            break;
          default:
            fail();
          }
        
          // This is really stupid. I guess once the position is 65535, you get the supertype directly?
          final Type supertype = ci.superClassType();
          // assertEquals("String", cett.target().name().local());
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


  /*
   * Inner and nested classes.
   */


  @Retention(RUNTIME)
  // Note as an interesting curiosity that TYPE_USE implies ANNOTATION_TYPE, TYPE and TYPE_PARAMETER as well.  See
  // https://mail.openjdk.org/pipermail/compiler-dev/2023-February/022200.html.
  @Target({ TYPE, TYPE_USE })
  public @interface A {}

  @Retention(RUNTIME)
  // Note as an interesting curiosity that TYPE_USE implies ANNOTATION_TYPE, TYPE and TYPE_PARAMETER as well.  See
  // https://mail.openjdk.org/pipermail/compiler-dev/2023-February/022200.html.
  @Target({ TYPE_USE })
  public @interface E {}

  @Retention(RUNTIME)
  // Note as an interesting curiosity that TYPE_USE implies ANNOTATION_TYPE, TYPE and TYPE_PARAMETER as well.  See
  // https://mail.openjdk.org/pipermail/compiler-dev/2023-February/022200.html.
  @Target({ TYPE_USE })
  public @interface H {}

  @A
  private static final class B {

    private B() {
      super();
    }

    public static final void c(@A String s) {}

    // Yow. @A annotates the array type (of the type denoted by String[][]). @E annotaes the component type (denoted by
    // String[]).
    public static final String @A [] @E [] d() {
      return null;
    }

  }

  private static class F<T, U> {}

  private static class G extends @H F<@E String, @E String> {}



}
