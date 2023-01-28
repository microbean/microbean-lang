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

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import javax.lang.model.util.SimpleTypeVisitor14;

public final class TypeClosureVisitor extends SimpleTypeVisitor14<TypeClosure, Void> {

  // @GuardedBy("itself")
  private final WeakHashMap<TypeMirror, TypeClosure> closureCache;

  private final SupertypeVisitor supertypeVisitor;

  private final PrecedesPredicate precedesPredicate;

  public TypeClosureVisitor(final SupertypeVisitor supertypeVisitor, final PrecedesPredicate precedesPredicate) {
    super();
    this.supertypeVisitor = Objects.requireNonNull(supertypeVisitor, "supertypeVisitor");
    this.precedesPredicate = Objects.requireNonNull(precedesPredicate, "precedesPredicate");
    this.closureCache = new WeakHashMap<>();
  }

  @Override
  protected final TypeClosure defaultAction(final TypeMirror t, final Void x) {
    // Interestingly, javac's Types#closure(Type) method returns a single-element list containing t if t is not a class
    // or interface type.  Nevertheless the intention seems to be that only class or interface types should be supplied,
    // so we enforce that here.
    throw new IllegalArgumentException("t: " + t);
  }

  @Override
  public final TypeClosure visitDeclared(final DeclaredType t, final Void x) {
    assert t.getKind() == TypeKind.DECLARED;
    return this.visitDeclaredOrIntersectionOrTypeVariable(t, x);
  }

  @Override
  public final TypeClosure visitIntersection(final IntersectionType t, final Void x) {
    assert t.getKind() == TypeKind.INTERSECTION;
    return this.visitDeclaredOrIntersectionOrTypeVariable(t, x);
  }

  @Override
  public final TypeClosure visitTypeVariable(final TypeVariable t, final Void x) {
    assert t.getKind() == TypeKind.TYPEVAR;
    return this.visitDeclaredOrIntersectionOrTypeVariable(t, x);
  }

  private final TypeClosure visitDeclaredOrIntersectionOrTypeVariable(final TypeMirror t, final Void x) {
    TypeClosure closure;
    synchronized (this.closureCache) {
      closure = this.closureCache.get(t);
    }
    if (closure == null) {
      switch (t.getKind()) {
      case INTERSECTION:
        // The closure does not include the intersection type itself.  Note that this little nugget effectively removes
        // intersection types from the possible types that will ever be passed to TypeClosure#union(TypeMirror).
        closure = this.visit(this.supertypeVisitor.visit(t));
        break;
      case DECLARED:
      case TYPEVAR:
        final TypeMirror st = this.supertypeVisitor.visit(t);
        switch (st.getKind()) {
        case DECLARED:
          // (Yes, it is OK that INTERSECTION is not present as a case; a TypeVariable cannot have an IntersectionType
          // as a supertype.)
          closure = this.visit(st);
          closure.union(t); // reflexive
          break;
        case TYPEVAR:
          closure = this.visit(st);
          // javac does this
          // (https://github.com/openjdk/jdk/blob/jdk-20+14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3717-L3718):
          //
          //   cl = closure(st).prepend(t);
          //
          // Note that there's no equality or "precedes" check in this one case. Is this a bug, a feature, or just an
          // optimization?  I don't know. I reproduce the behavior here, for better or for worse. This permits two equal
          // type variables in the closure, which otherwise would be filtered out.
          //
          // (The only time a supertype can be a type variable is if the subtype is also a type variable.)
          assert t.getKind() == TypeKind.TYPEVAR : "Expected " + TypeKind.TYPEVAR + "; got DECLARED; t: " + t + "; st: " + st;
          closure.prepend((TypeVariable)t); // reflexive
          break;
        case NONE:
          closure = new TypeClosure(this.precedesPredicate);
          assert t.getKind() == TypeKind.DECLARED; // ...i.e. not TYPEVAR
          closure.union(t); // reflexive
          break;
        default:
          throw new IllegalArgumentException("t: " + t);
        }
        break;
      default:
        throw new IllegalArgumentException("t: " + t);
      }
      for (final TypeMirror iface : this.supertypeVisitor.interfacesVisitor().visit(t)) {
        closure.union(this.visit(iface));
      }
      synchronized (this.closureCache) {
        closureCache.put(t, closure);
      }
    }
    return closure;
  }

}
