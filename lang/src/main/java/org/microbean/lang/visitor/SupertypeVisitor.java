/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2023 microBean™.
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

import java.util.List;
import java.util.Objects;

import java.util.function.Predicate;

import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import javax.lang.model.util.SimpleTypeVisitor14;

import org.microbean.lang.ElementSource;
import org.microbean.lang.Equality;

import org.microbean.lang.type.NoType;
import org.microbean.lang.type.Types;

// Returns the superclass (or parameterized superclass) of a type, if applicable. See #interfacesVisitor() for returning
// superinterfaces.
//
// Basically done
public final class SupertypeVisitor extends SimpleTypeVisitor14<TypeMirror, Void> {


  /*
   * Instance fields.
   */


  private final ElementSource elementSource;

  private final Equality equality;

  private final Types types;

  private final EraseVisitor eraseVisitor;

  private final Predicate<? super TypeMirror> filter;

  private final InterfacesVisitor interfacesVisitor; // (created by this class)

  private final BoundingClassVisitor boundingClassVisitor; // (inner class)


  /*
   * Constructors.
   */


  public SupertypeVisitor(final ElementSource elementSource,
                          final Types types,
                          final EraseVisitor eraseVisitor) {
    this(elementSource, null, types, eraseVisitor, null);
  }

  public SupertypeVisitor(final ElementSource elementSource,
                          final Types types,
                          final EraseVisitor eraseVisitor,
                          final Predicate<? super TypeMirror> filter) {
    this(elementSource, null, types, eraseVisitor, filter);
  }

  public SupertypeVisitor(final ElementSource elementSource,
                          final Equality equality,
                          final Types types,
                          final EraseVisitor eraseVisitor,
                          final Predicate<? super TypeMirror> filter) {
    super(NoType.NONE); // default return value from the visit*() methods
    this.filter = filter == null ? t -> true : filter;
    this.equality = equality == null ? new Equality(true) : equality;
    this.elementSource = Objects.requireNonNull(elementSource, "elementSource");
    this.types = Objects.requireNonNull(types, "types");
    this.eraseVisitor = Objects.requireNonNull(eraseVisitor, "eraseVisitor");
    this.interfacesVisitor = new InterfacesVisitor(elementSource, this.equality, types, eraseVisitor, this, filter);
    this.boundingClassVisitor = new BoundingClassVisitor(this);
  }


  /*
   * Instance methods.
   */

  public final SupertypeVisitor withFilter(final Predicate<? super TypeMirror> filter) {
    return filter == this.filter ? this :
      new SupertypeVisitor(this.elementSource, this.equality, this.types, this.eraseVisitor, filter);
  }

  public final InterfacesVisitor interfacesVisitor() {
    return this.interfacesVisitor;
  }

  @Override // SimpleTypeVisitor14
  public final TypeMirror visitArray(final ArrayType t, final Void x) {
    assert t.getKind() == TypeKind.ARRAY;
    final TypeMirror componentType = t.getComponentType();
    final TypeMirror returnValue;
    if (componentType.getKind().isPrimitive() || isJavaLangObjectType(componentType)) {
      // e.g. int[] or Object[]
      returnValue = this.topLevelArraySupertype();
    } else {
      final org.microbean.lang.type.ArrayType a = new org.microbean.lang.type.ArrayType(this.visit(componentType));
      a.addAnnotationMirrors(t.getAnnotationMirrors());
      returnValue = a;
    }
    if (this.filter.test(returnValue)) {
      return returnValue;
    }
    return this.visit(returnValue);
  }

  private final IntersectionType topLevelArraySupertype() {
    return
      new org.microbean.lang.type.IntersectionType(List.of(this.elementSource.element("java.lang.Object").asType(),
                                                           this.elementSource.element("java.io.Serializable").asType(),
                                                           this.elementSource.element("java.lang.Cloneable").asType()));
  }

  // The compiler's code is borderline incomprehensible.
  // https://github.com/openjdk/jdk/blob/jdk-20+11/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L2531-L2552
  @Override // SimpleTypeVisitor14
  @SuppressWarnings("unchecked")
  public final TypeMirror visitDeclared(final DeclaredType t, final Void x) {
    assert t.getKind() == TypeKind.DECLARED;
    final TypeMirror returnValue;
    final TypeElement element = (TypeElement)t.asElement();
    final TypeMirror supertype = element.getSuperclass();
    // TODO: if supertype is DefaultNoType.NONE...? as would happen when element is an interface?  The compiler
    // does some wonky caching initialization:
    // https://github.com/openjdk/jdk/blob/jdk-20+11/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L2536-L2537
    // I wonder: does it sneakily set this field to Object.class?  Or does it let it be TypeKind.NONE?
    if (this.types.raw(t)) {
      returnValue = this.eraseVisitor.visit(supertype, true);
    } else {

      // Get the actual declared type.  If, for example, t represents the type denoted by "List<String>", then get the
      // type denoted by "List<E>".
      final DeclaredType typeDeclaration = (DeclaredType)element.asType();

      // The type arguments of such a declared type are always type variables (declared by type parameters).  In the
      // type denoted by "List<E>", the sole type *argument* is the type denoted by (the type parameter element) "E",
      // and the sole type parameter element that declares that type is the element "E" itself.
      @SuppressWarnings("unchecked")
        final List<? extends TypeVariable> formals = (List<? extends TypeVariable>)Types.allTypeArguments(typeDeclaration);
      if (formals.isEmpty()) {
        returnValue = supertype;
      } else {
        returnValue =
          new SubstituteVisitor(this.elementSource,
                                this.equality,
                                this,
                                formals,
                                (List<? extends TypeVariable>)Types.allTypeArguments(this.boundingClassVisitor.visit(t))) // TODO: is this cast really and truly OK?
          .visit(supertype);
      }
    }
    if (this.filter.test(returnValue)) {
      return returnValue;
    }
    return this.visit(returnValue);
  }

  @Override // SimpleTypeVisitor14
  public final TypeMirror visitIntersection(final IntersectionType t, final Void x) {
    assert t.getKind() == TypeKind.INTERSECTION;
    final TypeMirror returnValue = t.getBounds().get(0); // TODO: presumes first bound will be the supertype; see https://github.com/openjdk/jdk/blob/jdk-19+25/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Type.java#L1268
    if (this.filter.test(returnValue)) {
      return returnValue;
    }
    return this.visit(returnValue);
  }

  @Override // SimpleTypeVisitor14
  public final TypeMirror visitTypeVariable(final TypeVariable t, final Void x) {
    assert t.getKind() == TypeKind.TYPEVAR;
    final TypeMirror upperBound = t.getUpperBound();
    final TypeMirror returnValue = switch (upperBound.getKind()) {
    case TYPEVAR -> upperBound;
    case INTERSECTION -> this.visit(upperBound);
    default -> Types.isInterface(upperBound) ? this.visit(upperBound) : upperBound;
    };
    if (this.filter.test(returnValue)) {
      return returnValue;
    }
    return this.visit(returnValue);
  }


  /*
   * Static methods.
   */


  private static final boolean isJavaLangObjectType(final TypeMirror t) {
    return
      t.getKind() == TypeKind.DECLARED &&
      ((DeclaredType)t).asElement() instanceof QualifiedNameable qn &&
      qn.getQualifiedName().contentEquals("java.lang.Object");
  }

}
