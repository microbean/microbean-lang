/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2023–2024 microBean™.
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

import javax.lang.model.type.TypeVisitor;

import org.microbean.lang.TypeAndElementSource;
import org.microbean.lang.Lang;

import org.microbean.lang.type.Types;

/**
 * A hub of sorts for visitors of various kinds designed to reproduce the innards of certain aspects of the {@code
 * javac} compiler at runtime.
 *
 * <p>A spiritually faithful port of the compiler's operations and constructs results in circular dependencies, just as
 * the compiler itself contains such circular dependencies. This class makes it easier to set up the intricate network
 * of visitors that depend on each other.</p>
 *
 * @author <a href="https://about.me/lairdnelson/" target="_top">Laird Nelson</a>
 */
public final class Visitors {


  /*
   * Instance fields.
   */


  private final TypeAndElementSource tes;

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


  /*
   * Constructors.
   */


  public Visitors(final TypeAndElementSource tes) {
    this(tes, false, true);
  }

  public Visitors(TypeAndElementSource tes,
                  final boolean subtypeCapture /* false by default */,
                  final boolean wildcardsCompatible /* true by default */) {
    super();
    if (tes == null) {
      tes = Lang.typeAndElementSource();
    }
    this.tes = tes;
    final Types types = new Types(tes);
    this.eraseVisitor = new EraseVisitor(tes, types);
    this.supertypeVisitor = new SupertypeVisitor(tes, types, this.eraseVisitor);
    this.boundingClassVisitor = new BoundingClassVisitor(this.supertypeVisitor);
    this.asSuperVisitor = new AsSuperVisitor(tes, null, types, this.supertypeVisitor);
    this.memberTypeVisitor =
      new MemberTypeVisitor(tes, null, types, this.asSuperVisitor, this.eraseVisitor, this.supertypeVisitor);

    this.containsTypeVisitor = new ContainsTypeVisitor(tes, types);

    this.sameTypeVisitor = new SameTypeVisitor(tes, this.containsTypeVisitor, this.supertypeVisitor, wildcardsCompatible);

    this.captureVisitor = new CaptureVisitor(tes, null, types, this.supertypeVisitor, this.memberTypeVisitor);

    this.subtypeVisitor =
      new SubtypeVisitor(tes,
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
    this.typeClosureVisitor = new TypeClosureVisitor(tes, this.supertypeVisitor, precedesPredicate);
    this.captureVisitor.setTypeClosureVisitor(this.typeClosureVisitor);

    assert this.initialized();
  }


  /*
   * Instance methods.
   */


  public final TypeAndElementSource typeAndElementSource() {
    return this.tes;
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
