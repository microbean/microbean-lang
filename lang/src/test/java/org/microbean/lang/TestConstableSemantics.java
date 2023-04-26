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

import java.lang.constant.ConstantDesc;

import java.lang.invoke.MethodHandles;

import java.util.Optional;

import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestConstableSemantics {

  private TestConstableSemantics() {
    super();
  }

  @Test
  final void testDeclaredType() throws ReflectiveOperationException {
    final ModuleElement javaBase = Lang.moduleElement("java.base");
    assertNotNull(javaBase);
    final TypeElement javaLangString = Lang.typeElement(javaBase, "java.lang.String");
    assertNotNull(javaLangString);
    final DeclaredType t = Lang.declaredType(null, javaLangString);
    assertNotNull(t);
    final Optional<? extends ConstantDesc> o = Lang.describeConstable(t);
    assertTrue(o.isPresent());
    assertTrue(Lang.types().isSameType(t, (TypeMirror)o.orElseThrow(AssertionError::new).resolveConstantDesc(MethodHandles.lookup())));
  }
  
}
