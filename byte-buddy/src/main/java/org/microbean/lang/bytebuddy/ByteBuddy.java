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
package org.microbean.lang.bytebuddy;

import java.util.Objects;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.TypeMirror;

import net.bytebuddy.description.method.MethodDescription;

import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDefinition.Sort;
import net.bytebuddy.description.type.TypeDescription;

import net.bytebuddy.pool.TypePool;
import net.bytebuddy.pool.TypePool.Resolution;

import org.microbean.lang.Modeler;

import org.microbean.lang.type.DefineableType;

@Deprecated
public final class ByteBuddy extends Modeler {

  private final TypePool typePool;

  public ByteBuddy() {
    this(TypePool.Default.ofSystemLoader());
  }

  public ByteBuddy(final TypePool typePool) {
    super();
    this.typePool = Objects.requireNonNull(typePool, "typePool");
  }

  public Element element(final Object k) {
    return switch (k) {
    case String s -> this.element(s);
    case CharSequence cs -> this.element(this.typePool.describe(cs.toString()).resolve()); // RECURSIVE
    case MethodDescription.InDefinedShape mdids -> this.element(mdids, () -> new org.microbean.lang.element.ExecutableElement(elementKind(mdids)), this::build);
    case PackageDescription pd -> this.element(pd, org.microbean.lang.element.PackageElement::new, this::build);
    case TypeDescription.ForPackageDescription pd -> this.element(pd, org.microbean.lang.element.PackageElement::new, this::build);
    case TypeDescription td -> this.element(td, () -> new org.microbean.lang.element.TypeElement(elementKind(td), nestingKind(td)), this::build);
    default -> throw new IllegalArgumentException("k: " + k + "; k.getClass(): " + k.getClass());
    };
  }

  @Override // ElementSource
  public Element element(final String n) {
    return this.element(this.typePool.describe(n).resolve());
  }

  @Override // ElementSource
  public Element element(final String m, final String n) {
    return this.element(n);
  }

  public TypeMirror type(final Object k) {
    if (k == null) {
      return org.microbean.lang.type.NoType.NONE;
    } else if (k == void.class || k == TypeDescription.ForLoadedType.of(void.class)) {
      return org.microbean.lang.type.NoType.VOID;
    }
    return switch (k) {
    case CharSequence cs -> this.type(this.typePool.describe(cs.toString()).resolve().asGenericType()); // RECURSIVE

    case PackageDescription pd -> org.microbean.lang.type.NoType.PACKAGE;
    case TypeDescription td when td.isPackageType() -> org.microbean.lang.type.NoType.PACKAGE;
    case TypeDescription td -> this.type(td.asGenericType()); // RECURSIVE
      
    case TypeDescription.Generic tdg when tdg.isPrimitive() -> throw new UnsupportedOperationException("TODO: implement?");
    case TypeDescription.Generic tdg when tdg.isArray() -> this.type(tdg, org.microbean.lang.type.ArrayType::new, this::build);
    case TypeDescription.Generic tdg -> this.type(tdg, org.microbean.lang.type.DeclaredType::new, this::build);

    default -> throw new IllegalArgumentException("k: " + k + "; k.getClass(): " + k.getClass());
    };
  }


  /*
   * Type builders.
   */

  
  private final void build(final TypeDescription.Generic tdg, final org.microbean.lang.type.ArrayType t) {
    throw new UnsupportedOperationException("TODO: implement");
  }

  private final void build(final TypeDescription.Generic tdg, final org.microbean.lang.type.DeclaredType t) {
    assert !tdg.isArray();
    assert !tdg.isPrimitive();
    t.setEnclosingType(this.type(tdg.getOwnerType()));
    if (tdg.getSort() == Sort.PARAMETERIZED) {
      for (final TypeDescription.Generic ta : tdg.getTypeArguments()) {
        t.addTypeArgument(type(ta));
      }
    }
  }


  /*
   * Element builders.
   */


  private final void build(final MethodDescription.InDefinedShape m, final org.microbean.lang.element.ExecutableElement e) {

    throw new UnsupportedOperationException("TODO: implement");
  }

  private final void build(final TypeDescription.ForPackageDescription pd, final org.microbean.lang.element.PackageElement e) {
    // What a weird model.
    this.build(pd.getPackage(), e);
  }
  
  private final void build(final PackageDescription pd, final org.microbean.lang.element.PackageElement e) {
    System.out.println("*** pd.getActualName(): " + pd.getActualName());
    System.out.println("*** pd.getInternalName(): " + pd.getInternalName());
    System.out.println("*** pd.getName(): " + pd.getName());
    e.setSimpleName(pd.getName());
    e.setType(type(pd));
    // NOTE: no enclosing element because ByteBuddy doesn't really do modules
  }

  private final void build(final TypeDescription td, final org.microbean.lang.element.TypeElement e) {
    this.build(td.asGenericType(), e);
  }

  private final void build(final TypeDescription.Generic td, final org.microbean.lang.element.TypeElement e) {
    // ByteBuddy's way of modeling things is quite odd.
    //
    // A TypeDescription (as opposed to a TypeDescription.Generic) does not fully model anything other than a
    // non-generic class.  So if this method is called with, say, a TypeDescription representing the declaration of
    // List.class, you will not be able to discover that "really" the declaration of this type has type parameters.
    //
    // Thinking out loud: I guess this is fundamentally because Byte Buddy models types first, then you get to elements
    // from them.  The javax.lang.model.* hierarchy kind of goes the other way: you have an element, and it's always
    // backed by a type that it declares.  This is all more conceptual than rigorous of course, but I'm trying to find
    // the mental model for each toolkit.
    //
    // Anyway, here we have an "element", which is just the "List" part of "List<E>" or "List<String>". So there's only
    // so much we can get out of it.
    assert !td.asErasure().isPackageType() : "td.isPackageType(): " + td;
    assert !td.asErasure().isArray();
    assert !td.asErasure().isPrimitive();
    e.setSimpleName(td.asErasure().getSimpleName());
    @SuppressWarnings("unchecked")
    final DefineableType<TypeElement> t = (DefineableType<TypeElement>)type(td);
    e.setType(t);
    t.setDefiningElement(e);

    // Modifiers
    if (td.isAbstract()) {
      e.addModifier(Modifier.ABSTRACT);
    } else if (td.isFinal()) {
      e.addModifier(Modifier.FINAL);
    }
    if (td.isPrivate()) {
      e.addModifier(Modifier.PRIVATE);
    } else if (td.isProtected()) {
      e.addModifier(Modifier.PROTECTED);
    } else if (td.isPublic()) {
      e.addModifier(Modifier.PUBLIC);
    }
    if (td.asErasure().isSealed()) {
      e.addModifier(Modifier.SEALED);
      for (final TypeDescription sc : td.asErasure().getPermittedSubtypes()) {
        e.addPermittedSubclass(type(sc));
      }
    }
    if (td.isStatic()) {
      e.addModifier(Modifier.STATIC);
    }      
    
    final Element enclosingElement;
    switch (e.getNestingKind()) {
    case ANONYMOUS:
    case LOCAL:
      Object enclosingObject = td.asErasure().getEnclosingMethod();
      if (enclosingObject == null) {
        enclosingObject = td.asErasure().getEnclosingType();
      }
      enclosingElement = element(enclosingObject);
      break;
    case MEMBER:
      enclosingElement = element(td.asErasure().getDeclaringType());
      break;
    case TOP_LEVEL:
      enclosingElement = element(td.asErasure().getPackage());
      break;
    default:
      throw new AssertionError();
    }
    e.setEnclosingElement(enclosingElement);

    e.setSuperclass(type(td.getSuperClass()));

    // These are actually annotated interfaces.
    for (final TypeDescription.Generic iface : td.getInterfaces()) {
      e.addInterface(type(iface));
    }

    // Suppose td is "supposed to" model List.class.  You would want to set e's type parameters from it.  But you can't.

    // TODO: annotations
    
    throw new UnsupportedOperationException("TODO: implement");
  }

  public static final ElementKind elementKind(final TypeDefinition td) {
    return
      td.isAnnotation() ? ElementKind.ANNOTATION_TYPE :
      td.isEnum() ? ElementKind.ENUM :
      td.isInterface() ? ElementKind.INTERFACE :
      td.isRecord() ? ElementKind.RECORD :
      !td.isPrimitive() && !td.isArray() ? ElementKind.CLASS :
      ElementKind.OTHER;
  }

  public static final ElementKind elementKind(final MethodDescription md) {
    return
      md.isConstructor() ? ElementKind.CONSTRUCTOR :
      md.isTypeInitializer() ? ElementKind.STATIC_INIT :
      md.getName().equals("<init>") ? ElementKind.INSTANCE_INIT : // remember we already checked constructorhood
      md.isMethod() ? ElementKind.METHOD :
      ElementKind.OTHER;
  }


  public static final NestingKind nestingKind(final TypeDefinition tdef) {
    final TypeDescription td = tdef.asErasure();
    return
      td.isAnonymousType() ? NestingKind.ANONYMOUS :
      td.isLocalType() ? NestingKind.LOCAL :
      td.isMemberType() ? NestingKind.MEMBER :
      NestingKind.TOP_LEVEL;
  }

  // Experimental
  public static final TypeDescription.Generic typeDescriptionGenericOf(final Class<?> c) {
    final java.lang.reflect.TypeVariable<?>[] tps = c.getTypeParameters();
    if (tps.length <= 0) {
      return TypeDescription.ForLoadedType.of(c).asGenericType();
    }
    return TypeDescription.Generic.Builder.parameterizedType(c, tps).build();
  }
  
  @FunctionalInterface
  private static interface Builder<K, T> {

    public void build(final K k, final T t);

  }

}
