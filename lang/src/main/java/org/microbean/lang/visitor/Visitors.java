/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2023 microBean™.
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
package org.microbean.lang.visitor;

import java.util.function.Predicate;

import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;

import org.microbean.lang.TypeAndElementSource;
import org.microbean.lang.Lang;

import org.microbean.lang.type.Types;

public final class Visitors {

  private final TypeAndElementSource elementSource;

  private final EraseVisitor eraseVisitor;

  private final SupertypeVisitor supertypeVisitor;

  private final BoundingClassVisitor boundingClassVisitor;

  private final AsSuperVisitor asSuperVisitor;

  private final MemberTypeVisitor memberTypeVisitor;

  private final ContainsTypeVisitor containsTypeVisitor;

  private final SameTypeVisitor sameTypeVisitor;

  private final CaptureVisitor captureVisitor;

  private final SubtypeVisitor subtypeVisitor;

  private final SubtypeUncheckedVisitor subtypeUncheckedVisitor;

  private final ConvertibleVisitor convertibleVisitor;

  private final PrecedesPredicate precedesPredicate;

  private final TypeClosureVisitor typeClosureVisitor;

  private final AssignableVisitor assignableVisitor;

  public Visitors(final TypeAndElementSource es) {
    this(es, false, true, t -> true);
  }

  public Visitors(final TypeAndElementSource es,
                  final boolean subtypeCapture /* false by default */,
                  final boolean wildcardsCompatible /* true by default */) {
    this(es, subtypeCapture, wildcardsCompatible, t -> true);
  }

  public Visitors(TypeAndElementSource es,
                  final boolean subtypeCapture /* false by default */,
                  final boolean wildcardsCompatible /* true by default */,
                  final Predicate<? super TypeMirror> supertypeFilter) {
    super();
    if (es == null) {
      es = Lang.typeAndElementSource();
    }
    this.elementSource = es;
    final Types types = new Types(es);
    this.eraseVisitor = new EraseVisitor(es, types);
    this.supertypeVisitor = new SupertypeVisitor(es, types, this.eraseVisitor, supertypeFilter);
    this.boundingClassVisitor = new BoundingClassVisitor(this.supertypeVisitor);
    this.asSuperVisitor = new AsSuperVisitor(es, null, types, this.supertypeVisitor);
    this.memberTypeVisitor =
      new MemberTypeVisitor(es, null, types, this.asSuperVisitor, this.eraseVisitor, this.supertypeVisitor);

    this.containsTypeVisitor = new ContainsTypeVisitor(es, types);

    this.sameTypeVisitor = new SameTypeVisitor(es, this.containsTypeVisitor, this.supertypeVisitor, wildcardsCompatible);

    this.captureVisitor = new CaptureVisitor(es, null, types, this.supertypeVisitor, this.memberTypeVisitor);

    this.subtypeVisitor =
      new SubtypeVisitor(es,
                         null,
                         types,
                         this.asSuperVisitor,
                         this.supertypeVisitor,
                         this.sameTypeVisitor,
                         this.containsTypeVisitor,
                         this.captureVisitor,
                         subtypeCapture);

    this.subtypeUncheckedVisitor = new SubtypeUncheckedVisitor(types, this.subtypeVisitor, this.asSuperVisitor, this.sameTypeVisitor, subtypeCapture);

    this.convertibleVisitor = new ConvertibleVisitor(types, this.subtypeUncheckedVisitor, subtypeVisitor);
    this.assignableVisitor = new AssignableVisitor(types, this.convertibleVisitor);

    final PrecedesPredicate precedesPredicate = new PrecedesPredicate(null, this.supertypeVisitor, this.subtypeVisitor);
    this.precedesPredicate = precedesPredicate;
    this.typeClosureVisitor = new TypeClosureVisitor(es, this.supertypeVisitor, precedesPredicate);
    this.captureVisitor.setTypeClosureVisitor(this.typeClosureVisitor);

    assert this.initialized();
  }

  public final TypeAndElementSource typeAndElementSource() {
    return this.elementSource;
  }

  public final EraseVisitor eraseVisitor() {
    return this.eraseVisitor;
  }

  public final SupertypeVisitor supertypeVisitor() {
    return this.supertypeVisitor;
  }

  public final InterfacesVisitor interfacesVisitor() {
    return this.supertypeVisitor.interfacesVisitor();
  }

  public final BoundingClassVisitor boundingClassVisitor() {
    return this.boundingClassVisitor;
  }

  public final AsSuperVisitor asSuperVisitor() {
    return this.asSuperVisitor;
  }

  public final MemberTypeVisitor memberTypeVisitor() {
    return this.memberTypeVisitor;
  }

  public final ContainsTypeVisitor containsTypeVisitor() {
    return this.containsTypeVisitor;
  }

  public final SameTypeVisitor sameTypeVisitor() {
    return this.sameTypeVisitor;
  }

  public final CaptureVisitor captureVisitor() {
    return this.captureVisitor;
  }

  public final SubtypeVisitor subtypeVisitor() {
    return this.subtypeVisitor;
  }

  public final SubtypeUncheckedVisitor subtypeUncheckedVisitor() {
    return this.subtypeUncheckedVisitor;
  }

  public final ConvertibleVisitor convertibleVisitor() {
    return this.convertibleVisitor;
  }

  public final AssignableVisitor assignableVisitor() {
    return this.assignableVisitor;
  }

  public final PrecedesPredicate precedesPredicate() {
    return this.precedesPredicate;
  }

  public final TypeClosureVisitor typeClosureVisitor() {
    return this.typeClosureVisitor;
  }

  private final boolean initialized() {
    for (final java.lang.reflect.Field f : this.getClass().getDeclaredFields()) {
      f.trySetAccessible();
      try {
        final Object visitor = f.get(this);
        for (final java.lang.reflect.Field ff : visitor.getClass().getDeclaredFields()) {
          ff.trySetAccessible();
          if (TypeVisitor.class.isAssignableFrom(ff.getType())) {
            if (ff.get(visitor) == null) {
              throw new AssertionError(ff + " in " + f + " was null");
            }
          }
        }
      } catch (final IllegalAccessException e) {
        throw new AssertionError(e.getMessage(), e);
      }
    }
    return true;
  }

}
