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

import javax.lang.model.element.TypeElement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

final class TestJavaLanguageModel {

  private TestJavaLanguageModel() {
    super();
  }

  @Test
  public void testJavaLanguageModel() {
    final JavaLanguageModel jlm = new JavaLanguageModel();
    final TypeElement s = jlm.elements().getTypeElement("java.lang.String");
    assertNotNull(s);
    final TypeElement x = jlm.elements().getTypeElement("java.util.logging.Level");
    assertNotNull(x);
    jlm.close();
  }
  
}
