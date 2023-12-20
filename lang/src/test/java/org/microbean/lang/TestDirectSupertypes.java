/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2022–2023 microBean™.
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

import java.util.List;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.microbean.lang.Lang.directSupertypes;
import static org.microbean.lang.Lang.declaredType;
import static org.microbean.lang.Lang.sameType;
import static org.microbean.lang.Lang.unwrap;

final class TestDirectSupertypes {

  private TestDirectSupertypes() {
    super();
  }

  @Test
  final void testDirectSupertypes() {
    final DeclaredType object = declaredType("java.lang.Object");
    List<? extends TypeMirror> dsts = directSupertypes(object);
    assertEquals(List.of(), dsts);    
    dsts = directSupertypes(declaredType("java.lang.Cloneable"));
    assertEquals(1, dsts.size());
    assertSame(unwrap(object), unwrap(dsts.get(0)));

    final DeclaredType list = declaredType("java.util.List");
    dsts = directSupertypes(list);
    assertEquals(2, dsts.size());
    assertTrue(sameType(unwrap(object), unwrap(dsts.get(0))));
  }
  
}
