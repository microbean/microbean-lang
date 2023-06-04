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
package org.microbean.lang.bytebuddy;

import javax.lang.model.type.TypeKind;

import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;

import net.bytebuddy.pool.TypePool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.microbean.lang.Lang.declaredType;
import static org.microbean.lang.Lang.primitiveType;
import static org.microbean.lang.Lang.typeElement;

final class TestByteBuddy2 {

  private ByteBuddy2 bb;
  
  private TestByteBuddy2() {
    super();
  }

  @BeforeEach
  final void setup() {
    this.bb = new ByteBuddy2();
  }
  
}
