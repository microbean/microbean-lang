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

import java.util.List;
import java.util.Objects;

import javax.lang.model.element.TypeElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import javax.lang.model.util.SimpleTypeVisitor14;

import org.microbean.lang.TypeAndElementSource;
import org.microbean.lang.Equality;

import org.microbean.lang.type.Types;

// Returns the superclass (or parameterized superclass) of a type, if applicable. See #interfacesVisitor() for returning
// superinterfaces.
//
// Basically done
public final class SupertypeVisitor extends SimpleTypeVisitor14<TypeMirror, Void> {


  /*
   * Instance fields.
   */


  private final TypeAndElementSource tes;

  private final Equality equality;

  private final Types types;

  private final EraseVisitor eraseVisitor;

  private final InterfacesVisitor interfacesVisitor; // (created by this class)

  private final BoundingClassVisitor boundingClassVisitor;


  /*
   * Constructors.
   */


  public SupertypeVisitor(final TypeAndElementSource tes,
                          final Types types,
                          final EraseVisitor eraseVisitor) {
    this(tes, null, types, eraseVisitor);
  }

  public SupertypeVisitor(final TypeAndElementSource tes,
                          final Equality equality,
                          final Types types,
                          final EraseVisitor eraseVisitor) {
    super(tes.noType(TypeKind.NONE)); // default return value from the visit*() methods
    this.equality = equality == null ? new Equality(true) : equality;
    this.tes = tes;
    this.types = Objects.requireNonNull(types, "types");
    this.eraseVisitor = Objects.requireNonNull(eraseVisitor, "eraseVisitor");
    this.interfacesVisitor = new InterfacesVisitor(tes, this.equality, types, eraseVisitor, this);
    this.boundingClassVisitor = new BoundingClassVisitor(tes, this);
  }


  /*
   * Instance methods.
   */

  public final InterfacesVisitor interfacesVisitor() {
    return this.interfacesVisitor;
  }

  @Override // SimpleTypeVisitor14
  public final TypeMirror visitArray(final ArrayType t, final Void x) {
    assert t.getKind() == TypeKind.ARRAY;
    final TypeMirror componentType = t.getComponentType();
    if (componentType.getKind().isPrimitive() || isJavaLangObjectType(componentType)) {
      // e.g. int[] or Object[]
      return this.topLevelArraySupertype();
    }
    final org.microbean.lang.type.ArrayType a = new org.microbean.lang.type.ArrayType(this.visit(componentType));
    a.addAnnotationMirrors(t.getAnnotationMirrors());
    return a;
  }

  private final IntersectionType topLevelArraySupertype() {
    return
      new org.microbean.lang.type.IntersectionType(List.of(this.tes.typeElement("java.lang.Object").asType(),
                                                           this.tes.typeElement("java.io.Serializable").asType(),
                                                           this.tes.typeElement("java.lang.Cloneable").asType()));
  }

  // The compiler's code is borderline incomprehensible.
  // https://github.com/openjdk/jdk/blob/jdk-20+11/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L2531-L2552
  @Override // SimpleTypeVisitor14
  @SuppressWarnings("unchecked")
  public final TypeMirror visitDeclared(final DeclaredType t, final Void x) {
    assert t.getKind() == TypeKind.DECLARED;
    final TypeElement element = (TypeElement)t.asElement();
    final TypeMirror supertype = element.getSuperclass();
    // TODO: if supertype is DefaultNoType.NONE...? as would happen when element is an interface? The compiler does some
    // wonky caching initialization:
    // https://github.com/openjdk/jdk/blob/jdk-20+11/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L2536-L2537
    // I wonder: does it sneakily set this field to Object.class?  Or does it let it be TypeKind.NONE?
    if (this.types.raw(t)) {
      return this.eraseVisitor.visit(supertype, true);
    }

    // Get the actual declared type. If, for example, t represents the type denoted by "List<String>", then get the type
    // denoted by "List<E>".
    final DeclaredType typeDeclaration = (DeclaredType)element.asType();

    // The type arguments of such a declared type are always type variables (declared by type parameters). In the type
    // denoted by "List<E>", the sole type *argument* is the type denoted by (the type parameter element) "E", and the
    // sole type parameter element that declares that type is the element "E" itself.
    @SuppressWarnings("unchecked")
    final List<? extends TypeVariable> formals = (List<? extends TypeVariable>)Types.allTypeArguments(typeDeclaration);
    if (formals.isEmpty()) {
      return supertype;
    }

    return
      new SubstituteVisitor(this.tes,
                            this.equality,
                            this,
                            formals,
                            (List<? extends TypeVariable>)Types.allTypeArguments(this.boundingClassVisitor.visit(t))) // TODO: is this cast really and truly OK?
      .visit(supertype);
  }

  @Override // SimpleTypeVisitor14
  public final TypeMirror visitIntersection(final IntersectionType t, final Void x) {
    assert t.getKind() == TypeKind.INTERSECTION;
    // Since an intersection type is always ordered from most specialized to least, it follows that the direct supertype
    // of an intersection type is its first bound. See
    // https://docs.oracle.com/javase/specs/jls/se22/html/jls-4.html#jls-4.10.2
    //
    // TODO: wait, no, this isn't correct at all. The JLS says: "The direct supertypes of an intersection type T1 &
    // ... & Tn are Ti (1 ≤ i ≤ n)." But javac's Types class sets the supertype_field (singular) field to the first
    // element of the list. See
    // https://github.com/openjdk/jdk/blob/jdk-19+25/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Type.java#L1268.
    return t.getBounds().get(0);
  }

  @Override // SimpleTypeVisitor14
  public final TypeMirror visitTypeVariable(final TypeVariable t, final Void x) {
    assert t.getKind() == TypeKind.TYPEVAR;
    final TypeMirror upperBound = t.getUpperBound();
    return switch (upperBound.getKind()) {
    case TYPEVAR -> upperBound;
    case INTERSECTION -> this.visit(upperBound);
    default -> Types.isInterface(upperBound) ? this.visit(upperBound) : upperBound;
    };
  }


  /*
   * Static methods.
   */


  private static final boolean isJavaLangObjectType(final TypeMirror t) {
    return
      t.getKind() == TypeKind.DECLARED &&
      ((TypeElement)((DeclaredType)t).asElement()).getQualifiedName().contentEquals("java.lang.Object");
  }

}
