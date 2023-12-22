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
package org.microbean.lang.element;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;

import javax.lang.model.type.DeclaredType;

import org.microbean.lang.Equality;

/**
 * A mutable implementation of the {@link javax.lang.model.element.AnnotationMirror} interface.
 *
 * @author <a href="https://about.me/lairdnelson/" target="_top">Laird Nelson</a>
 */
public class AnnotationMirror implements javax.lang.model.element.AnnotationMirror {


  /*
   * Instance fields.
   */


  private DeclaredType annotationType;

  private final Map<ExecutableElement, AnnotationValue> elementValues;

  private final Map<ExecutableElement, AnnotationValue> unmodifiableElementValues;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link AnnotationMirror}.
   */
  public AnnotationMirror() {
    super();
    this.elementValues = new HashMap<>();
    this.unmodifiableElementValues = Collections.unmodifiableMap(this.elementValues);
  }


  /*
   * Instance methods.
   */


  @Override // AnnotationMirror
  public final DeclaredType getAnnotationType() {
    return this.annotationType;
  }

  /**
   * Sets this {@link AnnotationMirror}'s associated {@link DeclaredType}.
   *
   * @param annotationType the type; must not be {@code null}; must return {@link
   * javax.lang.model.type.TypeKind#DECLARED DECLARED} from its {@link javax.lang.model.type.DeclaredType#getKind()
   * getKind()} method
   *
   * @exception NullPointerException if the affiliated type has already been set and the supplied {@code annotationType}
   * is {@code null}
   *
   * @exception IllegalStateException if the supplied {@link annotationType} is not identical to the one already set
   */
  public final void setAnnotationType(final DeclaredType annotationType) {
    final DeclaredType old = this.getAnnotationType();
    if (old == null) {
      if (annotationType != null) {
        this.annotationType = validateAnnotationType(annotationType);
      }
    } else if (old != annotationType) {
      throw new IllegalStateException("old: " + old + "; annotationType: " + annotationType);
    }
  }

  @Override // AnnotationMirror
  public final Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValues() {
    return this.unmodifiableElementValues;
  }

  /**
   * Installs an {@link AnnotationValue} that corresponds to the supplied {@link ExecutableElement}.
   *
   * @param ee the {@link ExecutableElement}; may (uselessly) be {@code null}
   *
   * @param av the {@link AnnotationValue}; may be {@code null}
   */
  public final void putElementValue(final ExecutableElement ee, final AnnotationValue av) {
    this.elementValues.put(ee, av);
  }

  /**
   * Bulk installs {@link AnnotationValue}s corresponding to {@link ExecutableElement}s.
   *
   * @param evs the new element values; must not be {@code null}
   *
   * @exception NullPointerException if {@code evs} is {@code null}
   */
  public final void setElementValues(final Map<? extends ExecutableElement, ? extends AnnotationValue> evs) {
    this.elementValues.putAll(evs);
  }

  @Override // Object
  public int hashCode() {
    return Equality.hashCode(this, true);
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    } else if (other instanceof AnnotationMirror her) { // instanceof is on purpose
      return Equality.equals(this, her, true);
    } else {
      return false;
    }
  }


  /*
   * Static methods.
   */


  private static final DeclaredType validateAnnotationType(final DeclaredType annotationType) {
    switch (annotationType.getKind()) {
    case DECLARED:
      return annotationType;
    default:
      throw new IllegalArgumentException("annotationType: " + annotationType);
    }
  }

  /**
   * Returns an {@link AnnotationMirror} representing the supplied {@link javax.lang.model.element.AnnotationMirror}.
   *
   * @param am the {@link javax.lang.model.element.AnnotationMirror} to convert or otherwise represent; if it is already
   * an {@link AnnotationMirror} it is simply returned; must not be {@code null}
   *
   * @return an {@link AnnotationMirror}; never {@code null}
   *
   * @exception NullPointerException if {@code am} is {@code null}
   */
  public static final AnnotationMirror of(final javax.lang.model.element.AnnotationMirror am) {
    if (am instanceof AnnotationMirror mam) {
      return mam;
    }
    final AnnotationMirror r = new AnnotationMirror();
    r.setAnnotationType(am.getAnnotationType());
    r.setElementValues(am.getElementValues());
    return r;
  }

}
