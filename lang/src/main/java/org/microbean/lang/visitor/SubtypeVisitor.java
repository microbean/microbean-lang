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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.lang.model.element.Name;
import javax.lang.model.element.QualifiedNameable;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
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

import org.microbean.lang.type.Types;

// Basically done
// See https://github.com/openjdk/jdk/blob/jdk-20+13/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1109-L1238
public final class SubtypeVisitor extends SimpleTypeVisitor14<Boolean, TypeMirror> {

  private final ElementSource elementSource;

  private final Equality equality;

  private final Types types;

  private final boolean capture;

  private final Set<TypeMirrorPair> cache;

  private final SupertypeVisitor supertypeVisitor;

  private final IsSameTypeVisitor isSameTypeVisitor;

  private ContainsTypeVisitor containsTypeVisitor;

  private AsSuperVisitor asSuperVisitor;

  public SubtypeVisitor(final ElementSource elementSource,
                        final Types types,
                        final SupertypeVisitor supertypeVisitor,
                        final IsSameTypeVisitor isSameTypeVisitor) {
    this(elementSource, null, types, supertypeVisitor, isSameTypeVisitor);
  }

  public SubtypeVisitor(final ElementSource elementSource,
                        final Equality equality,
                        final Types types,
                        final SupertypeVisitor supertypeVisitor,
                        final IsSameTypeVisitor isSameTypeVisitor) {
    this(elementSource, equality, types, supertypeVisitor, isSameTypeVisitor, true);
  }

  public SubtypeVisitor(final ElementSource elementSource,
                        final Equality equality,
                        final Types types,
                        final SupertypeVisitor supertypeVisitor,
                        final IsSameTypeVisitor isSameTypeVisitor,
                        final boolean capture) {
    super(Boolean.FALSE);
    this.elementSource = Objects.requireNonNull(elementSource, "elementSource");
    this.equality = equality == null ? new Equality(true) : equality;
    this.types = Objects.requireNonNull(types, "types");
    this.supertypeVisitor = Objects.requireNonNull(supertypeVisitor, "supertypeVisitor");
    this.isSameTypeVisitor = Objects.requireNonNull(isSameTypeVisitor, "isSameTypeVisitor");
    this.capture = capture;
    this.cache = new HashSet<>();
  }

  public final void setAsSuperVisitor(final AsSuperVisitor v) {
    if (this.asSuperVisitor != null) {
      throw new IllegalStateException();
    }
    this.asSuperVisitor = Objects.requireNonNull(v, "v");
  }
  
  public final void setContainsTypeVisitor(final ContainsTypeVisitor v) {
    if (this.containsTypeVisitor != null) {
      throw new IllegalStateException();
    }
    this.containsTypeVisitor = Objects.requireNonNull(v, "v");
  }

  final SubtypeVisitor withCapture(final boolean capture) {
    if (capture == this.capture) {
      return this;
    }
    final SubtypeVisitor subtypeVisitor =
      new SubtypeVisitor(this.elementSource, this.equality, this.types, this.supertypeVisitor, isSameTypeVisitor, capture);
    subtypeVisitor.containsTypeVisitor = this.containsTypeVisitor;
    subtypeVisitor.asSuperVisitor = this.asSuperVisitor;
    return subtypeVisitor;
  }

  // Is t a subtype of s?
  //
  // See
  // https://github.com/openjdk/jdk/blob/jdk-20+11/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1200-L1217
  //
  // See also:
  // https://github.com/openjdk/jdk/blob/jdk-20+11/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1082-L1107
  @Override
  public final Boolean visitArray(final ArrayType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.ARRAY;
    final TypeKind sKind = s.getKind();
    switch (sKind) {
    case ARRAY:
      final TypeMirror tct = t.getComponentType();
      final TypeMirror sct = ((ArrayType)s).getComponentType();
      if (tct.getKind().isPrimitive()) {
        return this.isSameTypeVisitor.visit(tct, sct);
      } else if (this.capture) {
        return
          new SubtypeVisitor(this.elementSource, this.equality, this.types, this.supertypeVisitor, this.isSameTypeVisitor, false)
          .visit(tct, sct);
      } else {
        return this.visit(tct, sct);
      }
    case DECLARED:
      // See
      // https://github.com/openjdk/jdk/blob/jdk-20+11/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1211-L1213
      // for better or for worse
      final Name sName = ((QualifiedNameable)((DeclaredType)s).asElement()).getQualifiedName();
      return
        sName.contentEquals("java.lang.Object") ||
        sName.contentEquals("java.lang.Cloneable") ||
        sName.contentEquals("java.io.Serializable");
    default:
      return false;
    }
  }

  @Override
  public final Boolean visitDeclared(final DeclaredType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.DECLARED;
    return this.visitDeclaredOrIntersection(t, s);
  }

  private final Boolean visitDeclaredOrIntersection(final TypeMirror t, final TypeMirror s) {
    assert t.getKind() == TypeKind.DECLARED || t.getKind() == TypeKind.INTERSECTION;
    // javac implements the subtype relation in a bizarre half-visitor, half-not-visitor setup, where equality and some
    // edge cases are tested first, and then the visitor is applied if the test fails. This means the visitor javac uses
    // is exceptionally weird. See for yourself:
    // https://github.com/openjdk/jdk/blob/jdk-21%2B14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1068-L1105
    //
    // Here, we try to fold this into the visitor itself.
    if (this.equality.equals(t, s, false)) {
      return Boolean.TRUE;
    }
    final TypeMirror sup = this.asSuperVisitor.visit(t, this.types.asElement(s, true /* yes, generate synthetic elements */));
    if (sup == null) {
      return false;
    } else if (sup.getKind() != TypeKind.DECLARED) {
      return this.withCapture(false).visit(sup, s);
    } else if (s.getKind() != TypeKind.DECLARED) {
      // The compiler ultimately does some logic that will ultimately return false if s is not a non-compound ClassType,
      // i.e. if s is anything other than a DeclaredType.  Handle that case early here.
      return false;
    }
    final DeclaredType supDt = (DeclaredType)sup;
    final DeclaredType sDt = (DeclaredType)s;
    return
      supDt.asElement() == sDt.asElement() &&
      (this.types.allTypeArguments(sDt).isEmpty() || this.containsTypeRecursive(sDt, supDt)) &&
      this.withCapture(false).visit(supDt.getEnclosingType(), sDt.getEnclosingType());
  }

  @Override
  public final Boolean visitError(final ErrorType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.ERROR;
    return true;
  }

  @Override
  public final Boolean visitIntersection(final IntersectionType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.INTERSECTION;
    // See
    // https://github.com/openjdk/jdk/blob/jdk-20+13/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1191-L1192
    // and
    // https://github.com/openjdk/jdk/blob/jdk-20+13/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1088-L1094
    return this.visitDeclaredOrIntersection(t, s);
  }

  @Override
  public final Boolean visitNoType(final NoType t, final TypeMirror s) {
    // javac implements the subtype relation in a bizarre half-visitor, half-not-visitor setup, where equality and some
    // edge cases are tested first, and then the visitor is applied if the test fails. This means the visitor javac uses
    // is exceptionally weird. See for yourself:
    // https://github.com/openjdk/jdk/blob/jdk-21%2B14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1068-L1105
    //
    // So javac's isSubtype() will return true for two NoType.NONE instances, but its visitor will return false. But in
    // practice, the visitor's comparison is never executed. We marry the two here.
    return this.equality.equals(t, s, false);
  }

  @Override
  public final Boolean visitNull(final NullType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.NULL;
    final TypeKind sKind = s.getKind();
    switch (sKind) {
    case ARRAY:
    case DECLARED:
    case NULL:
    case TYPEVAR:
      return true;
    default:
      return false;
    }
  }

  // See https://docs.oracle.com/javase/specs/jls/se19/html/jls-4.html#jls-4.10.1
  @Override
  public final Boolean visitPrimitive(final PrimitiveType t, final TypeMirror s) {
    final TypeKind tKind = t.getKind();
    final TypeKind sKind = s.getKind();
    switch (tKind) {
    case BOOLEAN:
    case DOUBLE:
      return tKind == sKind;
    case BYTE:
      switch (sKind) {
      case BYTE:
      case DOUBLE:
      case FLOAT:
      case INT:
      case LONG:
      case SHORT:
        return true;
      default:
        return false;
      }
    case CHAR:
      switch (sKind) {
      case CHAR:
      case DOUBLE:
      case FLOAT:
      case INT:
      case LONG:
        return true;
      default:
        return false;
      }
    case FLOAT:
      switch (sKind) {
      case DOUBLE:
      case FLOAT:
        return true;
      default:
        return false;
      }
    case INT:
      switch (sKind) {
      case DOUBLE:
      case FLOAT:
      case INT:
      case LONG:
        return true;
      }
    case LONG:
      switch (sKind) {
      case DOUBLE:
      case FLOAT:
      case LONG:
        return true;
      default:
        return false;
      }
    case SHORT:
      switch (sKind) {
      case DOUBLE:
      case FLOAT:
      case INT:
      case LONG:
      case SHORT:
        return true;
      default:
        return false;
      }
    default:
      return false;
    }
  }

  @Override
  public final Boolean visitTypeVariable(final TypeVariable t, final TypeMirror s) {
    assert t.getKind() == TypeKind.TYPEVAR;
    // javac implements the subtype relation in a bizarre half-visitor, half-not-visitor setup, where equality and some
    // edge cases are tested first, and then the visitor is applied if the test fails. This means the visitor javac uses
    // is exceptionally weird. See for yourself:
    // https://github.com/openjdk/jdk/blob/jdk-21%2B14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1068-L1105
    //
    // We try to marry javac's visitor-based subtype relation and its non-visitor-based isSubtype method, which is why
    // you see the equality test here.
    return this.equality.equals(t, s, false) || this.withCapture(false).visit(t.getUpperBound(), s);
  }

  @Override
  public final Boolean visitWildcard(final WildcardType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.WILDCARD;
    // See
    // https://github.com/openjdk/jdk/blob/jdk-20+13/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1129
    // and
    // https://bugs.java.com/bugdatabase/view_bug.do?bug_id=7034495
    // and
    // https://bugs.java.com/bugdatabase/view_bug.do?bug_id=7034922
    return false;
  }

  private final boolean containsTypeRecursive(final TypeMirror t, final TypeMirror s) {
    if (t.getKind() == TypeKind.DECLARED && s.getKind() == TypeKind.DECLARED) {
      final List<? extends TypeMirror> tTypeArguments = ((DeclaredType)t).getTypeArguments();
      final DeclaredType dts = (DeclaredType)s;
      List<? extends TypeMirror> sTypeArguments = dts.getTypeArguments();
      if (tTypeArguments.isEmpty() || sTypeArguments.isEmpty()) {
        return false;
      }
      final TypeMirrorPair pair = new TypeMirrorPair(this.types, this.isSameTypeVisitor, t, s);
      if (this.cache.add(pair)) {
        try {
          return this.containsTypeVisitor.visit(tTypeArguments, sTypeArguments);
        } finally {
          this.cache.remove(pair);
        }
      }
      final TypeMirror rewrittenS = this.rewriteSupers(dts);
      switch (rewrittenS.getKind()) {
      case DECLARED:
        sTypeArguments = ((DeclaredType)rewrittenS).getTypeArguments();
        return !sTypeArguments.isEmpty() && this.containsTypeVisitor.visit(tTypeArguments, sTypeArguments);
      default:
        return false;
      }
    }
    return false;
  }

  private final TypeMirror rewriteSupers(final TypeMirror t) {
    // I guess t could be an ArrayType (i.e. generic array type)
    if (!this.types.allTypeArguments(t).isEmpty()) {
      List<TypeVariable> from = new ArrayList<>();
      List<TypeMirror> to = new ArrayList<>();
      new AdaptingVisitor(this.elementSource, this.types, this.isSameTypeVisitor, this, from, to).adaptSelf((DeclaredType)t);
      if (!from.isEmpty()) {
        final List<TypeMirror> rewrite = new ArrayList<>();
        boolean changed = false;
        for (final TypeMirror orig : to) {
          TypeMirror s = this.rewriteSupers(orig); // RECURSIVE
          switch (s.getKind()) {
          case WILDCARD:
            // TODO: I'm not sure this case is actually possible. Ported from the javac nevertheless.
            if (((WildcardType)s).getSuperBound() != null) {
              // TODO: maybe need to somehow ensure this shows up as non-canonical/synthetic
              s = this.types.unboundedWildcardType(s.getAnnotationMirrors());
              changed = true;
            }
            break;
          default:
            if (s != orig) { // Don't need Equality.equals() here
              // TODO: maybe need to somehow ensure this shows up as non-canonical/synthetic
              s = this.types.upperBoundedWildcardType(this.types.extendsBound(s), s.getAnnotationMirrors());
              changed = true;
            }
            break;
          }
          rewrite.add(s);
        }
        if (changed) {
          // (If t is a DeclaredType or a TypeVariable, call asElement().asType() and visit that.)
          return new SubstituteVisitor(this.elementSource, this.equality, this.supertypeVisitor, from, rewrite)
            .visit(switch (t.getKind()) {
              case DECLARED -> ((DeclaredType)t).asElement().asType();
              case TYPEVAR -> ((TypeVariable)t).asElement().asType();
              default -> t;
              });
        }
      }
    }
    return t;
  }

}
