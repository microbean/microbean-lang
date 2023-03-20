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

import org.microbean.lang.ElementSource;
import org.microbean.lang.Equality;

import org.microbean.lang.type.DelegatingTypeMirror;

// A type closure list builder.
// NOT THREADSAFE
public final class TypeClosure {

  private final ElementSource elementSource;

  // DelegatingTypeMirror so things like list.contains(t) will work with arbitrary TypeMirror implementations
  private final Deque<DelegatingTypeMirror> deque;

  private final BiPredicate<? super Element, ? super Element> precedesPredicate;

  private final BiPredicate<? super Element, ? super Element> equalsPredicate;

  TypeClosure(final ElementSource elementSource, final SupertypeVisitor supertypeVisitor, final SubtypeVisitor subtypeVisitor) {
    this(elementSource, new PrecedesPredicate(supertypeVisitor, subtypeVisitor), null);
  }

  TypeClosure(final ElementSource elementSource, final BiPredicate<? super Element, ? super Element> precedesPredicate) {
    this(elementSource, precedesPredicate, null);
  }

  TypeClosure(final ElementSource elementSource,
              final BiPredicate<? super Element, ? super Element> precedesPredicate,
              final BiPredicate<? super Element, ? super Element> equalsPredicate) {
    super();
    this.elementSource = Objects.requireNonNull(elementSource, "elementSource");
    this.deque = new ArrayDeque<>(10);
    this.precedesPredicate = Objects.requireNonNull(precedesPredicate, "precedesPredicate");
    this.equalsPredicate = equalsPredicate == null ? Equality::equalsIncludingAnnotations : equalsPredicate;
  }

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
      this.deque.addFirst(DelegatingTypeMirror.of(t, this.elementSource));
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
        this.deque.addFirst(DelegatingTypeMirror.of(t, this.elementSource));
      } else if (this.deque.size() == 1) {
        // No need to recurse and get fancy; just add last
        this.deque.addLast(DelegatingTypeMirror.of(t, this.elementSource));
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
          this.deque.addLast(DelegatingTypeMirror.of(t, this.elementSource));
          break;
        default:
          throw new IllegalArgumentException("t: " + t);
        }
      }
      return;
    }

    final DelegatingTypeMirror head1 = this.deque.peekFirst();
    final Element head1E;
    switch (head1.getKind()) {
    case DECLARED:
      head1E = ((DeclaredType)head1).asElement();
      break;
    case TYPEVAR:
      head1E = ((TypeVariable)head1).asElement();
      break;
    default:
      throw new IllegalStateException();
    }

    final TypeMirror head2 = list.get(0);
    final Element head2E;
    switch (head2.getKind()) {
    case DECLARED:
      head2E = ((DeclaredType)head2).asElement();
      break;
    case TYPEVAR:
      head2E = ((TypeVariable)head2).asElement();
      break;
    default:
      throw new IllegalArgumentException("list: " + list);
    }

    if (this.equalsPredicate.test(head1E, head2E)) {
      // Don't include head2.
      this.deque.removeFirst(); // removes head1
      this.union(list.subList(1, size)); // RECURSIVE
      this.deque.addFirst(head1); // put head1 back
    } else if (this.precedesPredicate.test(head2E, head1E)) {
      this.union(list.subList(1, size)); // RECURSIVE
      this.deque.addFirst(DelegatingTypeMirror.of(head2, this.elementSource));
    } else {
      this.deque.removeFirst(); // removes head1
      this.union(list); // RECURSIVE
      this.deque.addFirst(head1); // put head1 back
    }
  }

  // Weird case. See
  // https://github.com/openjdk/jdk/blob/jdk-20+14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3717-L3718.
  final void prepend(final TypeVariable t) {
    if (t.getKind() != TypeKind.TYPEVAR) {
      throw new IllegalArgumentException("t: " + t);
    }
    this.deque.addFirst(DelegatingTypeMirror.of(t, this.elementSource));
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
      boolean keep = !toSkip.contains(DelegatingTypeMirror.of(current, this.elementSource));
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
              toSkip.add(DelegatingTypeMirror.of(t, this.elementSource));
            }
          }
        }
      }
    }
    classes.addAll(interfaces);
    classes.trimToSize();
    return Collections.unmodifiableList(classes);
  }

  // Every element of the returned list will have a TypeKind that is either DECLARED or TYPEVAR.
  public final List<? extends DelegatingTypeMirror> toList() {
    return List.copyOf(this.deque);
  }

}
