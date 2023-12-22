/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2022–2023 microBean™.
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
package org.microbean.lang;

import java.lang.annotation.Annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.lang.model.element.AnnotationMirror;

import org.microbean.lang.element.Element;

import org.microbean.lang.type.TypeMirror;

/**
 * A mutable implementation of the {@link javax.lang.model.AnnotatedConstruct} interface.
 *
 * @author <a href="https://about.me/lairdnelson/" target="_top">Laird Nelson</a>
 */
public abstract sealed class AnnotatedConstruct implements javax.lang.model.AnnotatedConstruct permits Element, TypeMirror {

  private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

  private final List<AnnotationMirror> annotationMirrors;

  private final List<AnnotationMirror> unmodifiableAnnotationMirrors;

  /**
   * Creates a new {@link AnnotatedConstruct}.
   *
   * @see #AnnotatedConstruct(Collection)
   */
  protected AnnotatedConstruct() {
    this(List.of());
  }

  /**
   * Creates a new {@link AnnotatedConstruct}.
   *
   * @param annotationMirror an {@link AnnotationMirror} to be borne by the new object
   *
   * @see #AnnotatedConstruct(Collection)
   */
  protected AnnotatedConstruct(final AnnotationMirror annotationMirror) {
    this(List.of(annotationMirror));
  }

  /**
   * Creates a new {@link AnnotatedConstruct}.
   *
   * @param annotationMirrors a {@link Collection} of {@link AnnotationMirror}s to be borne by the new object
   */
  protected AnnotatedConstruct(final Collection<? extends AnnotationMirror> annotationMirrors) {
    super();
    if (annotationMirrors == null) {
      this.annotationMirrors = new ArrayList<>(5);
    } else {
      this.annotationMirrors = new ArrayList<>(Math.max(5, annotationMirrors.size()));
      this.addAnnotationMirrors(annotationMirrors);
    }
    this.unmodifiableAnnotationMirrors = Collections.unmodifiableList(this.annotationMirrors);
  }

  @Override // AnnotatedConstruct
  public <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
    throw new UnsupportedOperationException();
  }

  @Override // AnnotatedConstruct
  @SuppressWarnings("unchecked")
  public <A extends Annotation> A[] getAnnotationsByType(final Class<A> annotationType) {
    throw new UnsupportedOperationException();
  }

  @Override // AnnotatedConstruct
  public List<? extends AnnotationMirror> getAnnotationMirrors() {
    return this.unmodifiableAnnotationMirrors;
  }

  /**
   * Adds an {@link AnnotationMirror} to this {@link AnnotatedConstruct}.
   *
   * @param a the {@link AnnotationMirror} to add; must not be {@code null}
   *
   * @exception NullPointerException if {@code a} is {@code null}
   */
  public void addAnnotationMirror(final AnnotationMirror a) {
    if (this.annotationMirrors.contains(a)) {
      throw new IllegalArgumentException("a: " + a);
    }
    this.annotationMirrors.add(this.validateAnnotationMirror(a));
  }

  /**
   * Adds {@link AnnotationMirror}s to this {@link AnnotatedConstruct}.
   *
   * @param as the {@link AnnotationMirror}s to add; must not be {@code null} and must not produce {@code null} elements
   *
   * @exception NullPointerException if {@code as} is {@code null} or produces a {@code null} element
   */
  public final void addAnnotationMirrors(final Iterable<? extends AnnotationMirror> as) {
    for (final AnnotationMirror a : as) {
      this.addAnnotationMirror(a);
    }
  }

  /**
   * Returns a valid version of the supplied {@link AnnotationMirror}, or throws some kind of {@link RuntimeException}
   * if validation fails.
   *
   * <p>This implementation simply checks to see if the supplied {@link AnnotationMirror} is non-{@code null}.</p>
   *
   * @param a the {@link AnnotationMirror} to validate; must not be {@code null}
   *
   * @return a valid version of the supplied {@link AnnotationMirror}; this implementation simply returns the supplied
   * {@link AnnotationMirror}
   *
   * @exception NullPointerException if {@code a} is {@code null}
   */
  protected AnnotationMirror validateAnnotationMirror(final AnnotationMirror a) {
    return Objects.requireNonNull(a, "a");
  }

}
