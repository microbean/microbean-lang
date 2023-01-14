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
package org.microbean.lang.type;

import java.util.List;

import javax.lang.model.element.AnnotationMirror;

import javax.lang.model.type.TypeKind;

public final class NoType extends TypeMirror implements javax.lang.model.type.NoType {


  /*
   * Static fields.
   */


  public static final NoType NONE = new NoType(javax.lang.model.type.TypeKind.NONE);

  public static final NoType MODULE = new NoType(javax.lang.model.type.TypeKind.MODULE);

  public static final NoType PACKAGE = new NoType(javax.lang.model.type.TypeKind.PACKAGE);

  public static final NoType VOID = new NoType(javax.lang.model.type.TypeKind.VOID);


  /*
   * Constructors.
   */


  private NoType(final javax.lang.model.type.TypeKind kind) {
    super(kind);
  }


  /*
   * Instance methods
   */


  @Override
  public final void addAnnotationMirror(final AnnotationMirror a) {
    throw new UnsupportedOperationException();
  }

  @Override // AnnotatedConstruct
  public final List<? extends AnnotationMirror> getAnnotationMirrors() {
    return List.of();
  }

  protected final TypeKind validateKind(final TypeKind kind) {
    switch (kind) {
    case MODULE:
    case NONE:
    case PACKAGE:
    case VOID:
      return kind;
    default:
      throw new IllegalArgumentException("kind: " + kind);
    }
  }


  /*
   * Static methods.
   */


  public static final NoType of(final TypeKind kind) {
    if (kind == null) {
      return NONE;
    }
    switch (kind) {
    case MODULE:
      return MODULE;
    case NONE:
      return NONE;
    case PACKAGE:
      return PACKAGE;
    case VOID:
      return VOID;
    default:
      throw new IllegalArgumentException("kind: " + kind);
    }
  }

  public static final NoType of(final javax.lang.model.type.TypeMirror t) {
    if (t instanceof NoType noType) {
      return noType;
    }
    return of(t.getKind());
  }

}
