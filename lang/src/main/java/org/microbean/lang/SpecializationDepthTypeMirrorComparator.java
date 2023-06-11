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

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import java.util.function.Function;

import javax.lang.model.element.QualifiedNameable;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.microbean.lang.type.DelegatingTypeMirror;

public final class SpecializationDepthTypeMirrorComparator implements Comparator<TypeMirror> {

  private final TypeAndElementSource elementSource;

  private final Equality equality;

  private final Function<? super DelegatingTypeMirror, ? extends Iterable<? extends TypeMirror>> directSupertypes;

  public SpecializationDepthTypeMirrorComparator() {
    this(Lang.typeAndElementSource(), Lang.sameTypeEquality(), Lang::directSupertypes);
  }

  public SpecializationDepthTypeMirrorComparator(final Equality equality) {
    this(Lang.typeAndElementSource(), equality, Lang::directSupertypes);
  }

  public SpecializationDepthTypeMirrorComparator(final TypeAndElementSource elementSource,
                                                 final Equality equality) {
    this(elementSource, equality, Lang::directSupertypes);
  }

  public SpecializationDepthTypeMirrorComparator(final TypeAndElementSource elementSource,
                                                 final Equality equality,
                                                 final Function<? super DelegatingTypeMirror, ? extends Iterable<? extends TypeMirror>> directSupertypes) {
    super();
    this.elementSource = Objects.requireNonNull(elementSource, "elementSource");
    this.equality = Objects.requireNonNull(equality, "equality");
    this.directSupertypes = Objects.requireNonNull(directSupertypes, "directSupertypes");
  }

  @Override // Comparator<TypeMirror>
  public final int compare(final TypeMirror t, final TypeMirror s) {
    return
      t == s ? 0 :
      t == null ? 1 :
      s == null ? -1 :
      // Note this comparison is "backwards"
      Integer.signum(this.specializationDepth(DelegatingTypeMirror.of(s, this.elementSource, this.equality)) -
                     this.specializationDepth(DelegatingTypeMirror.of(t, this.elementSource, this.equality)));
  }

  public final int specializationDepth(final TypeMirror t) {
    return this.specializationDepth(DelegatingTypeMirror.of(t, this.elementSource, this.equality));
  }

  @SuppressWarnings("fallthrough")
  private final int specializationDepth(final DelegatingTypeMirror t) {
    // See
    // https://github.com/openjdk/jdk/blob/2e340e855b760e381793107f2a4d74095bd40199/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3570-L3615.
    switch (t.getKind()) {
    case DECLARED:
      if (((QualifiedNameable)t.asElement()).getQualifiedName().contentEquals("java.lang.Object")) {
        return 0;
      }
      // fall through
    case ARRAY:
    case INTERSECTION:
    case TYPEVAR:
      // My initial specialization depth is 0, although we know I will have at least one supertype (java.lang.Object)
      // because we already handled java.lang.Object, which has no supertypes, above.
      int sd = 0;
      for (final TypeMirror s : this.directSupertypes.apply(t)) {
        sd = Math.max(sd, this.specializationDepth(DelegatingTypeMirror.of(s, this.elementSource, this.equality)));
      }
      // My specialization depth is equal to the greatest one of my direct supertypes, plus one (representing me, a
      // subtype).
      return sd + 1;
    case ERROR:
      throw new AssertionError("t.getKind() == TypeKind.ERROR; t: " + t);
    default:
      throw new IllegalArgumentException("t: " + t + "; t.getKind(): " + t.getKind());
    }
  }

}
