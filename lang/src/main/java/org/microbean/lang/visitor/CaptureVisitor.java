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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import javax.lang.model.util.SimpleTypeVisitor14;

import org.microbean.lang.ElementSource;
import org.microbean.lang.Equality;

import org.microbean.lang.type.Capture;
import org.microbean.lang.type.Types;

import static org.microbean.lang.type.Types.allTypeArguments;
import static org.microbean.lang.type.Types.isInterface;

// Basically done
// javac's capture implementation is not visitor-based.
// https://github.com/openjdk/jdk/blob/jdk-20+14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L4388-L4456
public final class CaptureVisitor extends SimpleTypeVisitor14<TypeMirror, Void> {

  private final ElementSource elementSource;

  private final Equality equality;

  private final Types types;

  private final SupertypeVisitor supertypeVisitor;

  private SubtypeVisitor subtypeVisitor;

  private final MemberTypeVisitor memberTypeVisitor;

  private TypeClosureVisitor typeClosureVisitor;

  public CaptureVisitor(final ElementSource elementSource,
                        final Equality equality,
                        final Types types,
                        final SupertypeVisitor supertypeVisitor, // used by internal SubstituteVisitor
                        final MemberTypeVisitor memberTypeVisitor) {
    super();
    this.elementSource = Objects.requireNonNull(elementSource, "elementSource");
    this.equality = equality == null ? new Equality(true) : equality;
    this.types = Objects.requireNonNull(types, "types");
    this.supertypeVisitor = Objects.requireNonNull(supertypeVisitor, "supertypeVisitor");
    this.memberTypeVisitor = Objects.requireNonNull(memberTypeVisitor, "memberTypeVisitor");
  }

  final void setSubtypeVisitor(final SubtypeVisitor v) {
    if (v.captureVisitor() != this) {
      throw new IllegalArgumentException("v");
    } else if (this.subtypeVisitor != v) {
      this.subtypeVisitor = v;
    }
  }

  public final void setTypeClosureVisitor(final TypeClosureVisitor v) {
    if (this.typeClosureVisitor == null || this.typeClosureVisitor != v) {
      this.typeClosureVisitor = Objects.requireNonNull(v, "v");
    }
  }

  @Override
  protected final TypeMirror defaultAction(final TypeMirror t, final Void x) {
    return t;
  }

  @Override
  @SuppressWarnings("unchecked")
  public final TypeMirror visitDeclared(DeclaredType t, final Void x) {
    assert t.getKind() == TypeKind.DECLARED;

    // https://github.com/openjdk/jdk/blob/jdk-20+14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L4392-L4398
    TypeMirror enclosingType = t.getEnclosingType();
    switch (enclosingType.getKind()) {
    case DECLARED:
      final TypeMirror capturedEnclosingType = this.visitDeclared((DeclaredType)enclosingType, null); // RECURSIVE
      if (capturedEnclosingType != enclosingType) {
        final Element element = t.asElement();
        final TypeMirror memberType = this.memberTypeVisitor.visit(capturedEnclosingType, element);
        t = (DeclaredType)new SubstituteVisitor(this.elementSource,
                                                this.equality,
                                                this.supertypeVisitor,
                                                ((DeclaredType)element.asType()).getTypeArguments(),
                                                t.getTypeArguments())
          .visit(memberType, null);
        assert t.getKind() == TypeKind.DECLARED;
        enclosingType = t.getEnclosingType();
      }
      break;
    case NONE:
      break;
    default:
      throw new IllegalArgumentException("t: " + t);
    }

    // https://github.com/openjdk/jdk/blob/jdk-20+14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L4399-L4401
    if (allTypeArguments(t).isEmpty() || this.types.raw(t)) {
      // (Suppose somehow t were an intersection type (which gets modeled as a ClassType as well in javac). t would then
      // be considered to be raw following the rules of javac (not sure about the language specification; the two
      // frequently diverge). So it is accurate for this visitor not to implement visitIntersection().)
      return t;
    }

    // https://github.com/openjdk/jdk/blob/jdk-20+14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L4403-L4406
    final DeclaredType G = (DeclaredType)t.asElement().asType();
    final List<? extends TypeVariable> A = (List<? extends TypeVariable>)G.getTypeArguments();
    final List<? extends TypeMirror> T = t.getTypeArguments(); // t.getKind() == TypeKind.DECLARED ? ((DeclaredType)t).getTypeArguments() : List.of();
    final List<? extends TypeMirror> S = withFreshCapturedTypeVariables(T);

    // https://github.com/openjdk/jdk/blob/jdk-20+14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L4408-L4449
    assert A.size() == T.size();
    assert A.size() == S.size();
    boolean captured = false;
    for (int i = 0; i < A.size(); i++) {
      final TypeVariable currentAHead = A.get(i);
      assert currentAHead.getKind() == TypeKind.TYPEVAR;
      final TypeMirror currentSHead = S.get(i);
      final TypeMirror currentTHead = T.get(i);
      if (currentSHead != currentTHead) {
        captured = true;
        TypeMirror Ui = currentAHead.getUpperBound();
        if (Ui == null) {
          Ui = this.elementSource.element("java.lang.Object").asType();
        }
        final Capture Si = (Capture)currentSHead;
        final WildcardType Ti = (WildcardType)currentTHead;
        Si.setLowerBound(Ti.getSuperBound());
        final TypeMirror TiExtendsBound = Ti.getExtendsBound();
        if (TiExtendsBound == null) {
          Si.setUpperBound(new SubstituteVisitor(this.elementSource, this.equality, this.supertypeVisitor, A, S).visit(Ui));
        } else {
          // TiExtendsBound can be DECLARED, INTERSECTION or TYPEVAR
          Si.setUpperBound(glb(TiExtendsBound,
                               new SubstituteVisitor(this.elementSource, this.equality, this.supertypeVisitor, A, S).visit(Ui)));
        }
      }
    }

    // https://github.com/openjdk/jdk/blob/jdk-20+14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L4451-L4455
    if (captured) {
      assert t.getKind() == TypeKind.DECLARED;
      return syntheticDeclaredType(t, S);
    }
    return t;
  }

  // https://github.com/openjdk/jdk/blob/jdk-20+14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L4102-L4156
  private final TypeMirror glb(final TypeMirror t, final TypeMirror s) {
    // TODO: technically I think t and s can only be either DECLARED, INTERSECTION or TYPEVAR, since that's all closures
    // will accept. See also
    // https://stackoverflow.com/questions/73683649/in-the-javac-source-code-why-does-closuretype-return-a-non-empty-list-for-non.
    if (s == null) {
      return t;
    } else if (t.getKind().isPrimitive()) {
      throw new IllegalArgumentException("t: " + t);
    } else if (s.getKind().isPrimitive()) {
      throw new IllegalArgumentException("s: " + s);
    } else if (this.subtypeVisitor.withCapture(false).visit(t, s)) {
      return t;
    } else if (this.subtypeVisitor.withCapture(false).visit(s, t)) {
      return s;
    }

    final TypeClosure tc = this.typeClosureVisitor.visit(t);
    tc.union(this.typeClosureVisitor.visit(s));

    List<? extends TypeMirror> minimumTypes = tc.toMinimumTypes(this.subtypeVisitor);
    final int size = minimumTypes.size();
    switch (size) {
    case 0:
      return this.elementSource.element("java.lang.Object").asType();
    case 1:
      return minimumTypes.get(0);
    }

    boolean classes = false;
    final Collection<TypeMirror> capturedTypeVariables = new ArrayList<>(size);
    final Collection<TypeMirror> lowers = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      final TypeMirror minimumType = minimumTypes.get(i);
      if (!isInterface(minimumType)) {
        if (!classes) {
          classes = true;
        }
        final TypeMirror lower = capturedTypeVariableLowerBound(minimumType);
        if (minimumType != lower && lower.getKind() != TypeKind.NULL) {
          capturedTypeVariables.add(minimumType);
          lowers.add(lower);
        }
      }
    }
    if (classes) {
      if (lowers.isEmpty()) {
        throw new IllegalArgumentException("t: " + t);
      }
      final List<TypeMirror> bounds = new ArrayList<>(minimumTypes);
      bounds.removeIf(capturedTypeVariables::contains);
      bounds.addAll(lowers);
      return this.glb(bounds); // RECURSIVE, in a way
    }
    return org.microbean.lang.type.IntersectionType.of(minimumTypes);
  }

  // https://github.com/openjdk/jdk/blob/jdk-20+14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L4092-L4100
  private final TypeMirror glb(final List<? extends TypeMirror> ts) {
    if (ts.isEmpty()) {
      return null;
    }
    TypeMirror t1 = ts.get(0);
    for (int i = 1; i < ts.size(); i++) {
      t1 = this.glb(t1, ts.get(i)); // RECURSIVE, in a way
    }
    return t1;
  }

  private static final TypeMirror capturedTypeVariableLowerBound(final TypeMirror capture) {
    if (capture.getKind() == TypeKind.TYPEVAR && capture instanceof Capture c) {
      final TypeMirror lowerBound = c.getLowerBound();
      if (lowerBound == null) {
        return org.microbean.lang.type.NullType.INSTANCE;
      } else {
        return lowerBound.getKind() == TypeKind.NULL ? lowerBound : capturedTypeVariableLowerBound(lowerBound); // RECURSIVE
      }
    }
    return capture;
  }

  private static final List<? extends TypeMirror> withFreshCapturedTypeVariables(final List<? extends TypeMirror> typeArguments) {
    if (typeArguments.isEmpty()) {
      return List.of();
    }
    final List<TypeMirror> list = new ArrayList<>(typeArguments.size());
    for (final TypeMirror typeArgument : typeArguments) {
      switch (typeArgument.getKind()) {
      case WILDCARD:
        list.add(new Capture((WildcardType)typeArgument));
        break;
      default:
        list.add(typeArgument);
        break;
      }
    }
    return Collections.unmodifiableList(list);
  }

  private static final DeclaredType syntheticDeclaredType(final DeclaredType canonicalType,
                                                          final List<? extends TypeMirror> typeArguments) {
    final org.microbean.lang.type.DeclaredType t = new org.microbean.lang.type.DeclaredType();
    t.setEnclosingType(canonicalType.getEnclosingType());
    t.addTypeArguments(typeArguments);
    t.setDefiningElement((TypeElement)canonicalType.asElement());
    return t;
  }

}
