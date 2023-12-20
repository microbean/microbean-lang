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
package org.microbean.lang.bytebuddy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import java.util.List;

import net.bytebuddy.description.annotation.AnnotationDescription;

import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;

import net.bytebuddy.pool.TypePool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class TestByteBuddy {

  private TestByteBuddy() {
    super();
  }

  @Test
  final void testModeling() {
    // This gives you what amounts to a parameterized type with no type arguments. Byte Buddy calls it a
    // TypeDescription.Generic of Sort NON_GENERIC (which is nonsensical since List is definitely a generic class).  You
    // can't get its type arguments because the Sort is NON_GENERIC.  This is like a crippled DeclaredType resulting
    // from elements.getTypeElement("java.util.List").asType() (note that you can call DeclaredType#getTypeArguments()
    // and you will get back an array of TypeVariable types, not an IllegalStateException!).
    final TypeDescription.Generic listType = TypeDefinition.Sort.describe(List.class);
    assertSame(TypeDefinition.Sort.NON_GENERIC, listType.getSort());
    assertThrows(IllegalStateException.class, listType::getTypeArguments); // this is just plain weird

    // To "see" its element's type parameters you "erase" the type (which didn't have any arguments anyway so it's not
    // really erasing).  This is really kind of like DeclaredType#asElement(), which is not really an erasing operation.
    // This whole thing is exceptionally strange.
    final TypeDescription listElement = listType.asErasure(); // this is also weird
    final List<TypeDescription.Generic> typeParameters = listElement.getTypeVariables(); // these are type PARAMETERS, dang it
    assertEquals(1, typeParameters.size());
    final TypeDescription.Generic e = typeParameters.get(0); // this is basically a TypeParameterElement
    assertSame(TypeDefinition.Sort.VARIABLE, e.getSort()); // this is kind of like TypeParameterElement#asType()
    assertEquals("E", e.getSymbol()); // this is also weird; types don't have names
    
  }

  @Test
  final void testModeling2() {
    final TypeDescription.Generic listTypeViaSortDescribe = TypeDefinition.Sort.describe(List.class);
    final TypeDescription listTypeViaTypePoolDescribe = TypePool.Default.ofSystemLoader().describe(List.class.getName()).resolve();
    assertEquals(listTypeViaSortDescribe, listTypeViaTypePoolDescribe);
    assertEquals(listTypeViaTypePoolDescribe, listTypeViaSortDescribe);

    final TypeDescription.Generic x = listTypeViaTypePoolDescribe.asGenericType();
    assertEquals(listTypeViaTypePoolDescribe, x);
    assertEquals(x, listTypeViaTypePoolDescribe);
    assertEquals(listTypeViaSortDescribe, x);
    assertEquals(x, listTypeViaSortDescribe);
    
    assertEquals(listTypeViaSortDescribe.asErasure(), listTypeViaTypePoolDescribe);
    assertSame(listTypeViaTypePoolDescribe, listTypeViaTypePoolDescribe.asErasure());
  }

  @Test
  final void testWeirdTypeUseAnnotations() {
    // The upshot of all of this is that if you have a TypeDescription.Generic in your hand, you don't know whether it
    // represents a type *declaration* or a type *use* (an element or a type).

    // Here is an example of a TypeDescription.Generic describing a type *use* (the type underlying "@Glop Goop" in
    // "implements @Glop Goop" in the Gorp class below).  Note that it has annotations, namely type use annotations:
    final TypeDescription.Generic goopType = TypeDefinition.Sort.describe(Gorp.class).getInterfaces().get(0);
    final AnnotationDescription glopTypeUse = goopType.getDeclaredAnnotations().get(0);
    assertEquals("Glop", glopTypeUse.getAnnotationType().getSimpleName());

    // Here is an example of a TypeDescription.Generic describing a type *declaration* (the actual Goop interface
    // below).  Note that it has no annotations.
    final TypeDescription.Generic goop = TypeDefinition.Sort.describe(Goop.class);
    assertEquals(0, goop.getDeclaredAnnotations().size());
  }

  private static final class Gorp implements @Glop Goop {

  }

  private static interface Goop {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.TYPE_USE })
  private @interface Glop {

  }
  
}
