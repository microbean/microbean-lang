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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import javax.lang.model.util.SimpleTypeVisitor14;

import org.microbean.lang.ElementSource;
import org.microbean.lang.Equality;

import org.microbean.lang.element.DelegatingElement;

import org.microbean.lang.type.Types;

// Basically done
// isSameType() in javac's Types.java
public final class IsSameTypeVisitor extends SimpleTypeVisitor14<Boolean, TypeMirror> {

  private final ElementSource elementSource;

  private final Equality equality;

  private final ContainsTypeVisitor containsTypeVisitor;

  private final SupertypeVisitor supertypeVisitor;

  private final boolean wildcardsComparable;

  // See comments in visitExecutable().
  // private final HasSameParameterTypesVisitor hasSameParameterTypesVisitor; // inner class

  public IsSameTypeVisitor(final ElementSource elementSource,
                           final ContainsTypeVisitor containsTypeVisitor,
                           final SupertypeVisitor supertypVisitor,
                           final boolean wildcardsCompatible) {
    this(elementSource, null, containsTypeVisitor, supertypVisitor, wildcardsCompatible);
  }

  public IsSameTypeVisitor(final ElementSource elementSource,
                           final Equality equality,
                           final ContainsTypeVisitor containsTypeVisitor,
                           final SupertypeVisitor supertypeVisitor,
                           final boolean wildcardsComparable) {
    super(Boolean.FALSE);
    this.elementSource = Objects.requireNonNull(elementSource, "elementSource");
    this.equality = equality == null ? new Equality(false) : equality;
    this.containsTypeVisitor = Objects.requireNonNull(containsTypeVisitor, "containsTypeVisitor");
    containsTypeVisitor.setIsSameTypeVisitor(this);
    this.supertypeVisitor = Objects.requireNonNull(supertypeVisitor, "supertypeVisitor");
    // See comments in visitExecutable().
    // this.hasSameParameterTypesVisitor = new HasSameParameterTypesVisitor();
    this.wildcardsComparable = wildcardsComparable;
  }

  @Override
  protected final Boolean defaultAction(final TypeMirror t, final TypeMirror s) {
    return t == s || this.equality.equals(t, s);
  }

  @Override
  public final Boolean visitArray(final ArrayType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.ARRAY;
    if (t == s) {
      return true;
    }
    switch (s.getKind()) {
    case ARRAY:
      return this.visitArray(t, (ArrayType)s);
    default:
      return Boolean.FALSE;
    }
  }

  private final boolean visitArray(final ArrayType t, final ArrayType s) {
    assert t.getKind() == TypeKind.ARRAY;
    assert s.getKind() == TypeKind.ARRAY;
    if (t == s) {
      return true;
    }
    final TypeMirror tct = t.getComponentType();
    final TypeMirror sct = s.getComponentType();
    return this.containsTypeEquivalent(tct, sct);
  }

  @Override
  public final Boolean visitDeclared(final DeclaredType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.DECLARED;
    if (t == s) {
      return true;
    }
    switch (s.getKind()) {
    case DECLARED:
      return this.visitDeclared(t, (DeclaredType)s);
    case INTERSECTION:
      // (Basically returns false.)
      return this.visitDeclared(t, (IntersectionType)s);
    case WILDCARD:
      return this.visitDeclared(t, (WildcardType)s);
    default:
      return false;
    }
  }

  // https://github.com/openjdk/jdk/blob/jdk-20+16/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1424-L1426
  private final boolean visitDeclared(final DeclaredType t, final DeclaredType s) {
    assert t.getKind() == TypeKind.DECLARED && s.getKind() == TypeKind.DECLARED;
    assert t != s;
    return
      t.asElement() == s.asElement() && // TODO: *true* identity? Or just extreme equality?
      this.visit(t.getEnclosingType(), s.getEnclosingType()) && // RECURSIVE
      this.containsTypeEquivalent(t.getTypeArguments(), s.getTypeArguments());
  }

  private final boolean visitDeclared(final DeclaredType t, final IntersectionType s) {
    assert t.getKind() == TypeKind.DECLARED && s.getKind() == TypeKind.INTERSECTION;
    // https://github.com/openjdk/jdk/blob/jdk-20+16/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1424-L1426
    //
    // javac falls back on some common code that starts with:
    //
    //   return t.tsym == s.tsym
    //       && ...
    //
    // But the Symbol of an IntersectionClassType will never be the same as the Symbol of any other Type.  That's
    // because
    // https://github.com/openjdk/jdk/blob/jdk-20+16/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L2497-L2504
    // is the only place where a new IntersectionClassType is created
    // (https://github.com/openjdk/jdk/search?q=%22new+IntersectionClassType%22), and you can see that a new synthetic
    // Symbol is created each time as well.
    //
    // Therefore we can just return false.
    return false;
  }

  private final boolean visitDeclared(final DeclaredType t, final WildcardType s) {
    assert t.getKind() == TypeKind.DECLARED && s.getKind() == TypeKind.WILDCARD;
    // https://github.com/openjdk/jdk/blob/jdk-20+16/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1401-L1402
    final TypeMirror superBound = s.getSuperBound();
    return
      superBound != null &&
      s.getExtendsBound() == null &&
      this.visitDeclared(t, this.elementSource.element("java.lang.Object").asType()) &&
      this.visit(t, superBound);
  }

  @Override
  public final Boolean visitError(final ErrorType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.ERROR;
    return true;
  }

  @Override
  public final Boolean visitExecutable(final ExecutableType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.EXECUTABLE;
    if (t == s) {
      return true;
    }
    switch (s.getKind()) {
    case EXECUTABLE:
      return this.visitExecutable(t, (ExecutableType)s);
    default:
      return false;
    }
  }

  private final boolean visitExecutable(final ExecutableType t, final ExecutableType s) {
    assert t.getKind() == TypeKind.EXECUTABLE && s.getKind() == TypeKind.EXECUTABLE;
    assert t != s;
    if (!hasSameBounds(t, s)) {
      return false;
    }
    final ExecutableType substitutedS = new SubstituteVisitor(this.elementSource,
                                                              this.equality,
                                                              this.supertypeVisitor,
                                                              s.getTypeVariables(),
                                                              t.getTypeVariables())
      .visitExecutable(s, null);
    if (s != substitutedS) {
      return this.visitExecutable(t, substitutedS); // RECURSIVE
    }
    // javac has, effectively
    // (https://github.com/openjdk/jdk/blob/jdk-20+16/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1445):
    //
    //   return hasSameArgs(t, s) && this.visit(t.getReturnType(), s.getReturnType())
    //
    // hasSameArgs() in javac ends up calling a visitor that does exactly what this current visitExecutable() method
    // does, so effectively the compiler checks an executable twice. That seems silly. The only "extra" thing
    // hasSameArgs does is call containsTypeEquivalent(t, s).
    //
    // So instead of this:
    //
    //   return hasSameArgs(t, s) && this.visit(t.getReturnType(), s.getReturnType());
    //
    // …we'll just do this:
    return containsTypeEquivalent(t, s) && this.visit(t.getReturnType(), s.getReturnType());
  }

  @Override
  public final Boolean visitIntersection(final IntersectionType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.INTERSECTION;
    if (t == s) {
      return true;
    }
    switch (s.getKind()) {
    case INTERSECTION:
      return this.visitIntersection(t, (IntersectionType)s);
    default:
      // https://github.com/openjdk/jdk/blob/jdk-20+16/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1424-L1426
      //
      // javac falls back on some common code that starts with:
      //
      //   return t.tsym == s.tsym
      //       && ...
      //
      // But the Symbol of an IntersectionClassType will never be the same as the Symbol of any other Type.  That's
      // because
      // https://github.com/openjdk/jdk/blob/jdk-20+16/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L2497-L2504
      // is the only place where a new IntersectionClassType is created
      // (https://github.com/openjdk/jdk/search?q=%22new+IntersectionClassType%22), and you can see that a new synthetic
      // Symbol is created each time as well.
      //
      // Therefore we can just return false.
      return false;
    }
  }

  // https://github.com/openjdk/jdk/blob/jdk-20+16/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1404-L1423
  private final boolean visitIntersection(final IntersectionType t, final IntersectionType s) {
    assert t.getKind() == TypeKind.INTERSECTION && s.getKind() == TypeKind.INTERSECTION;
    assert t != s;
    if (!this.visit(this.supertypeVisitor.visit(t), this.supertypeVisitor.visit(s))) {
      return false;
    }
    final Map<DelegatingElement, TypeMirror> tMap = new HashMap<>();
    for (final TypeMirror ti : this.supertypeVisitor.interfacesVisitor().visitIntersection(t, null)) {
      assert ti.getKind() == TypeKind.DECLARED;
      assert ti instanceof DeclaredType;
      tMap.put(DelegatingElement.of(((DeclaredType)t).asElement(), this.elementSource), ti);
    }
    for (final TypeMirror si : this.supertypeVisitor.interfacesVisitor().visitIntersection(s, null)) {
      assert si.getKind() == TypeKind.DECLARED;
      assert si instanceof DeclaredType;
      final TypeMirror ti = tMap.remove(((DeclaredType)si).asElement());
      if (ti == null || !this.visit(ti, si)) {
        return false;
      }
    }
    return tMap.isEmpty();
  }

  @Override
  public final Boolean visitNoType(final NoType t, final TypeMirror s) {
    final TypeKind tKind = t.getKind();
    assert
      tKind == TypeKind.MODULE ||
      tKind == TypeKind.NONE ||
      tKind == TypeKind.PACKAGE ||
      tKind == TypeKind.VOID;
    if (t == s) {
      return true;
    }
    switch (tKind) {
    case NONE:
      // https://github.com/openjdk/jdk/blob/jdk-20+16/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1361-L1362
      return s.getKind() == TypeKind.NONE;
    case PACKAGE:
      // https://github.com/openjdk/jdk/blob/jdk-20+16/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1448-L1451
      return t == s;
    default:
      return this.equality.equals(t, s);
    }
  }

  @Override
  public final Boolean visitNull(final NullType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.NULL;
    if (t == s) {
      return true;
    }
    switch (s.getKind()) {
    case NULL:
      // https://github.com/openjdk/jdk/blob/jdk-20+16/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1361-L1362
      return true;
    default:
      return this.equality.equals(t, s);
    }
  }

  @Override
  public final Boolean visitPrimitive(final PrimitiveType t, final TypeMirror s) {
    final TypeKind tKind = t.getKind();
    assert tKind.isPrimitive();
    if (t == s) {
      return true;
    }
    final TypeKind sKind = s.getKind();
    switch (s.getKind()) {
    case BOOLEAN:
    case BYTE:
    case CHAR:
    case DOUBLE:
    case FLOAT:
    case INT:
    case LONG:
    case SHORT:
      assert s.getKind().isPrimitive();
      return tKind == sKind;
    default:
      return this.equality.equals(t, s);
    }
  }

  @Override
  public final Boolean visitTypeVariable(final TypeVariable t, final TypeMirror s) {
    assert t.getKind() == TypeKind.TYPEVAR;
    switch (s.getKind()) {
    case TYPEVAR:
      // https://github.com/openjdk/jdk/blob/jdk-20+16/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1363-L1368
      return t == s;
    case WILDCARD:
      return this.visitTypeVariable(t, (WildcardType)s);
    default:
      return false; // really?
    }
  }

  private final boolean visitTypeVariable(final TypeVariable t, final WildcardType s) {
    assert t.getKind() == TypeKind.TYPEVAR && s.getKind() == TypeKind.WILDCARD;
    // https://github.com/openjdk/jdk/blob/jdk-20+16/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1370-L1374
    return
      s.getExtendsBound() == null &&
      s.getSuperBound() != null &&
      this.visit(t, this.elementSource.element("java.lang.Object").asType());
  }

  @Override
  public final Boolean visitWildcard(final WildcardType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.WILDCARD;
    return s.getKind() == TypeKind.WILDCARD && this.visitWildcard(t, (WildcardType)s);
  }

  private final boolean visitWildcard(final WildcardType t, final WildcardType s) {
    assert t.getKind() == TypeKind.WILDCARD && s.getKind() == TypeKind.WILDCARD;
    // See
    // https://github.com/openjdk/jdk/blob/jdk-20+16/src/jdk.compiler/share/classes/com/sun/tools/javac/model/JavacTypes.java#L88-L90,
    // and then
    // https://github.com/openjdk/jdk/blob/jdk-20+16/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1382-L1391.
    if (this.wildcardsComparable) {
      final TypeMirror tExtendsBound = t.getExtendsBound();
      final TypeMirror sExtendsBound = s.getExtendsBound();
      final TypeMirror tSuperBound = t.getSuperBound();
      final TypeMirror sSuperBound = s.getSuperBound();
      // return (t.kind == t2.kind || (t.isExtendsBound() && s.isExtendsBound())) &&
      //         isSameType(t.type, t2.type);
      if (tExtendsBound == null) {
        if (sExtendsBound == null) {
          if (tSuperBound == null) {
            return sSuperBound == null;
          } else if (sSuperBound == null) {
            // t is super-bounded
            // s is super-bounded and extends-bounded
            return false;
          } else {
            // t is super-bounded
            // s is super-bounded
            return this.visit(tSuperBound, sSuperBound);
          }
        } else if (tSuperBound == null) {
          if (sSuperBound == null) {
            // t is super-bounded and extends-bounded
            // s is extends-bounded
            return this.visit(tExtendsBound, sExtendsBound);
          } else {
            throw new IllegalArgumentException("s: " + s);
          }
        } else if (sSuperBound == null) {
          return false;
        } else {
          throw new IllegalArgumentException("s: " + s);
        }
      } else if (tSuperBound == null) {
        if (sExtendsBound == null) {
          return sSuperBound == null && this.visit(tExtendsBound, sExtendsBound);
        } else if (sSuperBound == null) {
          // t is extends-bounded
          // s is extends-bounded
          return this.visit(tExtendsBound, sExtendsBound);
        } else {
          throw new IllegalArgumentException("s: " + s);
        }
      } else {
        throw new IllegalArgumentException("t: " + t);
      }
    }
    return false;
  }

  // NOTE: Not currently used. See comments in visitExecutable() above.
  //
  // https://github.com/openjdk/jdk/blob/jdk-20+16/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3245-L3256
  //
  // Duplication in javac all over the place.
  /*
  private final boolean hasSameArgs(final ExecutableType t, final ExecutableType s) {
    return this.hasSameParameterTypesVisitor.visit(t, s);
  }
  */

  // https://github.com/openjdk/jdk/blob/jdk-20+16/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3468-L3480
  private final boolean hasSameBounds(final ExecutableType t, final ExecutableType s) {
    final List<? extends TypeVariable> tVariables = t.getTypeVariables();
    final List<? extends TypeVariable> sVariables = s.getTypeVariables();
    if (tVariables.size() != sVariables.size() || tVariables.isEmpty()) {
      // This size check code is not in javac (only an emptiness check) but seems harmless and an easy productive
      // optimization.  My guess is the size check was deemed expensive because javac's List is a linked list.
      return false;
    }
    final Iterator<? extends TypeVariable> ti = tVariables.iterator();
    final Iterator<? extends TypeVariable> si = sVariables.iterator();
    while (ti.hasNext() &&
           si.hasNext() &&
           this.visit(ti.next().getUpperBound(),
                      new SubstituteVisitor(this.elementSource,
                                            this.equality,
                                            this.supertypeVisitor,
                                            sVariables,
                                            tVariables).visit(si.next().getUpperBound()))) { // TODO: concurrent iteration exception?
      continue;
    }
    return !ti.hasNext() && !si.hasNext();
  }

  // https://github.com/openjdk/jdk/blob/jdk-20+16/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L4562-L4565
  private final boolean containsTypeEquivalent(final TypeMirror t, final TypeMirror s) {
    return
      this.visit(t, s) ||
      this.containsTypeVisitor.visit(t, s) && this.containsTypeVisitor.visit(s, t);
  }

  private final boolean containsTypeEquivalent(final List<? extends TypeMirror> ts, final List<? extends TypeMirror> ss) {
    if (ts.size() != ss.size() || ts.isEmpty()) {
      // This size check code is not in javac (only an emptiness check) but seems harmless and an easy productive
      // optimization.  My guess is the size check was deemed expensive because javac's List is a linked list.
      return false;
    }
    final Iterator<? extends TypeMirror> ti = ts.iterator();
    final Iterator<? extends TypeMirror> si = ss.iterator();
    while (ti.hasNext() &&
           si.hasNext() &&
           this.containsTypeEquivalent(ti.next(), si.next())) {
      continue;
    }
    return !ti.hasNext() && !si.hasNext();
  }

  // NOTE: Not currently used.
  //
  // See https://github.com/openjdk/jdk/blob/jdk-20+16/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3266-L3298.
  //
  // Note that visitExecutable, above, a port of javac logic, calls
  // this (indirectly), and duplicates most of its logic.  This is
  // very weird.  I don't know why the compiler checks things multiple
  // times.
  /*
  private final class HasSameParameterTypesVisitor extends SimpleTypeVisitor14<Boolean, ExecutableType> {

    HasSameParameterTypesVisitor() {
      super(Boolean.FALSE);
    }

    @Override
    public final Boolean visitExecutable(final ExecutableType t, final ExecutableType s) {
      assert t.getKind() == TypeKind.EXECUTABLE;
      if (s.getKind() == TypeKind.EXECUTABLE) {
        if (hasSameBounds(t, s)) {
          // TODO: already done in visitExecutable() above
          return false;
        }
        // TODO: already done in visitExecutable() above
        final ExecutableType substitutedS =
          new SubstituteVisitor(supertypeVisitor, s.getTypeVariables(), t.getTypeVariables()).visitExecutable(s, null);
        if (s != substitutedS) {
          return this.visit(t, substitutedS);
        }
        return containsTypeEquivalent(t.getParameterTypes(), s.getParameterTypes());
      }
      return false;
    }

  }
  */

}
