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

import java.util.List;
import java.util.Objects;

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

import org.microbean.lang.Equality;

import org.microbean.lang.type.NoType;
import org.microbean.lang.type.Types;

// Basically done
public final class SupertypeVisitor extends SimpleTypeVisitor14<TypeMirror, Void> {


  /*
   * Static fields.
   */


  private static final TypeMirror TOP_LEVEL_ARRAY_SUPERTYPE =
    new org.microbean.lang.type.IntersectionType(List.of(Types.JAVA_LANG_OBJECT_TYPE,
                                                         Types.JAVA_IO_SERIALIZABLE_TYPE,
                                                         Types.JAVA_LANG_CLONEABLE_TYPE));


  /*
   * Instance fields.
   */


  private final Equality equality;

  private final Types types;

  private final EraseVisitor eraseVisitor;

  private final InterfacesVisitor interfacesVisitor; // (created by this class)

  private final BoundingClassVisitor boundingClassVisitor; // (inner class)


  /*
   * Constructors.
   */


  public SupertypeVisitor(final Types types,
                          final EraseVisitor eraseVisitor) {
    this(null, types, eraseVisitor);
  }
  
  public SupertypeVisitor(final Equality equality,
                          final Types types,
                          final EraseVisitor eraseVisitor) {
    super();
    this.equality = equality == null ? new Equality(true) : equality;
    this.types = Objects.requireNonNull(types, "types");
    this.eraseVisitor = Objects.requireNonNull(eraseVisitor, "eraseVisitor");
    this.boundingClassVisitor = new BoundingClassVisitor(); // (inner class)
    this.interfacesVisitor = new InterfacesVisitor(this.equality, types, eraseVisitor, this);
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
    if (componentType.getKind().isPrimitive() || isObjectType(componentType)) {
      // e.g. int[] or Object[]
      return TOP_LEVEL_ARRAY_SUPERTYPE;
    }
    final org.microbean.lang.type.ArrayType a = new org.microbean.lang.type.ArrayType(this.visit(componentType));
    a.addAnnotationMirrors(t.getAnnotationMirrors());
    return a;
  }

  // The compiler's code is borderline incomprehensible.
  // https://github.com/openjdk/jdk/blob/jdk-20+11/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L2531-L2552
  @Override // SimpleTypeVisitor14
  @SuppressWarnings("unchecked")
  public final TypeMirror visitDeclared(final DeclaredType t, final Void x) {
    assert t.getKind() == TypeKind.DECLARED;
    final TypeMirror supertype = ((TypeElement)t.asElement()).getSuperclass();
    // TODO: if supertype is DefaultNoType.NONE...? as would happen when t.asElement() is an interface?  The compiler
    // does some wonky caching initialization:
    // https://github.com/openjdk/jdk/blob/jdk-20+11/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L2536-L2537
    // I wonder: does it sneakily set this field to Object.class?  Or does it let it be TypeKind.NONE?
    if (this.types.raw(t)) {
      return this.eraseVisitor.visit(supertype, true);
    }

    // Get the actual declared type.  If, for example, t represents the type "behind" the element "List<String>", then
    // Types.declaredTypeMirror(t) returns the declared type "behind" the element "List<E>".
    final DeclaredType declaredType = Types.declaredTypeMirror(t);

    // The type arguments of such a declared type are always type variables (declared by type parameters).  In the type
    // "behind" the element "List<E>", the sole type *argument* is the type "behind" the element "E", and the sole type
    // parameter that declares that type is the element "E" itself.
    @SuppressWarnings("unchecked")
    final List<? extends TypeVariable> formals = (List<? extends TypeVariable>)Types.allTypeArguments(declaredType);
    if (formals.isEmpty()) {
      return supertype;
    }
    return
      new SubstituteVisitor(this.equality,
                            this,
                            formals,
                            (List<? extends TypeVariable>)Types.allTypeArguments(this.boundingClassVisitor.visitDeclared(t, x)))
      .visit(supertype);
  }

  @Override // SimpleTypeVisitor14
  public final TypeMirror visitError(final ErrorType t, final Void x) {
    assert t.getKind() == TypeKind.ERROR;
    return NoType.NONE;
  }

  @Override // SimpleTypeVisitor14
  public final TypeMirror visitIntersection(final IntersectionType t, final Void x) {
    assert t.getKind() == TypeKind.INTERSECTION;
    return t.getBounds().get(0); // TODO: presumes first bound will be the supertype; see https://github.com/openjdk/jdk/blob/jdk-19+25/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Type.java#L1268
  }

  @Override // SimpleTypeVisitor14
  public final TypeMirror visitTypeVariable(final TypeVariable t, final Void x) {
    assert t.getKind() == TypeKind.TYPEVAR;
    final TypeMirror upperBound = t.getUpperBound();
    switch (upperBound.getKind()) {
    case TYPEVAR:
      return upperBound;
    case INTERSECTION:
      return this.visit(upperBound);
    default:
      return Types.isInterface(upperBound) ? this.visit(upperBound) : upperBound;
    }
  }


  /*
   * Static methods.
   */


  private static final boolean isObjectType(final TypeMirror t) {
    return
      t.getKind() == TypeKind.DECLARED &&
      ((DeclaredType)t).asElement() instanceof QualifiedNameable qn &&
      qn.getQualifiedName().contentEquals("java.lang.Object");
  }


  /*
   * Inner and nested classes.
   */


  private final class BoundingClassVisitor extends SimpleTypeVisitor14<TypeMirror, Void> {

    private BoundingClassVisitor() {
      super();
    }

    @Override // SimpleTypeVisitor14
    protected final TypeMirror defaultAction(final TypeMirror t, final Void x) {
      return t;
    }

    @Override
    public final DeclaredType visitDeclared(final DeclaredType t, final Void x) {
      assert t.getKind() == TypeKind.DECLARED;
      final TypeMirror enclosingType = t.getEnclosingType();
      final TypeMirror visitedEnclosingType = this.visit(enclosingType);
      if (enclosingType == visitedEnclosingType) {
        return t;
      }
      final org.microbean.lang.type.DeclaredType dt = new org.microbean.lang.type.DeclaredType();
      dt.addTypeArguments(t.getTypeArguments());
      dt.addAnnotationMirrors(t.getAnnotationMirrors());
      dt.setDefiningElement((TypeElement)t.asElement());
      dt.setEnclosingType(visitedEnclosingType);
      return dt;
    }

    @Override
    public final TypeMirror visitTypeVariable(final TypeVariable t, final Void x) {
      assert t.getKind() == TypeKind.TYPEVAR;
      return this.visit(SupertypeVisitor.this.visitTypeVariable(t, x));
    }
  }

}
