/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2022–2024 microBean™.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import java.util.concurrent.ConcurrentMap;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import static org.microbean.lang.Lang.declaredType;
import static org.microbean.lang.Lang.primitiveType;
import static org.microbean.lang.Lang.typeElement;
import static org.microbean.lang.Lang.typeVariable;
import static org.microbean.lang.Lang.wildcardType;

final class TestComparators {

  private TestComparators() {
    super();
  }

  @SuppressWarnings("rawtypes")
  @Test
  final <T extends Cloneable & ConcurrentMap<? extends List<? extends Integer>, ? super ArrayList>> void spike() throws ReflectiveOperationException {
    final List<TypeMirror> l = new ArrayList<>();
    final TypeVariable tv = typeVariable(this.getClass().getDeclaredMethod("spike"), "T");
    l.add(tv);
    final TypeMirror listQuestionMarkExtendsString = declaredType(null, typeElement("java.util.List"), wildcardType(declaredType("java.lang.String"), null));
    l.add(listQuestionMarkExtendsString);
    final TypeMirror list = declaredType(null, typeElement("java.util.List"));
    l.add(list);

    final TypeAndElementSource tes = Lang.typeAndElementSource();
    final Comparator<TypeMirror> c =
      new TestingTypeMirrorComparator(t -> t.getKind() == TypeKind.DECLARED && !((DeclaredType)t).asElement().getKind().isInterface())
      .thenComparing(new TestingTypeMirrorComparator(t -> t.getKind() == TypeKind.DECLARED && ((DeclaredType)t).asElement().getKind().isInterface()))
      .thenComparing(new SpecializationDepthTypeMirrorComparator(tes, new SameTypeEquality(tes), Lang::directSupertypes))
      .thenComparing(NameTypeMirrorComparator.INSTANCE);

    Collections.sort(l, c);
  }

}
