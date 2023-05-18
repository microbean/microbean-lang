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

import javax.lang.model.element.Element;
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

import static org.microbean.lang.type.Types.allTypeArguments;
import static org.microbean.lang.type.Types.asElement;
import static org.microbean.lang.type.Types.unboundedWildcardType;
import static org.microbean.lang.type.Types.upperBoundedWildcardType;

// Deliberately not thread safe.
// See https://github.com/openjdk/jdk/blob/jdk-20+13/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1109-L1238
public final class SubtypeVisitor extends SimpleTypeVisitor14<Boolean, TypeMirror> {

  private final ElementSource elementSource;

  private final Equality equality;

  private final Types types;

  private final boolean capture;

  private final Set<TypeMirrorPair> cache;

  private final SupertypeVisitor supertypeVisitor;

  private final ContainsTypeVisitor containsTypeVisitor;

  private final SameTypeVisitor sameTypeVisitor;

  private final CaptureVisitor captureVisitor;

  private final AsSuperVisitor asSuperVisitor;

  private SubtypeVisitor withCaptureVariant;

  private SubtypeVisitor withoutCaptureVariant;

  public SubtypeVisitor(final ElementSource elementSource,
                        final Equality equality,
                        final Types types,
                        final AsSuperVisitor asSuperVisitor,
                        final SupertypeVisitor supertypeVisitor, // used only for intersection type cases; passed to substitute visitor
                        final SameTypeVisitor sameTypeVisitor,
                        final ContainsTypeVisitor containsTypeVisitor,
                        final CaptureVisitor captureVisitor,
                        final boolean capture) {
    super(Boolean.FALSE);
    this.cache = new HashSet<>();
    this.elementSource = Objects.requireNonNull(elementSource, "elementSource");
    this.equality = equality == null ? new Equality(true) : equality;
    this.types = Objects.requireNonNull(types, "types");
    this.asSuperVisitor = Objects.requireNonNull(asSuperVisitor, "asSuperVisitor");
    this.supertypeVisitor = Objects.requireNonNull(supertypeVisitor, "supertypeVisitor");
    this.sameTypeVisitor = Objects.requireNonNull(sameTypeVisitor, "sameTypeVisitor");
    this.containsTypeVisitor = Objects.requireNonNull(containsTypeVisitor, "containsTypeVisitor");
    this.captureVisitor = Objects.requireNonNull(captureVisitor, "captureVisitor");

    this.capture = capture;
    if (capture) {
      this.withCaptureVariant = this;
    } else {
      this.withoutCaptureVariant = this;
    }

    asSuperVisitor.setSubtypeVisitor(this);
    containsTypeVisitor.setSubtypeVisitor(this);
    captureVisitor.setSubtypeVisitor(this);
  }

  public final SubtypeVisitor withAsSuperVisitor(final AsSuperVisitor asSuperVisitor) {
    if (asSuperVisitor == this.asSuperVisitor) {
      return this;
    }
    return
      new SubtypeVisitor(this.elementSource,
                         this.equality,
                         this.types,
                         asSuperVisitor,
                         this.supertypeVisitor,
                         this.sameTypeVisitor,
                         this.containsTypeVisitor(),
                         this.captureVisitor(),
                         this.capture);
  }

  final SubtypeVisitor withCapture(final boolean capture) {
    if (capture) {
      if (this.withCaptureVariant == null) {
        this.withCaptureVariant =
          new SubtypeVisitor(this.elementSource,
                         this.equality,
                         this.types,
                         this.asSuperVisitor,
                         this.supertypeVisitor,
                         this.sameTypeVisitor,
                         this.containsTypeVisitor,
                         this.captureVisitor,
                         true);
      }
      return this.withCaptureVariant;
    } else if (this.withoutCaptureVariant == null) {
      this.withoutCaptureVariant =
        new SubtypeVisitor(this.elementSource,
                           this.equality,
                           this.types,
                           this.asSuperVisitor,
                           this.supertypeVisitor,
                           this.sameTypeVisitor,
                           this.containsTypeVisitor,
                           this.captureVisitor,
                           false);
    }
    return this.withoutCaptureVariant;
  }

  final AsSuperVisitor asSuperVisitor() {
    return this.asSuperVisitor;
  }

  final CaptureVisitor captureVisitor() {
    return this.captureVisitor;
  }

  final ContainsTypeVisitor containsTypeVisitor() {
    return this.containsTypeVisitor;
  }

  @Override
  protected final Boolean defaultAction(final TypeMirror t, final TypeMirror s) {
    if (this.equality.equals(t, Objects.requireNonNull(s, "s"))) {
      // TODO: this is always equalsNoMetadata(), which means "equals minus annotations", which, in javac, means ==
      // except in the sole case of arrays, where the "elemtype"s are compared for equality.
      //
      // javac implements the subtype relation in a bizarre half-visitor, half-not-visitor setup, where equality and some
      // edge cases are tested first, and then the visitor is applied if the test fails. This means the visitor javac uses
      // is exceptionally weird. See for yourself:
      // https://github.com/openjdk/jdk/blob/jdk-21%2B14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1068-L1105
      //
      // Here, we try to fold this into the visitor itself.
      return Boolean.TRUE;
    }
    return super.defaultAction(t, s);
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
    if (super.visitArray(t, s)) {
      return Boolean.TRUE;
    }
    final TypeKind sKind = s.getKind();
    switch (sKind) {
    case ARRAY:
      // https://github.com/openjdk/jdk/blob/jdk-21%2B22/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1199C29-L1204
      final TypeMirror tct = t.getComponentType();
      final TypeMirror sct = ((ArrayType)s).getComponentType();
      // If both are primitive arrays, then see if their component types are "the same". Otherwise return the visitation
      // of the component types.
      return tct.getKind().isPrimitive() ? this.sameTypeVisitor.visit(tct, sct) : this.withCapture(false).visit(tct, sct);
    case DECLARED:
      // https://github.com/openjdk/jdk/blob/jdk-21%2B22/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1206-L1211
      final Name sName = ((QualifiedNameable)((DeclaredType)s).asElement()).getQualifiedName();
      return
        sName.contentEquals("java.lang.Object") ||
        sName.contentEquals("java.lang.Cloneable") ||
        sName.contentEquals("java.io.Serializable");
    default:
      return Boolean.FALSE;
    }
  }

  @Override
  public final Boolean visitDeclared(final DeclaredType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.DECLARED;
    if (super.visitDeclared(t, s)) {
      // See
      // https://github.com/openjdk/jdk/blob/jdk-21%2B15/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1080-L1081
      // and defaultAction() above
      return Boolean.TRUE;
    }
    return this.visitDeclaredOrIntersection(t, s);
  }

  private final Boolean visitDeclaredOrIntersection(TypeMirror t, final TypeMirror s) {
    assert t.getKind() == TypeKind.DECLARED || t.getKind() == TypeKind.INTERSECTION;
    assert !this.equality.equals(t, s); // better have already checked it

    // From javac's isSubtype(Type t, Type s, boolean capture); see
    // https://github.com/openjdk/jdk/blob/jdk-21%2B15/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1085-L1091
    if (s.getKind() == TypeKind.INTERSECTION) {
      if (!this.visit(t, this.supertypeVisitor.visit(s))) {
        // Visiting the supertype of an intersection type is exactly visiting its first bound. Bounds of an intersection
        // type, it turns out, must be (partially) ordered from most specialized to least specialized, with classes preceding
        // interfaces. So we visit its first bound, which may be an interface or a non-interface type.
        return Boolean.FALSE;
      }
      for (final TypeMirror i : this.supertypeVisitor.interfacesVisitor().visit(s)) { // TODO: really we should start with its second bound
        if (!this.visit(t, i)) {
          return Boolean.FALSE;
        }
      }
      return Boolean.TRUE;
    }

    if (t.getKind() == TypeKind.DECLARED) {
      // TODO:
      /*
      // Generally, if 's' is a lower-bounded type variable, recur on lower bound; but
      // for inference variables and intersections, we need to keep 's'
      // (see JLS 4.10.2 for intersections and 18.2.3 for inference vars)
      if (!t.hasTag(UNDETVAR) && !t.isCompound()) { // [ljnelson] we handled this above
          // TODO: JDK-8039198, bounds checking sometimes passes in a wildcard as s
          Type lower = cvarLowerBound(wildLowerBound(s));
          if (s != lower && !lower.hasTag(BOT))
              return isSubtype(capture ? capture(t) : t, lower, false);
      }
      */

      // Capture t if necessary. Capturing only does anything on a parameterized type, so therefore only on a type where
      // t.getKind() == TypeKind.DECLARED (notably not INTERSECTION). See
      // https://github.com/openjdk/jdk/blob/jdk-21%2B15/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1103.
      if (this.capture) {
        t = this.captureVisitor.visit(t);
        assert t.getKind() == TypeKind.DECLARED;
      }
    }

    final Element sElement = asElement(s, true /* yes, generate synthetic elements */);
    assert sElement != null : "sElement == null; s: " + s;
    final TypeMirror tsup = this.asSuperVisitor.visit(t, sElement);
    if (tsup == null) {
      return Boolean.FALSE;
    } else if (tsup.getKind() != TypeKind.DECLARED) {
      assert tsup.getKind() != TypeKind.INTERSECTION;
      return this.withCapture(false).visit(tsup, s);
    } else if (s.getKind() != TypeKind.DECLARED) {
      assert s.getKind() != TypeKind.INTERSECTION; // already handled above
      // The compiler ultimately does some logic that will ultimately return false if s is not a non-compound ClassType,
      // i.e. if s is anything other than a DeclaredType.  Handle that case early here.
      return Boolean.FALSE;
    }

    final DeclaredType tsupDt = (DeclaredType)tsup;
    final DeclaredType sDt = (DeclaredType)s;

    if (tsupDt.asElement() == sDt.asElement()) {
      // so far so good
      if (allTypeArguments(sDt).isEmpty()) {
        // so far so good
      } else if (this.containsTypeRecursive(sDt, tsupDt)) {
        // so far so good
      } else {
        // bzzt you lose
        return Boolean.FALSE;
      }
      // so far so good
      return this.withCapture(false).visit(tsupDt.getEnclosingType(), sDt.getEnclosingType());
    } else {
      return Boolean.FALSE;
    }
  }

  @Override
  public final Boolean visitError(final ErrorType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.ERROR;
    return Boolean.TRUE;
  }

  @Override
  public final Boolean visitIntersection(final IntersectionType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.INTERSECTION;
    if (super.visitIntersection(t, s)) {
      return Boolean.TRUE;
    }
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
    return this.equality.equals(t, s);
  }

  @Override
  public final Boolean visitNull(final NullType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.NULL;
    return switch (s.getKind()) {
    case ARRAY, DECLARED, NULL, TYPEVAR -> Boolean.TRUE;
    default -> Boolean.FALSE;
    };
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
      default:
        return false;
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
    return this.equality.equals(t, s) || this.withCapture(false).visit(t.getUpperBound(), s);
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
      final TypeMirrorPair pair = new TypeMirrorPair(this.types, this.sameTypeVisitor, t, s);
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
    if (!allTypeArguments(t).isEmpty()) {
      List<TypeVariable> from = new ArrayList<>();
      List<TypeMirror> to = new ArrayList<>();
      new AdaptingVisitor(this.elementSource, this.types, this.sameTypeVisitor, this, from, to).adaptSelf((DeclaredType)t);
      if (!from.isEmpty()) {
        final List<TypeMirror> rewrite = new ArrayList<>();
        boolean changed = false;
        for (final TypeMirror orig : to) {
          TypeMirror s = this.rewriteSupers(orig); // RECURSIVE
          switch (s.getKind()) {
          case WILDCARD:
            // TODO: I'm not sure this case is actually possible. Ported from the javac nevertheless.
            //
            // TODO: I need to check and make sure I'm not getting burned by "bound", which in javac may mean, for
            // example, "the lower bound of the type parameter element that the wildcard parameterizes"
            if (((WildcardType)s).getSuperBound() != null) {
              // TODO: maybe need to somehow ensure this shows up as non-canonical/synthetic
              s = unboundedWildcardType(s.getAnnotationMirrors());
              changed = true;
            }
            break;
          default:
            if (s != orig) { // Don't need Equality.equals() here
              // TODO: maybe need to somehow ensure this shows up as non-canonical/synthetic
              s = upperBoundedWildcardType(this.types.extendsBound(s), s.getAnnotationMirrors());
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
