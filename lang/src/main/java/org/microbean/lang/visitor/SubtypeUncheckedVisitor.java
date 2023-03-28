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

import javax.lang.model.element.Element;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import javax.lang.model.util.SimpleTypeVisitor14;

import org.microbean.lang.type.Types;

import static org.microbean.lang.type.Types.asElement;

// Deliberately not thread safe.
public final class SubtypeUncheckedVisitor extends SimpleTypeVisitor14<Boolean, TypeMirror> {

  private final Types types;

  private final SubtypeVisitor subtypeVisitor;

  private final AsSuperVisitor asSuperVisitor;

  private final SameTypeVisitor sameTypeVisitor;

  private final boolean capture;

  private SubtypeUncheckedVisitor withCaptureVariant;

  private SubtypeUncheckedVisitor withoutCaptureVariant;

  public SubtypeUncheckedVisitor(final Types types,
                                 final SubtypeVisitor subtypeVisitor,
                                 final AsSuperVisitor asSuperVisitor,
                                 final SameTypeVisitor sameTypeVisitor,
                                 final boolean capture /* true by default */) {
    super();
    this.types = Objects.requireNonNull(types);
    this.subtypeVisitor = subtypeVisitor.withCapture(capture);
    this.asSuperVisitor = Objects.requireNonNull(asSuperVisitor, "asSuperVisitor");
    this.sameTypeVisitor = Objects.requireNonNull(sameTypeVisitor, "sameTypeVisitor");
    this.capture = capture;
    if (capture) {
      this.withCaptureVariant = this;
    } else {
      this.withoutCaptureVariant = this;
    }
  }

  final SubtypeUncheckedVisitor withCapture(final boolean capture) {
    if (capture) {
      if (this.withCaptureVariant == null) {
        this.withCaptureVariant = new SubtypeUncheckedVisitor(this.types, this.subtypeVisitor, this.asSuperVisitor, this.sameTypeVisitor, true);
      }
      return this.withCaptureVariant;
    } else if (this.withoutCaptureVariant == null) {
      this.withoutCaptureVariant = new SubtypeUncheckedVisitor(this.types, this.subtypeVisitor, this.asSuperVisitor, this.sameTypeVisitor, false);
    }
    return this.withoutCaptureVariant;
  }

  @Override
  protected final Boolean defaultAction(final TypeMirror t, final TypeMirror s) {
    if (this.subtypeVisitor.withCapture(capture).visit(t, s)) {
      return Boolean.TRUE;
    }

    if (this.types.raw(s)) {
      return Boolean.FALSE;
    }

    final Element e = asElement(s, true /* yes, generate synthetic elements */);
    if (e == null) {
      return Boolean.FALSE;
    }

    final TypeMirror t2 = this.asSuperVisitor.visit(t, e);
    return this.types.raw(t2);
  }

  @Override
  public final Boolean visitArray(final ArrayType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.ARRAY;
    switch (s.getKind()) {
    case ARRAY:
      final TypeMirror tct = t.getComponentType();
      if (tct.getKind().isPrimitive()) {
        return this.sameTypeVisitor.visit(tct, ((ArrayType)s).getComponentType());
      } else {
        return this.withCapture(false).visit(tct, ((ArrayType)s).getComponentType());
      }
    default:
      break;
    }

    if (this.subtypeVisitor.withCapture(capture).visit(t, s)) {
      return Boolean.TRUE;
    }

    if (this.types.raw(s)) {
      return Boolean.FALSE;
    }

    final Element e = asElement(s, true /* yes, generate synthetic elements */);
    if (e == null) {
      return Boolean.FALSE;
    }

    final TypeMirror t2 = this.asSuperVisitor.visit(t, e);
    return this.types.raw(t2);
  }

  // See https://github.com/openjdk/jdk/blob/jdk-21%2B15/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1029-L1032
  @Override
  public final Boolean visitTypeVariable(final TypeVariable t, final TypeMirror s) {
    assert t.getKind() == TypeKind.TYPEVAR;
    return
      this.subtypeVisitor.withCapture(capture).visit(t, s) ||
      this.withCapture(false).visit(t.getUpperBound(), s);
  }

}
