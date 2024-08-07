/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2023–2024 microBean™.
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
import java.util.Objects;

import javax.lang.model.element.QualifiedNameable;

import javax.lang.model.type.TypeMirror;

import org.microbean.lang.type.DelegatingTypeMirror;

/**
 * A {@link Comparator} that partially orders certain {@linkplain TypeMirror#getKind() kinds} of {@link TypeMirror}s
 * according to the depths of their specialization hierarchies such that subtypes precede supertypes.
 *
 * <p><strong>This {@link Comparator} implementation is inconsistent with equals.</strong></p>
 *
 * @author <a href="https://about.me/lairdnelson/" target="_top">Laird Nelson</a>
 *
 * @see #compare(TypeMirror, TypeMirror)
 *
 * @see #specializationDepth(TypeMirror)
 */
public final class SpecializationDepthTypeMirrorComparator implements Comparator<TypeMirror> {


  /*
   * Instance fields.
   */


  private final TypeAndElementSource tes;

  private final Equality equality;


  /*
   * Constructors.
   */


  public SpecializationDepthTypeMirrorComparator(final TypeAndElementSource tes) {
    this(tes, null);
  }

  /**
   * Creates a new {@link SpecializationDepthTypeMirrorComparator}.
   *
   * @param tes a {@link TypeAndElementSource}; must not be {@code null}
   *
   * @param equality an {@link Equality}; may be {@code null} in which case a new {@link SameTypeEquality} will be used
   * instead
   *
   * @exception NullPointerException if {@code tes} is {@code null}
   *
   * @see Lang#typeAndElementSource()
   */
  public SpecializationDepthTypeMirrorComparator(final TypeAndElementSource tes,
                                                 final Equality equality) {
    super();
    this.tes = Objects.requireNonNull(tes, "tes");
    this.equality = equality == null ? new SameTypeEquality(this.tes) : equality;
  }


  /*
   * Instance methods.
   */


  @Override // Comparator<TypeMirror>
  public final int compare(final TypeMirror t, final TypeMirror s) {
    return
      t == s ? 0 :
      t == null ? 1 :
      s == null ? -1 :
      // Note this comparison is "backwards"
      Integer.signum(this.specializationDepth(DelegatingTypeMirror.of(s, this.tes, this.equality)) -
                     this.specializationDepth(DelegatingTypeMirror.of(t, this.tes, this.equality)));
  }

  /**
   * Returns the <em>specialization depth</em> of the supplied {@link TypeMirror}, which must be an {@linkplain
   * javax.lang.model.type.TypeKind#ARRAY array type}, a {@linkplain javax.lang.model.type.TypeKind#DECLARED declared
   * type}, an {@linkplain javax.lang.model.type.TypeKind#INTERSECTION intersection type} or a {@linkplain
   * javax.lang.model.type.TypeKind#TYPEVAR type variable}.
   *
   * <p>The specialization depth of the type representing {@link java.lang.Object java.lang.Object} is {@code 0}.</p>
   *
   * <p>The specialization depth of an immediate subclass of {@link Object} is {@code 1}.</p>
   *
   * <p>The specialization depth of an immediate subclass of an immediate subclass of {@link Object} is {@code 2}. And
   * so on.</p>
   *
   * @param t a {@link TypeMirror}; must not be {@code null}; must be an {@linkplain
   * javax.lang.model.type.TypeKind#ARRAY array type}, a {@linkplain javax.lang.model.type.TypeKind#DECLARED declared
   * type}, an {@linkplain javax.lang.model.type.TypeKind#INTERSECTION intersection type} or a {@linkplain
   * javax.lang.model.type.TypeKind#TYPEVAR type variable}
   *
   * @return {@code 0} or a positive {@code int} representing the specialization depth of the {@link TypeMirror} in question
   *
   * @exception NullPointerException if {@code t} is {@code null}
   *
   * @exception IllegalArgumentException if {@code t} is unsuitable for any reason
   */
  public final int specializationDepth(final TypeMirror t) {
    return this.specializationDepth(DelegatingTypeMirror.of(t, this.tes, this.equality));
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
      for (final TypeMirror s : this.tes.directSupertypes(t)) {
        sd = Math.max(sd, this.specializationDepth(DelegatingTypeMirror.of(s, this.tes, this.equality)));
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
