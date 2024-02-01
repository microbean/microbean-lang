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

import java.util.Comparator;
import java.util.Objects;

import java.util.function.Predicate;

import javax.lang.model.type.TypeMirror;

/**
 * A {@link Comparator} of {@link TypeMirror}s that uses a {@link Predicate} to compare instances.
 *
 * @author <a href="https://about.me/lairdnelson/" target="_top">Laird Nelson</a>
 */
public class TestingTypeMirrorComparator implements Comparator<TypeMirror> {


  /*
   * Instance fields.
   */


  private final Predicate<? super TypeMirror> p;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link TestingTypeMirrorComparator}.
   *
   * @param p a {@link Predicate} for use by the {@link #compare(TypeMirror, TypeMirror)} method; must not be {@code
   * null}
   *
   * @exception NullPointerException if {@code p} is {@code null}
   */
  public TestingTypeMirrorComparator(final Predicate<? super TypeMirror> p) {
    super();
    this.p = Objects.requireNonNull(p, "p");
  }


  /*
   * Instance methods.
   */


  /**
   * Compares {@code t} with {@code s} using the {@link Predicate} supplied at {@linkplain
   * #TestingTypeMirrorComparator(Predicate) construction time} ({@code p}).
   *
   * @param t a {@link TypeMirror}; may be {@code null}
   *
   * @param s a {@link TypeMirror}; may be {@code null}
   *
   * @return {@code 0} if both {@link TypeMirror}s are indistinguishable, a negative value if {@code t} logically
   * precedes {@code s}, or a positive value if {@code s} logically precedes {@code t}
   */
  @Override // Comparator<TypeMirror>
  public final int compare(final TypeMirror t, final TypeMirror s) {
    return
      t == s ? 0 :
      t == null ? 1 :
      s == null ? -1 :
      p.test(t) ? p.test(s) ? 0 : -1 :
      p.test(s) ? 1 : 0;
  }

}
