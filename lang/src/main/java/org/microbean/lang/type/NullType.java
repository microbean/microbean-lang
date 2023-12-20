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
package org.microbean.lang.type;

import java.util.List;

import javax.lang.model.element.AnnotationMirror;

import javax.lang.model.type.TypeKind;

public final class NullType extends TypeMirror implements javax.lang.model.type.NullType {

  public static final NullType INSTANCE = new NullType();

  private NullType() {
    super(TypeKind.NULL);
  }

  @Override
  public final void addAnnotationMirror(final AnnotationMirror a) {
    throw new UnsupportedOperationException();
  }

  @Override // AnnotatedConstruct
  public final List<? extends AnnotationMirror> getAnnotationMirrors() {
    return List.of();
  }

}
