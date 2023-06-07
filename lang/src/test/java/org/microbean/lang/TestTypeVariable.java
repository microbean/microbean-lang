/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2023 microBean™.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.microbean.lang;

import java.lang.reflect.TypeVariable;

import java.util.Arrays;

import java.util.concurrent.ConcurrentMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TestTypeVariable {

  private TestTypeVariable() {
    super();
  }

  @Test
  final <T extends Cloneable & ConcurrentMap<String, String>> void testTypeVariable() throws ReflectiveOperationException {
    final TypeVariable<?> tv = this.getClass().getDeclaredMethod("testTypeVariable").getTypeParameters()[0];
    // Note that in all three cases the bounds are as listed, not sorted according to specificity.
    assertEquals("[interface java.lang.Cloneable, java.util.concurrent.ConcurrentMap<java.lang.String, java.lang.String>]", Arrays.asList(tv.getBounds()).toString());
    assertEquals("[java.lang.Cloneable, java.util.concurrent.ConcurrentMap<java.lang.String,java.lang.String>]", Lang.typeParameterElement(tv).getBounds().toString());
    assertEquals("java.lang.Object&java.lang.Cloneable&java.util.concurrent.ConcurrentMap<java.lang.String,java.lang.String>", Lang.typeVariable(tv).getUpperBound().toString());
  }
  
}
