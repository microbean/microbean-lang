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

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public final class TypeVariablesFirstTypeMirrorComparator implements Comparator<TypeMirror> {

  public static final TypeVariablesFirstTypeMirrorComparator INSTANCE = new TypeVariablesFirstTypeMirrorComparator();

  private TypeVariablesFirstTypeMirrorComparator() {
    super();
  }

  @Override // Comparator<TypeMirror>
  public final int compare(final TypeMirror t, final TypeMirror s) {
    return
      t == s ? 0 :
      t == null ? 1 :
      s == null ? -1 :
      t.getKind() == TypeKind.TYPEVAR ? s.getKind() == TypeKind.TYPEVAR ? 0 : -1 :
      s.getKind() == TypeKind.TYPEVAR ? 1 : 0;
  }

}
