/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2022 microBean™.
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

public abstract sealed class AnnotatedConstruct implements javax.lang.model.AnnotatedConstruct permits Element, TypeMirror {

  private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

  private final List<AnnotationMirror> annotationMirrors;

  private final List<AnnotationMirror> unmodifiableAnnotationMirrors;

  protected AnnotatedConstruct() {
    this(List.of());
  }

  protected AnnotatedConstruct(final AnnotationMirror annotationMirror) {
    this(List.of(annotationMirror));
  }

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

  public void addAnnotationMirror(final AnnotationMirror a) {
    this.annotationMirrors.add(this.validateAnnotationMirror(a));
  }

  public final void addAnnotationMirrors(final Iterable<? extends AnnotationMirror> as) {
    for (final AnnotationMirror a : as) {
      this.addAnnotationMirror(a);
    }
  }

  protected AnnotationMirror validateAnnotationMirror(final AnnotationMirror a) {
    return Objects.requireNonNull(a, "a");
  }

}
