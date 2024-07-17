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

import java.io.IOException;
import java.io.UncheckedIOException;

import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;

import java.lang.invoke.MethodHandles;

import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;

import java.util.Comparator;
import java.util.Optional;

import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestModuleRelatedIssues {
  
  private TestModuleRelatedIssues() {
    super();
  }

  @Test
  final void reportModuleInformation() {
    assertEquals("org.microbean.lang", Lang.class.getModule().getName());
    final ModuleLayer bootLayer = ModuleLayer.boot();
    assertSame(Lang.class.getModule().getLayer(), bootLayer);

    System.out.println("ModuleLayer.boot().modules():");
    bootLayer.modules().stream()
      .map(Module::getName)
      .sorted()
      .forEach(System.out::println);
    System.out.println();

    System.out.println("ModuleFinder.ofSystem().findAll():");
    ModuleFinder.ofSystem().findAll().stream()
      .map(mr -> mr.descriptor().name())
      .sorted()
      .forEach(System.out::println);
    System.out.println();
    
    System.out.println("Lang.allModuleElements():");
    Lang.allModuleElements().stream()
      .map(e -> e.getQualifiedName().toString())
      .sorted()
      .forEach(System.out::println);
  }

  @Test
  final void testGetTypeElementRequiresNonNullModule() {
    assertThrows(NullPointerException.class, () -> Lang.pe().getElementUtils().getTypeElement(null, "bogus"));
  }
  
  @Test
  final void testGraal() {
    ModuleElement me = Lang.moduleElement("com.oracle.graal.graal_enterprise");
    assertNull(me);
    me = Lang.moduleElement("java.base");
    assertNotNull(me);
    assertNull(Lang.moduleElement("org.graalvm.truffle.compiler"));
  }
  
  @Test
  final void testModuleRelatedIssues() {
    // Maven puts the (modular) junit-jupiter-api jar file on the classpath, not the module path, so there's no module
    // by this name on the module paththat can be found.
    assertNull(Lang.moduleElement("org.junit.jupiter.api"));

    // The unnamed module at runtime looks like this:
    final Module m = Test.class.getModule();
    assertNotNull(m);
    assertFalse(m.isNamed());
    assertNull(m.getName());

    // Note the lack of ModuleElement parameter:
    TypeElement e = Lang.typeElement("org.junit.jupiter.api.Test");
    assertNotNull(e);

    final ModuleElement unnamedModule = Lang.moduleOf(e);
    assertTrue(unnamedModule.isUnnamed());

    // The unnamed module can "see" Test. Note that the ModuleElement parameter is not necessarily the module that
    // (indirectly) encloses Test!
    e = Lang.typeElement(unnamedModule, "org.junit.jupiter.api.Test");
    assertNotNull(e);
    assertEquals(unnamedModule, Lang.moduleOf(e));

    // java.base does not read the unnamed module, so should not be able to "see" Test. That is, e is not null because
    // org.junit.jupiter.api.Test does not "belong" to it, it is null because java.base does not read
    // org.junit.jupiter.api.Test (because it does not (cannot, except with command-line hacks) read the unnamed
    // module).
    e = Lang.typeElement(Lang.moduleElement("java.base"), "org.junit.jupiter.api.Test");
    assertNull(e);

    assertThrows(NullPointerException.class, () -> Lang.typeElement((ModuleElement)null, "org.junit.jupiter.api.Test"));

    // org.microbean.lang reads the unnamed module (every module reads the unnamed module) so should be able to "see"
    // Test. Note, to state the obvious, that Test is not enclosed by a package enclosed by org.microbean.lang;
    // org.microbean.lang is the "viewing" or reading module.
    e = Lang.typeElement(Lang.moduleElement("org.microbean.lang"), "org.junit.jupiter.api.Test");
    assertNotNull(e);

    
    
  }
  
}
