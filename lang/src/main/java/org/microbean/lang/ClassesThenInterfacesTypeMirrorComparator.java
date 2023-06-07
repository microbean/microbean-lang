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

// Only works on reference types (and IntersectionTypes).
public final class ClassesThenInterfacesTypeMirrorComparator implements Comparator<TypeMirror> {

  public static final ClassesThenInterfacesTypeMirrorComparator INSTANCE = new ClassesThenInterfacesTypeMirrorComparator();

  private ClassesThenInterfacesTypeMirrorComparator() {
    super();
  }

  @Override // Comparator<TypeMirror>
  public final int compare(final TypeMirror t, final TypeMirror s) {
    // A note on type variables and intersection types:
    //
    // An intersection type is just a way of representing the many bounds of a type variable in a single type. This also
    // means that IntersectionTypes must play by rules imposed by TypeVariables, the only types that "use" them. So in
    // the Java language model, a TypeVariable has exactly one upper bound, which may, itself, be an IntersectionType.
    //
    // If its sole bound is an IntersectionType, then *its* first (but never only) bound will be a DeclaredType.
    //
    // If its sole bound is not an IntersectionType, then it will be either a DeclaredType or a TypeVariable.
    //
    // Applying these rules together and recursively where necessary, we can tell whether any given TypeVariable "is an
    // interface" if either (a) its sole bound is a DeclaredType that "is an interface" or (b) its sole bound is an
    // IntersectionType whose first bound is a DeclaredType that "is an interface".
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
    case INTERSECTION -> iface(((IntersectionType)t).getBounds().get(0));
    case TYPEVAR -> iface(((TypeVariable)t).getUpperBound());
    case ERROR -> throw new AssertionError("t.getKind() == TypeKind.ERROR; t: " + t);
    default -> throw new IllegalArgumentException("t: " + t);
    };
  }

}
