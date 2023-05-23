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

import java.lang.reflect.Field;

import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;

import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import com.sun.tools.javac.model.JavacTypes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestOuterInner {

  private com.sun.tools.javac.code.Types javacCodeTypes;

  private TestOuterInner() {
    super();
  }

  @BeforeEach
  final void setup() throws IllegalAccessException, NoSuchFieldException {
    final Field f = JavacTypes.class.getDeclaredField("types");
    assertTrue(f.trySetAccessible());    
    this.javacCodeTypes = (com.sun.tools.javac.code.Types)f.get(Lang.pe().getTypeUtils());
  }

  @Test
  final void testOuterInner() {
    final DataHolder<String> outer = new DataHolder<>("hello");
    final DataHolder<String>.Inner inner = outer.data();
    // Hmm. OK.
    final TypeElement e = Lang.typeElement(DataHolder.class);
    assertNotNull(e);
    final DeclaredType outerDeclaredType = Lang.declaredType(e, Lang.declaredType(String.class));
    assertSame(TypeKind.DECLARED, outerDeclaredType.getKind());
    final DeclaredType innerDeclaredType = Lang.declaredType(outerDeclaredType, Lang.typeElement(inner.getClass()));
    assertSame(TypeKind.DECLARED, innerDeclaredType.getKind());
  }

  private static class DataHolder<T> {

    private final Inner data;
    
    private DataHolder(final T data) {
      super();
      this.data = new Inner(data);
    }

    private Inner data() {
      return this.data;
    }

    private class Inner {

      private final T data;
      
      private Inner(final T data) {
        super();
        this.data = data;
      }

      private final T data() {
        return this.data;
      }
      
    }
    
  }
  
  
}
