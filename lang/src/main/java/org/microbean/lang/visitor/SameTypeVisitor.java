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

import org.microbean.lang.TypeAndElementSource;
import org.microbean.lang.Equality;

import org.microbean.lang.element.DelegatingElement;

import org.microbean.lang.type.Types;

// Basically done
// isSameType() in javac's Types.java
public final class SameTypeVisitor extends SimpleTypeVisitor14<Boolean, TypeMirror> {

  private final TypeAndElementSource elementSource;

  private final Equality equality;

  final ContainsTypeVisitor containsTypeVisitor;

  private final SupertypeVisitor supertypeVisitor;

  private final boolean wildcardsComparable;

  public SameTypeVisitor(final TypeAndElementSource elementSource,
                         final ContainsTypeVisitor containsTypeVisitor,
                         final SupertypeVisitor supertypeVisitor,
                         final boolean wildcardsCompatible) {
    this(elementSource, new Equality(false), containsTypeVisitor, supertypeVisitor, wildcardsCompatible);
  }

  public SameTypeVisitor(final TypeAndElementSource elementSource,
                         final Equality equality,
                         final ContainsTypeVisitor containsTypeVisitor,
                         final SupertypeVisitor supertypeVisitor, // used in visitExecutable, visitIntersection, hasSameBounds (only called from visitExecutable)
                         final boolean wildcardsComparable) {
    super(Boolean.FALSE);
    this.elementSource = Objects.requireNonNull(elementSource, "elementSource");
    this.equality = equality == null ? new Equality(false) : equality;
    this.wildcardsComparable = wildcardsComparable;
    this.containsTypeVisitor = Objects.requireNonNull(containsTypeVisitor, "containsTypeVisitor");
    this.supertypeVisitor = Objects.requireNonNull(supertypeVisitor, "supertypeVisitor");
    containsTypeVisitor.setSameTypeVisitor(this);
  }

  public final SameTypeVisitor withSupertypeVisitor(final SupertypeVisitor supertypeVisitor) {
    if (supertypeVisitor == this.supertypeVisitor) {
      return this;
    }
    return
      new SameTypeVisitor(this.elementSource, this.equality, this.containsTypeVisitor(), supertypeVisitor, this.wildcardsComparable);
      
  }

  final ContainsTypeVisitor containsTypeVisitor() {
    return this.containsTypeVisitor;
  }

  @Override
  protected final Boolean defaultAction(final TypeMirror t, final TypeMirror s) {
    return t == s || this.equality.equals(t, s);
  }

  @Override
  public final Boolean visitArray(final ArrayType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.ARRAY;
    return t == s || switch (s.getKind()) {
    case ARRAY -> this.containsTypeEquivalent(t.getComponentType(), ((ArrayType)s).getComponentType());
    default -> Boolean.FALSE;
    };
  }

  @Override
  public final Boolean visitDeclared(final DeclaredType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.DECLARED;
    return t == s || switch (s.getKind()) {
    case DECLARED -> this.visitDeclared(t, (DeclaredType)s);
    case INTERSECTION -> this.visitDeclared(t, (IntersectionType)s); // basically returns false
    case WILDCARD -> this.visitDeclared(t, (WildcardType)s);
    default -> Boolean.FALSE;
    };
  }

  // https://github.com/openjdk/jdk/blob/jdk-20+16/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1424-L1426
  private final boolean visitDeclared(final DeclaredType t, final DeclaredType s) {
    assert t.getKind() == TypeKind.DECLARED && s.getKind() == TypeKind.DECLARED;
    assert t != s;
    return
      this.equality.equals(t.asElement(), s.asElement()) && // yes, really extreme equality not identity/==
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
      this.visitDeclared(t, this.elementSource.typeElement("java.lang.Object").asType()) &&
      this.visit(t, superBound);
  }

  @Override
  public final Boolean visitError(final ErrorType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.ERROR;
    return Boolean.TRUE;
  }

  @Override
  public final Boolean visitExecutable(final ExecutableType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.EXECUTABLE;
    return t == s || switch (s.getKind()) {
    case EXECUTABLE -> this.visitExecutable(t, (ExecutableType)s);
    default -> Boolean.FALSE;
    };
  }

  private final boolean visitExecutable(final ExecutableType t, final ExecutableType s) {
    assert t.getKind() == TypeKind.EXECUTABLE && s.getKind() == TypeKind.EXECUTABLE;
    assert t != s;

    // In javac, an ExecutableType has its representation spread across a "ForAll" type and a "MethodType", where a
    // ForAll type "has a" MethodType.  The ForAll type is basically a decorator, decorating its MethodType with its
    // type variables.
    //
    // javac's isSameTypeVisitor TypeRelation first checks to see if two ForAlls have the "same" type variables (see
    // hasSameBounds(); javac cannot decide on a term to mean, roughly, equivalent, using "same" and "equivalent" and
    // "equal" oftentimes to mean the same thing).
    //
    // Here we follow suit:
    final List<? extends TypeVariable> ttvs = t.getTypeVariables();
    final List<? extends TypeVariable> stvs = s.getTypeVariables();
    if (!hasSameBounds(ttvs, stvs)) {
      return false;
    }
    final ExecutableType substitutedS =
      new SubstituteVisitor(this.elementSource, this.equality, this.supertypeVisitor, stvs, ttvs).visitExecutable(s, null);
    if (s != substitutedS) {
      return this.visitExecutable(t, substitutedS); // RECURSIVE
    }

    // OK, we completed the "ForAll" part. Next, javac checks the MethodType "portion", which consists of the parameter
    // types and the return type. We've already checked the type variables involved, so we know they're compatible.
    return
      containsTypeEquivalent(t.getParameterTypes(), s.getParameterTypes()) && this.visit(t.getReturnType(), s.getReturnType());
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
      tMap.put(DelegatingElement.of(((DeclaredType)t).asElement(), this.elementSource), ti);
    }
    for (final TypeMirror si : this.supertypeVisitor.interfacesVisitor().visitIntersection(s, null)) {
      assert si.getKind() == TypeKind.DECLARED;
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
    return t == s || switch (tKind) {
    case NONE -> s.getKind() == TypeKind.NONE;
    default -> this.equality.equals(t, s);
    };
  }

  @Override
  public final Boolean visitNull(final NullType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.NULL;
    return t == s || switch (s.getKind()) {
    case NULL -> Boolean.TRUE;
    default -> this.equality.equals(t, s);
    };
  }

  @Override
  public final Boolean visitPrimitive(final PrimitiveType t, final TypeMirror s) {
    assert t.getKind().isPrimitive();
    return t == s || this.equality.equals(t, s);
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
      this.visit(t, this.elementSource.typeElement("java.lang.Object").asType());
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

  // https://github.com/openjdk/jdk/blob/jdk-20+16/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3468-L3480
  private final boolean hasSameBounds(final ExecutableType t, final ExecutableType s) {
    return hasSameBounds(t.getTypeVariables(), s.getTypeVariables());
  }

  private final boolean hasSameBounds(final List<? extends TypeVariable> ts, final List<? extends TypeVariable> ss) {
    final int size = ts.size();
    if (size != ss.size()) {
      return false;
    } else if (size > 0) {
      final SubstituteVisitor sv = new SubstituteVisitor(this.elementSource, this.equality, this.supertypeVisitor, ss, ts);
      for (int i = 0; i < size; i++) {
        final TypeVariable t = ts.get(i);
        final TypeVariable s = ss.get(i);
        if (!this.visit(t.getUpperBound(), sv.visit(s.getUpperBound())) ||
            !this.visit(t.getLowerBound(), sv.visit(s.getLowerBound()))) { // lower bounds only relevant for captures
          return false;
        }
      }
    }
    return true;
  }

  private final boolean containsTypeEquivalent(final List<? extends TypeMirror> ts, final List<? extends TypeMirror> ss) {
    final int size = ts.size();
    if (size != ss.size()) {
      return false;
    }
    for (int i = 0; i < size; i++) {
      if (!this.containsTypeEquivalent(ts.get(i), ss.get(i))) {
        return false;
      }
    }
    return true;
  }

  // https://github.com/openjdk/jdk/blob/jdk-20+16/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L4562-L4565
  private final boolean containsTypeEquivalent(final TypeMirror t, final TypeMirror s) {
    return
      this.visit(t, s) ||
      this.containsTypeVisitor.visit(t, s) && this.containsTypeVisitor.visit(s, t);
  }

}
