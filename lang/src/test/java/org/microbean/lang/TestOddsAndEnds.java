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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestOddsAndEnds {

  private JavaLanguageModel jlm;
  
  private TestOddsAndEnds() {
    super();
  }

  @BeforeEach
  final void setup() {
    this.jlm = new JavaLanguageModel();
  }

  @AfterEach
  final void tearDown() {

  }

  @Test
  final void testTypeUseAnnotationOnClass() {
    final TypeElement b = jlm.typeElement("org.microbean.lang.TestOddsAndEnds.B");
    // B is actually a declaration annotation per the rules of the JLS!
    assertEquals(1, b.getAnnotationMirrors().size());
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE_USE }) // ...which includes TYPE and TYPE_PARAMETER
  private @interface A {}

  @A
  private static final class B {}
  
}
