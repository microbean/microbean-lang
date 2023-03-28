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
package org.microbean.lang;

import javax.lang.model.type.TypeVisitor;

import org.microbean.lang.ElementSource;

import org.microbean.lang.type.Types;

import org.microbean.lang.visitor.AsSuperVisitor;
import org.microbean.lang.visitor.CaptureVisitor;
import org.microbean.lang.visitor.ContainsTypeVisitor;
import org.microbean.lang.visitor.EraseVisitor;
import org.microbean.lang.visitor.IsAssignableVisitor;
import org.microbean.lang.visitor.IsConvertibleVisitor;
import org.microbean.lang.visitor.IsSameTypeVisitor;
import org.microbean.lang.visitor.IsSubtypeUncheckedVisitor;
import org.microbean.lang.visitor.MemberTypeVisitor;
import org.microbean.lang.visitor.PrecedesPredicate;
import org.microbean.lang.visitor.SubtypeVisitor;
import org.microbean.lang.visitor.SupertypeVisitor;
import org.microbean.lang.visitor.TypeClosureVisitor;

public final class Visitors {

  private final EraseVisitor eraseVisitor;

  private final SupertypeVisitor supertypeVisitor;

  private final AsSuperVisitor asSuperVisitor;

  private final MemberTypeVisitor memberTypeVisitor;

  private final ContainsTypeVisitor containsTypeVisitor;

  private final IsSameTypeVisitor isSameTypeVisitor;

  private final CaptureVisitor captureVisitor;

  private final SubtypeVisitor subtypeVisitor;

  private final IsSubtypeUncheckedVisitor isSubtypeUncheckedVisitor;

  private final IsConvertibleVisitor isConvertibleVisitor;

  private final TypeClosureVisitor typeClosureVisitor;

  private final IsAssignableVisitor isAssignableVisitor;
  
  public Visitors(final ElementSource es) {
    this(es, false, true);
  }

  public Visitors(final ElementSource es,
                  final boolean subtypeCapture /* false by default */,
                  final boolean wildcardsCompatible /* true by default */) {
    super();
    final Types types = new Types(es);
    this.eraseVisitor = new EraseVisitor(es, types);
    this.supertypeVisitor = new SupertypeVisitor(es, types, this.eraseVisitor);
    this.asSuperVisitor = new AsSuperVisitor(es, null, types, this.supertypeVisitor);
    this.memberTypeVisitor =
      new MemberTypeVisitor(es, null, types, this.asSuperVisitor, this.eraseVisitor, this.supertypeVisitor);

    this.containsTypeVisitor = new ContainsTypeVisitor(es, types);

    this.isSameTypeVisitor = new IsSameTypeVisitor(es, this.containsTypeVisitor, this.supertypeVisitor, wildcardsCompatible);

    this.captureVisitor = new CaptureVisitor(es, null, types, this.supertypeVisitor, this.memberTypeVisitor);

    this.subtypeVisitor =
      new SubtypeVisitor(es,
                         null,
                         types,
                         this.asSuperVisitor,
                         this.supertypeVisitor,
                         this.isSameTypeVisitor,
                         this.containsTypeVisitor,
                         this.captureVisitor,
                         subtypeCapture);

    this.isSubtypeUncheckedVisitor = new IsSubtypeUncheckedVisitor(types, this.subtypeVisitor, this.asSuperVisitor, this.isSameTypeVisitor, subtypeCapture);

    this.isConvertibleVisitor = new IsConvertibleVisitor(types, this.isSubtypeUncheckedVisitor, subtypeVisitor);
    this.isAssignableVisitor = new IsAssignableVisitor(types, this.isConvertibleVisitor);
    
    final PrecedesPredicate precedesPredicate = new PrecedesPredicate(null, this.supertypeVisitor, this.subtypeVisitor);
    this.typeClosureVisitor = new TypeClosureVisitor(es, this.supertypeVisitor, precedesPredicate);
    this.captureVisitor.setTypeClosureVisitor(this.typeClosureVisitor);

    assert this.initialized();
  }

  public final EraseVisitor eraseVisitor() {
    return this.eraseVisitor;
  }

  public final SupertypeVisitor supertypeVisitor() {
    return this.supertypeVisitor;
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

  public final IsSameTypeVisitor isSameTypeVisitor() {
    return this.isSameTypeVisitor;
  }

  public final CaptureVisitor captureVisitor() {
    return this.captureVisitor;
  }

  public final SubtypeVisitor subtypeVisitor() {
    return this.subtypeVisitor;
  }

  public final IsSubtypeUncheckedVisitor isSubtypeUncheckedVisitor() {
    return this.isSubtypeUncheckedVisitor;
  }

  public final IsConvertibleVisitor isConvertibleVisitor() {
    return this.isConvertibleVisitor;
  }

  public final IsAssignableVisitor isAssignableVisitor() {
    return this.isAssignableVisitor;
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
