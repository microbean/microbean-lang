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

public final class ConvertibleVisitor extends SimpleTypeVisitor14<Boolean, TypeMirror> {

  private final Types types;
  
  private final SubtypeUncheckedVisitor subtypeUncheckedVisitor;

  private final SubtypeVisitor subtypeVisitor;
  
  public ConvertibleVisitor(final Types types,
                            final SubtypeUncheckedVisitor subtypeUncheckedVisitor,
                            final SubtypeVisitor subtypeVisitor) {
    super();
    this.types = Objects.requireNonNull(types, "types");
    this.subtypeUncheckedVisitor = Objects.requireNonNull(subtypeUncheckedVisitor, "subtypeUncheckedVisitor").withCapture(true);
    this.subtypeVisitor = Objects.requireNonNull(subtypeVisitor, "subtypeVisitor");
  }

  public final ConvertibleVisitor withSubtypeUncheckedVisitor(final SubtypeUncheckedVisitor subtypeUncheckedVisitor) {
    if (subtypeUncheckedVisitor == this.subtypeUncheckedVisitor) {
      return this;
    }
    return new ConvertibleVisitor(this.types, subtypeUncheckedVisitor, this.subtypeVisitor);
  }

  public final ConvertibleVisitor withSubtypeVisitor(final SubtypeVisitor subtypeVisitor) {
    if (subtypeVisitor == this.subtypeVisitor) {
      return this;
    }
    return new ConvertibleVisitor(this.types, this.subtypeUncheckedVisitor, subtypeVisitor);
  }

  @Override
  protected final Boolean defaultAction(final TypeMirror t, final TypeMirror s) {
    assert !t.getKind().isPrimitive();
    return
      s.getKind().isPrimitive() ? this.subtypeVisitor.visit(this.types.unbox(t), s) : this.subtypeUncheckedVisitor.visit(t, s);
  }

  @Override
  public final Boolean visitPrimitive(final PrimitiveType t, final TypeMirror s) {
    assert t.getKind().isPrimitive();
    return
      s.getKind().isPrimitive() ? this.subtypeUncheckedVisitor.visit(t, s) : this.subtypeVisitor.visit(this.types.box(t), s);
  }

}
