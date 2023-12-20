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

import java.util.Comparator;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

@Deprecated(forRemoval = true)
// Only works on reference types (and IntersectionTypes).
public final class ClassesThenInterfacesTypeMirrorComparator implements Comparator<TypeMirror> {

  public static final ClassesThenInterfacesTypeMirrorComparator INSTANCE = new ClassesThenInterfacesTypeMirrorComparator();

  private ClassesThenInterfacesTypeMirrorComparator() {
    super();
  }

  @Override // Comparator<TypeMirror>
  public final int compare(final TypeMirror t, final TypeMirror s) {
    // See ../../../../../site/markdown/type-variables-and-intersection-types.md.
    return
      t == s ? 0 :
      t == null ? 1 :
      s == null ? -1 :
      iface(t) ? iface(s) ? 0 : 1 :
      iface(s) ? -1 : 0;
  }

  private static final boolean iface(final TypeMirror t) {
    return switch (t.getKind()) {
    case ARRAY -> false;
    case DECLARED -> ((DeclaredType)t).asElement().getKind().isInterface();
    case INTERSECTION -> iface(((IntersectionType)t).getBounds().get(0)); // will be DeclaredType (nothing else)
    case TYPEVAR -> iface(((TypeVariable)t).getUpperBound()); // will be DeclaredType, IntersectionType or TypeVariable (notably never ArrayType)
    case ERROR -> throw new AssertionError("t.getKind() == TypeKind.ERROR; t: " + t);
    default -> throw new IllegalArgumentException("t: " + t);
    };
  }

}
