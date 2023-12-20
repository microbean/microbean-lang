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
package org.microbean.lang.visitor;

import java.util.Objects;

import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;

import javax.lang.model.util.SimpleTypeVisitor14;

import org.microbean.lang.type.Types;

public final class AssignableVisitor extends SimpleTypeVisitor14<Boolean, TypeMirror> {

  private final Types types;

  private final ConvertibleVisitor convertibleVisitor;

  public AssignableVisitor(final Types types,
                           final ConvertibleVisitor convertibleVisitor) {
    super();
    this.types = Objects.requireNonNull(types, "types");
    this.convertibleVisitor = Objects.requireNonNull(convertibleVisitor, "convertibleVisitor");
  }

  public final AssignableVisitor withConvertibleVisitor(final ConvertibleVisitor convertibleVisitor) {
    if (convertibleVisitor == this.convertibleVisitor) {
      return this;
    }
    return new AssignableVisitor(this.types, convertibleVisitor);
  }

  @Override
  protected final Boolean defaultAction(final TypeMirror t, final TypeMirror s) {
    return this.convertibleVisitor.visit(t, s);
  }

  @Override
  public final Boolean visitPrimitive(final PrimitiveType t, final TypeMirror s) {
    // TODO: check out Types#isAssignable(), particularly the isSubRangeOf(INT) && t.constValue() != null part to see if
    // it is actually possible to implement this
    return super.visitPrimitive(t, s);
  }

}
