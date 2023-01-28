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

import java.util.function.BiPredicate;

import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import org.microbean.lang.Equality;

final class SubstituteVisitor extends StructuralTypeMapping<Void> {


  /*
   * Instance fields.
   */


  private final Equality equality;

  private final SupertypeVisitor supertypeVisitor;

  private final List<TypeMirror> from;

  private final List<TypeMirror> to;


  /*
   * Constructors.
   */


  SubstituteVisitor(final Equality equality,
                    final SupertypeVisitor supertypeVisitor,
                    List<? extends TypeMirror> from,
                    List<? extends TypeMirror> to) {
    super();
    this.equality = equality == null ? new Equality(false) : equality;
    this.supertypeVisitor = Objects.requireNonNull(supertypeVisitor, "supertypeVisitor");
    // https://github.com/openjdk/jdk/blob/jdk-20+12/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3321-L3322
    // "If lists have different length, discard leading elements of the longer list."
    int fromSize = from.size();
    int toSize = to.size();
    if (fromSize != toSize) {
      if (fromSize > toSize) {
        from = from.subList(fromSize - toSize, fromSize);
        fromSize = from.size();
      }
      if (toSize > fromSize) {
        to = to.subList(toSize - fromSize, toSize);
        toSize = to.size();
      }
      assert fromSize == toSize;
    }
    this.from = List.copyOf(from);
    this.to = List.copyOf(to);
  }


  /*
   * Instance methods.
   */


  final SubstituteVisitor with(final List<? extends TypeMirror> from,
                               final List<? extends TypeMirror> to) {
    return new SubstituteVisitor(this.equality, this.supertypeVisitor, from, to);
  }

  // https://github.com/openjdk/jdk/blob/jdk-20+12/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3382-L3411
  @Override
  public final ExecutableType visitExecutable(ExecutableType e, final Void x) {
    assert e.getKind() == TypeKind.EXECUTABLE;

    /*
      if (Type.containsAny(to, t.tvars)) {
          //perform alpha-renaming of free-variables in 't'
          //if 'to' types contain variables that are free in 't'
          List<Type> freevars = newInstances(t.tvars);
          t = new ForAll(freevars,
                         Types.this.subst(t.qtype, t.tvars, freevars));
      }
    */

    List<? extends TypeVariable> typeVariables = e.getTypeVariables();
    if (anyMatches(this.to, typeVariables, this.equality::equals)) {
      e = new org.microbean.lang.type.ExecutableType(e, newInstances(typeVariables));
      typeVariables = e.getTypeVariables();
    }

    // Now do substitution on the type variable bounds themselves.
    final List<? extends TypeVariable> visitedTypeVariables = this.visitUpperBoundsOf(typeVariables);

    // Visit the other "parts" of the method that are structurally relevant.
    ExecutableType visitedE = (ExecutableType)super.visitExecutable(e, x);

    if (typeVariables == visitedTypeVariables) {
      if (e == visitedE) {
        return e;
      }
    } else {
      visitedE = this.with(typeVariables, visitedTypeVariables).visitExecutable(visitedE, x);
    }
    return new org.microbean.lang.type.ExecutableType(visitedE, visitedTypeVariables);
  }

  @Override
  public final IntersectionType visitIntersection(final IntersectionType t, final Void x) {
    assert t.getKind() == TypeKind.INTERSECTION;
    final TypeMirror supertype = this.supertypeVisitor.visit(t, x);
    final TypeMirror visitedSupertype = this.visit(supertype, x);
    final List<? extends TypeMirror> interfaces = this.supertypeVisitor.interfacesVisitor().visit(t, x);
    final List<? extends TypeMirror> visitedInterfaces = this.visit(interfaces, x);
    if (supertype == visitedSupertype && interfaces == visitedInterfaces) {
      return t;
    }
    final List<TypeMirror> list = new ArrayList<>(visitedInterfaces.size() + 1);
    list.add(visitedSupertype);
    list.addAll(visitedInterfaces);
    return new org.microbean.lang.type.IntersectionType(list);
  }

  // https://github.com/openjdk/jdk/blob/jdk-20+12/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3347-L3357
  @Override
  public final TypeMirror visitTypeVariable(final TypeVariable tv, final Void x) {
    assert tv.getKind() == TypeKind.TYPEVAR;
    if (!(tv instanceof org.microbean.lang.type.Capture)) {
      final int size = this.from.size();
      assert size == this.to.size();
      for (int i = 0; i < size; i++) {
        final TypeMirror f = this.from.get(i);
        assert f.getKind() == TypeKind.TYPEVAR;
        assert f instanceof TypeVariable;
        if (this.equality.equals(f, tv)) {
          final TypeMirror t = this.to.get(i);
          assert t.getKind() == TypeKind.TYPEVAR;
          assert t instanceof TypeVariable;
          return t;
        }
      }
    }
    return tv;
  }

  // https://github.com/openjdk/jdk/blob/jdk-20+12/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3373-L3380
  @Override
  public final WildcardType visitWildcard(final WildcardType wt, final Void x) {
    assert wt.getKind() == TypeKind.WILDCARD;
    TypeMirror extendsBound = wt.getExtendsBound();
    TypeMirror superBound = wt.getSuperBound();
    if (superBound == null) {
      if (extendsBound == null) {
        // Unbounded.  No need to do anything else.
        return wt;
      }
      // Upper-bounded.
      final TypeMirror visitedExtendsBound = this.visit(extendsBound); // RECURSIVE
      if (extendsBound == visitedExtendsBound) {
        return wt;
      }
      return org.microbean.lang.type.WildcardType.upperBoundedWildcardType(visitedExtendsBound);
    } else if (extendsBound == null) {
      // Lower-bounded.
      final TypeMirror visitedSuperBound = this.visit(superBound); // RECURSIVE
      if (superBound == visitedSuperBound) {
        return wt;
      }
      return org.microbean.lang.type.WildcardType.lowerBoundedWildcardType(visitedSuperBound);
    } else {
      // Wildcards can only specify a single bound, either upper or lower.
      throw new IllegalArgumentException("wt: " + wt);
    }
  }

  // A port/translation of Types#newInstances(List).  "perform alpha-renaming of free-variables in 't' [free variables
  // in tvs]"
  //
  // This is a slavish port for the most part. There are several cases where we could make things more efficient but the
  // undocumented semantics might be lost.
  //
  // https://github.com/openjdk/jdk/blob/jdk-20+12/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3489-L3507
  private final List<? extends TypeVariable> newInstances(final List<? extends TypeVariable> tvs) {
    assert !tvs.isEmpty(); // shouldn't be called if so

    final List<TypeVariable> visitedTvs = new ArrayList<>(tvs);
    final SubstituteVisitor visitor = this.with(tvs, visitedTvs);
    for (int i = 0; i < visitedTvs.size(); i++) {

      // Get the existing type variable at position i.
      final TypeVariable tv = visitedTvs.get(i);

      final TypeMirror upperBound = tv.getUpperBound();

      // Compute a new upper bound for it.  Note the usage of tvs and visitedTvs in the from and to slots respectively.
      final TypeMirror visitedUpperBound = visitor.visit(upperBound); // RECURSIVE

      // It *seems* that if upperBound and visitedUpperBound are identical we wouldn't have to do anything below.  But
      // the compiler's version of this method always creates a new instance, and the method is named newInstances, so
      // we do too.

      // Create a new TypeVariable whose upper bound is the just-visited upper bound.
      final org.microbean.lang.type.TypeVariable newTv = new org.microbean.lang.type.TypeVariable(visitedUpperBound, tv.getLowerBound());
      newTv.setDefiningElement((TypeParameterElement)tv.asElement());

      // Replace the type variable at position i (the very one we're looking at) with the new, newly-bounded type
      // variable.
      visitedTvs.set(i, newTv);
    }
    return Collections.unmodifiableList(visitedTvs);
  }

  // https://github.com/openjdk/jdk/blob/3cd3a83647297f525f5eab48ce688e024ca6b08c/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3413-L3452
  private final List<? extends TypeVariable> visitUpperBoundsOf(final List<? extends TypeVariable> tvs) {
    if (tvs.isEmpty()) {
      return tvs; // preserve identity whenever possible
    }

    // Phase 1 (it appears): effectively call visit() on the upper bound of all the type variables.  If this didn't
    // result in any changes, we're done.
    List<TypeMirror> visitedUpperBounds = null; // new ArrayList<>(tvs.size());
    boolean changed = false;
    for (final TypeVariable tv : tvs) {
      final TypeMirror upperBound = tv.getUpperBound();
      final TypeMirror visitedUpperBound = this.visit(upperBound);
      visitedUpperBounds.add(visitedUpperBound);
      if (!changed && upperBound != visitedUpperBound) {
        changed = true;
      }
    }
    if (!changed) {
      return tvs;
    }

    // Phase 2: Create a list of type variables temporarily without upper bounds.  We will give them upper bounds in
    // phase 3.
    final List<org.microbean.lang.type.TypeVariable> newTvs = new ArrayList<>(tvs.size());
    for (final TypeVariable tv : tvs) {
      final org.microbean.lang.type.TypeVariable newTv = new org.microbean.lang.type.TypeVariable(null, tv.getLowerBound());
      newTv.setDefiningElement((TypeParameterElement)tv.asElement());
      newTvs.add(newTv);
    }

    // (All Lists at this point are the same size.)

    // Phase 3: Perform substitution over the visited (phase 1) upper bounds of the caller-supplied type variables with
    // the un-upper-bounded type variable "prototypes" created in phase 2.  (This will result in type variables that
    // have no upper bound yet, but we have them in a list, and so can doctor them in phase 4 below.)
    final SubstituteVisitor visitor = this.with(tvs, newTvs);
    for (int i = 0; i < visitedUpperBounds.size(); i++) {
      visitedUpperBounds.set(i, visitor.visit(visitedUpperBounds.get(i)));
    }

    // Phase 4: Replace the unspecified upper bounds (see phase 2) with the substituted bounds we calculated in phase 1
    // and doctored in phase 3.
    for (int i = 0; i < newTvs.size(); i++) {
      final org.microbean.lang.type.TypeVariable upperBoundlessTv = newTvs.get(i);
      final org.microbean.lang.type.TypeVariable newTv = new org.microbean.lang.type.TypeVariable(visitedUpperBounds.get(i), upperBoundlessTv.getLowerBound());
      newTv.setDefiningElement((TypeParameterElement)upperBoundlessTv.asElement());
      newTvs.set(i, newTv);
    }

    return Collections.unmodifiableList(newTvs);
  }


  /*
   * Static methods.
   */


  private static final boolean anyMatches(final Iterable<? extends TypeMirror> ts,
                                          final Collection<? extends TypeMirror> ss,
                                          final BiPredicate<? super TypeMirror, ? super TypeMirror> p) {
    if (!ss.isEmpty()) {
      for (final TypeMirror t : ts) {
        if (matchesAny(t, ss, p)) {
          return true;
        }
      }
    }
    return false;
  }

  // Does at least one s in ss "pass the test" represented by p when p is invoked with t and s?
  private static final boolean matchesAny(final TypeMirror t,
                                          final Iterable<? extends TypeMirror> ss,
                                          final BiPredicate<? super TypeMirror, ? super TypeMirror> p) {
    for (final TypeMirror s : ss) {
      if (p.test(t, s)) {
        return true;
      }
    }
    return false;
  }



}
