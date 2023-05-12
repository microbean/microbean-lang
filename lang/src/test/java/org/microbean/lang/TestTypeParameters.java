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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.util.List;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TestTypeParameters {

  private TestTypeParameters() {
    super();
  }

  @Test
  final void testTypeParameters() {
    final TypeElement a = new JavaLanguageModel().typeElement("org.microbean.lang.TestTypeParameters.A");
    final List<? extends TypeParameterElement> typeParameters = a.getTypeParameters();
    final TypeParameterElement b = typeParameters.get(0);
    assertEquals("B", b.getSimpleName().toString());

    // I'm expecting @C to be an annotation on B-the-type-parameter and not on B-the-type-variable.
    // I'm expecting @D to be an annotation on B-the-type-parameter and not on B-the-type-variable.
    // I'm expecting @E to be an annotation on B-the-type-parameter and not on B-the-type-variable.
    // I'm expecting @F to be an annotation on B-the-type-parameter and not on B-the-type-variable, but in fact it doesn't compile due to https://bugs.openjdk.org/browse/JDK-8303784.
    assertEquals(3, b.getAnnotationMirrors().size());
    assertEquals(0, b.asType().getAnnotationMirrors().size());
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE_PARAMETER })
  private @interface C{}

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE_USE }) // remember that this implies TYPE and TYPE_PARAMETER as well
  private @interface D{}

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE_PARAMETER, ElementType.TYPE_USE }) // effectively the same as just TYPE_USE
  private @interface E{}

  // Not applicable in JDK 19 or 20 to type parameter declarations, but should be; see https://bugs.openjdk.org/browse/JDK-8303784.
  // @Retention(RetentionPolicy.RUNTIME)
  // private @interface F{}
  
  private static final class A<@C @D @E /* @F */ B> {

  }
  
}
