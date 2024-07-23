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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import java.util.function.BiPredicate;

import javax.lang.model.element.Element;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import org.microbean.lang.TypeAndElementSource;
import org.microbean.lang.Equality;

import org.microbean.lang.type.DelegatingTypeMirror;

/**
 * A supplier of a {@link List} of {@link DelegatingTypeMirror}s, built by a {@link TypeClosureVisitor}.
 *
 * @author <a href="https://about.me/lairdnelson/" target="_top">Laird Nelson</a>
 *
 * @see #toList()
 *
 * @see TypeClosureVisitor
 */
// A type closure list builder.
// NOT THREADSAFE
public final class TypeClosure {


  /*
   * Instance fields.
   */


  private final TypeAndElementSource tes;

  // DelegatingTypeMirror so things like list.contains(t) will work with arbitrary TypeMirror implementations
  private final Deque<DelegatingTypeMirror> deque;

  private final BiPredicate<? super Element, ? super Element> precedesPredicate;

  private final BiPredicate<? super Element, ? super Element> equalsPredicate;


  /*
   * Constructors.
   */


  TypeClosure(final TypeAndElementSource tes, final SupertypeVisitor supertypeVisitor, final SubtypeVisitor subtypeVisitor) {
    this(tes, new PrecedesPredicate(supertypeVisitor, subtypeVisitor), null);
  }

  TypeClosure(final TypeAndElementSource tes, final BiPredicate<? super Element, ? super Element> precedesPredicate) {
    this(tes, precedesPredicate, null);
  }

  TypeClosure(final TypeAndElementSource tes,
              final BiPredicate<? super Element, ? super Element> precedesPredicate,
              final BiPredicate<? super Element, ? super Element> equalsPredicate) {
    super();
    this.tes = Objects.requireNonNull(tes, "tes");
    this.deque = new ArrayDeque<>(10);
    this.precedesPredicate = Objects.requireNonNull(precedesPredicate, "precedesPredicate");
    this.equalsPredicate = equalsPredicate == null ? Equality::equalsIncludingAnnotations : equalsPredicate;
  }


  /*
   * Instance methods.
   */


  final void union(final TypeMirror t) {
    // t must be DECLARED or TYPEVAR.  TypeClosureVisitor deals with INTERSECTION before it gets here.

    final Element e;
    switch (t.getKind()) {
    case DECLARED:
      e = ((DeclaredType)t).asElement();
      break;
    case TYPEVAR:
      e = ((TypeVariable)t).asElement();
      break;
    default:
      throw new IllegalArgumentException("t: " + t); // TODO: or just return
    }

    final DelegatingTypeMirror head = this.deque.peekFirst();
    if (head == null) {
      this.deque.addFirst(DelegatingTypeMirror.of(t, this.tes));
      return;
    }

    final Element headE;
    switch (head.getKind()) {
    case DECLARED:
      headE = ((DeclaredType)head).asElement();
      break;
    case TYPEVAR:
      headE = ((TypeVariable)head).asElement();
      break;
    default:
      throw new IllegalStateException();
    }

    if (!this.equalsPredicate.test(e, headE)) {
      if (this.precedesPredicate.test(e, headE)) {
        this.deque.addFirst(DelegatingTypeMirror.of(t, this.tes));
      } else if (this.deque.size() == 1) {
        // No need to recurse and get fancy; just add last
        this.deque.addLast(DelegatingTypeMirror.of(t, this.tes));
      } else {
        this.deque.removeFirst(); // returns head
        this.union(t); // RECURSIVE
        this.deque.addFirst(head);
      }
    }

  }

  final void union(final TypeClosure tc) {
    this.union(tc.toList());
  }

  // A spiritual port of
  // https://github.com/openjdk/jdk/blob/jdk-21%2B35/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3783-L3802. Adds
  // elements of list in appropriate locations in this TypeClosure.
  private final void union(final List<? extends TypeMirror> list) {
    final int size = list.size();
    switch (size) {
    case 0:
      return;
    case 1:
      this.union(list.get(0));
      return;
    default:
      break;
    }

    if (this.deque.isEmpty()) {
      for (final TypeMirror t : list) {
        switch (t.getKind()) {
        case DECLARED:
        case TYPEVAR:
          this.deque.addLast(DelegatingTypeMirror.of(t, this.tes));
          break;
        default:
          throw new IllegalArgumentException("t: " + t);
        }
      }
      return;
    }

    final DelegatingTypeMirror head0 = this.deque.peekFirst();
    final Element head0E;
    switch (head0.getKind()) {
    case DECLARED:
      head0E = ((DeclaredType)head0).asElement();
      break;
    case TYPEVAR:
      head0E = ((TypeVariable)head0).asElement();
      break;
    default:
      throw new IllegalStateException();
    }

    final TypeMirror head1 = list.get(0);
    final Element head1E;
    switch (head1.getKind()) {
    case DECLARED:
      head1E = ((DeclaredType)head1).asElement();
      break;
    case TYPEVAR:
      head1E = ((TypeVariable)head1).asElement();
      break;
    default:
      throw new IllegalArgumentException("list: " + list);
    }

    if (this.equalsPredicate.test(head0E, head1E)) {
      // Don't include head1.
      this.deque.removeFirst(); // removes head0
      this.union(list.subList(1, size)); // RECURSIVE
      this.deque.addFirst(head0); // put head0 back
    } else if (this.precedesPredicate.test(head1E, head0E)) {
      this.union(list.subList(1, size)); // RECURSIVE
      this.deque.addFirst(DelegatingTypeMirror.of(head1, this.tes));
    } else {
      this.deque.removeFirst(); // removes head0
      this.union(list); // RECURSIVE
      this.deque.addFirst(head0); // put head0 back
    }
  }

  // Weird case. See
  // https://github.com/openjdk/jdk/blob/jdk-20+14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3717-L3718.
  final void prepend(final TypeVariable t) {
    if (t.getKind() != TypeKind.TYPEVAR) {
      throw new IllegalArgumentException("t: " + t);
    }
    this.deque.addFirst(DelegatingTypeMirror.of(t, this.tes));
  }

  // Port of javac's Types#closureMin(List<Type>)
  // https://github.com/openjdk/jdk/blob/jdk-20+14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3915-L3945
  final List<? extends TypeMirror> toMinimumTypes(final SubtypeVisitor subtypeVisitor) {
    final ArrayList<TypeMirror> classes = new ArrayList<>();
    final List<TypeMirror> interfaces = new ArrayList<>();
    final Set<DelegatingTypeMirror> toSkip = new HashSet<>();

    final TypeMirror[] array = this.deque.toArray(new TypeMirror[0]);
    final int size = array.length;

    for (int i = 0, next = 1; i < size; i++, next++) {
      final TypeMirror current = array[i];
      boolean keep = !toSkip.contains(DelegatingTypeMirror.of(current, this.tes));
      if (keep && current.getKind() == TypeKind.TYPEVAR && next < size) {
        for (int j = next; j < size; j++) {
          if (subtypeVisitor.withCapture(false).visit(array[j], current)) {
            // If there's a subtype of current "later" in the list, then there's no need to "keep" current; it will be
            // implied by the subtype's supertype hierarchy.
            keep = false;
            break;
          }
        }
      }
      if (keep) {
        if (current.getKind() == TypeKind.DECLARED && ((DeclaredType)current).asElement().getKind().isInterface()) {
          interfaces.add(current);
        } else {
          classes.add(current);
        }
        if (next < size) {
          for (int j = next; j < size; j++) {
            final TypeMirror t = array[j];
            if (subtypeVisitor.withCapture(false).visit(current, t)) {
              // As we're processing this.list, we can skip supertypes of current (t, here) because we know current is
              // already more specialized than they are.
              toSkip.add(DelegatingTypeMirror.of(t, this.tes));
            }
          }
        }
      }
    }
    classes.addAll(interfaces);
    classes.trimToSize();
    return Collections.unmodifiableList(classes);
  }

  /**
   * Returns an immutable {@link List} of {@link DelegatingTypeMirror}s that this {@link TypeClosure} contains.
   *
   * <p>The {@link DelegatingTypeMirror}s within the returned {@link List} will {@linkplain TypeMirror#getKind() report}
   * {@link TypeKind} instances that are either {@link TypeKind#DECLARED} or {@link TypeKind#TYPEVAR} and none
   * other.</p>
   *
   * @return an immutable {@link List} of {@link DelegatingTypeMirror}s that this {@link TypeClosure} contains; never
   * {@code null}
   *
   * @see TypeClosureVisitor
   */
  // Every element of the returned list will have a TypeKind that is either DECLARED or TYPEVAR.
  public final List<? extends DelegatingTypeMirror> toList() {
    return List.copyOf(this.deque);
  }

}
