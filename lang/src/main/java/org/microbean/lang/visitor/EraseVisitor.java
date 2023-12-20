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

import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import org.microbean.lang.TypeAndElementSource;

import org.microbean.lang.type.Types;

// Basically done.
public final class EraseVisitor extends StructuralTypeMapping<Boolean> {

  private final Types types;

  public EraseVisitor(final TypeAndElementSource elementSource, final Types types) {
    super(elementSource);
    this.types = Objects.requireNonNull(types, "types");
  }

  // https://github.com/openjdk/jdk/blob/jdk-20+12/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L2442-L2459
  @Override // StructuralTypeMapping
  public final DeclaredType visitDeclared(final DeclaredType t, final Boolean recurse) {
    assert t.getKind() == TypeKind.DECLARED;
    // In the compiler, there's a LOT going on here.
    //
    //   Type erased = t.tsym.erasure(Types.this)
    //
    // Every Type in com.sun.tools.javac.code has a Symbol. (This includes nonsensical symbols.) Every Symbol has a
    // Type.
    //
    // This all loosely corresponds with javax.lang.model.type.DeclaredType#asElement() and Element#asType() but there
    // are no nonsensical Elements.
    //
    // A Type's Symbol holds what I'll call the element type, or element Type when I'm being exceptionally precise. As
    // javac goes about its business, it can take a Type (with its Symbol, which references, in many cases, that very
    // Type in a circular fashion), and decorate it with another Type. When this happens, the decorating Type's Symbol's
    // Type will be the decorated "element Type". (Often, but not always, this usage will indicate that the decorating
    // Type is a parameterized type, whose (declared) element type is the Type being parameterized/used.)
    //
    // A Symbol has various methods on it that can cache various bits of ancillary data related to its Type.
    // Symbol#erasure(Types) is one such method.
    //
    // It looks like this:
    //
    //   /** The symbol's erased type.
    //    */
    //   public Type erasure(Types types) {
    //       if (erasure_field == null)
    //           erasure_field = types.erasure(type); // recurse is false
    //       return erasure_field;
    //   }
    //
    // BUT NOTE: ClassSymbol overrides it (see
    // https://github.com/openjdk/jdk/blob/jdk-20+12/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Symbol.java#L1352-L1358):
    //
    //   public Type erasure(Types type) {
    //       if (erasure_field == null)
    //           erasure_field = new ClassType(types.erasure(type.getEnclosingType()),
    //                                         List.nil(), this,
    //                                         type.getMetadata());
    //       return erasure_field;
    //   }
    //
    // So when the field is null, the equivalent of this.visit(t.getEnclosingType(), false) is first called, erasing the
    // enclosing type. Then a new ClassType is created, wrapping the original Symbol, which references the original
    // (unerased) ClassType, which is now the element type, with no type arguments.
    //
    // In this toolkit, we don't want to get into the same "cache stuff in the symbol" business if we can at all help
    // it.
    final org.microbean.lang.type.DeclaredType erasedType;
    if (t instanceof org.microbean.lang.type.DeclaredType dt && dt.isErased()) {
      erasedType = dt;
    } else {
      final org.microbean.lang.type.DeclaredType dt = new org.microbean.lang.type.DeclaredType(true /* erased */);
      dt.setEnclosingType(this.visit(t.getEnclosingType(), false));
      dt.setDefiningElement((TypeElement)t.asElement());
      assert this.types.raw(dt);
      erasedType = dt;
    }
    // Commenting this out because it does not appear to be necessary
    // in javac.
    /*
    if (Boolean.TRUE.equals(recurse)) {
      // This is extremely weird.  In the compiler, if recurse is true, then an ErasedClassType (subtype of ClassType)
      // is created to decorate the already-decorated ClassType being erased.  An ErasedClassType differs from a regular
      // ClassType only in that it returns true from its hasErasedSuperclasses() method, whereas
      // ClassType#hasErasedSuperclasses() returns isRaw().  Maybe this is just a performance optimization?
      //
      // ErasedClassType does not appear anywhere else in all of the JDK, so it seems to exist solely to return true
      // from its hasErasedSuperclasses().  Note that isRaw() will also return true for erased classes.
      erasedType = new DefaultDeclaredType(erasedType.getEnclosingType(), List.of(), true, List.of());
    }
    */
    return erasedType;
  }

  @Override // SimpleTypeVisitor6
  public final TypeMirror visitTypeVariable(final TypeVariable t, final Boolean recurse) {
    assert t.getKind() == TypeKind.TYPEVAR;
    return this.visit(t.getUpperBound(), recurse);
  }

  @Override // StructuralTypeMapping
  public final TypeMirror visitWildcard(final WildcardType t, final Boolean recurse) {
    assert t.getKind() == TypeKind.WILDCARD;
    return this.visit(this.types.extendsBound(t), recurse);
  }

}
