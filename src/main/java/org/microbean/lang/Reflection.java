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
package org.microbean.lang;

import java.lang.annotation.Annotation;

import java.lang.module.ModuleDescriptor;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import org.microbean.lang.element.DelegatingElement;
import org.microbean.lang.element.Encloseable;

import org.microbean.lang.type.DefineableType;

public final class Reflection {

  private static final Equality EQUALITY_NO_ANNOTATIONS = new Equality(false);

  private final Map<Annotation, AnnotationMirror> annotationMirrors;

  private final Map<Object, TypeMirror> typeMirrors;

  private final Map<Object, Element> elements;

  Reflection() {
    super();
    this.annotationMirrors = new ConcurrentHashMap<>();
    this.elements = new ConcurrentHashMap<>();
    this.typeMirrors = new ConcurrentHashMap<>();
    this.typeMirrors.put(boolean.class, org.microbean.lang.type.PrimitiveType.BOOLEAN);
    this.typeMirrors.put(byte.class, org.microbean.lang.type.PrimitiveType.BYTE);
    this.typeMirrors.put(char.class, org.microbean.lang.type.PrimitiveType.CHAR);
    this.typeMirrors.put(double.class, org.microbean.lang.type.PrimitiveType.DOUBLE);
    this.typeMirrors.put(float.class, org.microbean.lang.type.PrimitiveType.FLOAT);
    this.typeMirrors.put(int.class, org.microbean.lang.type.PrimitiveType.INT);
    this.typeMirrors.put(long.class, org.microbean.lang.type.PrimitiveType.LONG);
    this.typeMirrors.put(short.class, org.microbean.lang.type.PrimitiveType.SHORT);
    this.typeMirrors.put(void.class, org.microbean.lang.type.NoType.VOID);
  }

  final void clear() {
    this.annotationMirrors.clear();
    this.typeMirrors.clear();
    this.elements.clear();
  }


  /*
   * Annotations.
   */


  public final AnnotationMirror annotation(final Annotation a, final ClassLoader cl) throws ReflectiveOperationException {
    AnnotationMirror r = this.annotationMirrors.get(a);
    if (r == null) {
      final org.microbean.lang.element.AnnotationMirror am = new org.microbean.lang.element.AnnotationMirror();
      r = this.annotationMirrors.putIfAbsent(a, am);
      if (r == null) {
        this.build(cl, a, am);
        r = am;
      }
    }
    return r;
  }

  public final List<? extends AnnotationMirror> annotations(final AnnotatedElement ae, final ClassLoader cl) throws ReflectiveOperationException {
    return ae == null ? List.of() : annotations(ae.getDeclaredAnnotations(), cl);
  }

  private final List<? extends AnnotationMirror> annotations(final Annotation[] annotations, final ClassLoader cl) throws ReflectiveOperationException {
    if (annotations == null || annotations.length <= 0) {
      return List.of();
    }
    final List<AnnotationMirror> l = new ArrayList<>(annotations.length);
    for (final Annotation a : annotations) {
      l.add(annotation(a, cl));
    }
    return Collections.unmodifiableList(l);
  }

  public final List<? extends AnnotationMirror> annotations(final Enum<?> e, final ClassLoader cl) throws ReflectiveOperationException {
    if (e == null) {
      return List.of();
    }
    // Not sure this is needed or correct.
    final Field[] fields = e.getDeclaringClass().getDeclaredFields();
    final int ordinal = e.ordinal();
    for (int i = 0; i < fields.length; i++) {
      final Field field = fields[i];
      if (i == ordinal && field.isEnumConstant()) {
        return annotations(field, cl);
      }
    }
    return List.of();
  }

  public final AnnotationValue annotationValue(final Object value, final ClassLoader cl) throws ReflectiveOperationException {
    if (value == null) {
      return null;
    }
    return switch (value) {
    case String s -> new org.microbean.lang.element.AnnotationValue(s);
    case Boolean b -> new org.microbean.lang.element.AnnotationValue(b);
    case Integer i -> new org.microbean.lang.element.AnnotationValue(i);
    case Enum<?> e -> annotationValue(element(e, cl), cl);
    case Class<?> c -> annotationValue(type(c, cl), cl);
    case Object[] array -> new org.microbean.lang.element.AnnotationValue(annotationValues(array, cl));
    case Annotation a -> annotationValue(annotation(a, cl), cl);
    case Byte b -> new org.microbean.lang.element.AnnotationValue(b);
    case Character c -> new org.microbean.lang.element.AnnotationValue(c);
    case Double d -> new org.microbean.lang.element.AnnotationValue(d);
    case Float f -> new org.microbean.lang.element.AnnotationValue(f);
    case Long l -> new org.microbean.lang.element.AnnotationValue(l);
    case Short s -> new org.microbean.lang.element.AnnotationValue(s);
    case Method m -> annotationValue(m.getDefaultValue(), cl);
    case TypeMirror t -> new org.microbean.lang.element.AnnotationValue(t);
    case VariableElement v when v.getKind() == ElementKind.ENUM_CONSTANT -> new org.microbean.lang.element.AnnotationValue(v);
    case AnnotationMirror a -> new org.microbean.lang.element.AnnotationValue(a);
    case Collection<?> c -> new org.microbean.lang.element.AnnotationValue(annotationValues(c, cl));
    case AnnotationValue a -> a;
    default -> throw new IllegalArgumentException("value: " + value);
    };
  }

  private final List<? extends AnnotationValue> annotationValues(final Collection<?> c, final ClassLoader cl)
    throws ReflectiveOperationException {
    if (c == null || c.isEmpty()) {
      return List.of();
    }
    final List<AnnotationValue> list = new ArrayList<>(c.size());
    for (final Object value : c) {
      list.add(annotationValue(value, cl));
    }
    return Collections.unmodifiableList(list);
  }

  private final List<? extends AnnotationValue> annotationValues(final Object[] o, final ClassLoader cl)
    throws ReflectiveOperationException {
    if (o == null || o.length <= 0) {
      return List.of();
    }
    final List<AnnotationValue> list = new ArrayList<>(o.length);
    for (final Object value : o) {
      list.add(annotationValue(value, cl));
    }
    return Collections.unmodifiableList(list);
  }


  /*
   * Elements.
   */


  public final Element element(final Object k, final ClassLoader cl) throws ReflectiveOperationException {
    Element r = this.elements.get(k);
    if (r == null) {
      return switch (k) {
      case AnnotatedType a -> element(a.getType(), cl); // RECURSIVE
      case Annotation a -> element(a.annotationType(), cl); // RECURSIVE
      case Class<?> c -> element(cl, c, () -> new org.microbean.lang.element.TypeElement(kind(c), nestingKind(c)), this::build);
      case Constructor<?> c -> element(cl, c, () -> new org.microbean.lang.element.ExecutableElement(ElementKind.CONSTRUCTOR), this::build);
      case Enum<?> e -> element(cl, e, () -> new org.microbean.lang.element.VariableElement(ElementKind.ENUM_CONSTANT), this::build);
      case Field f -> element(cl, f, () -> new org.microbean.lang.element.VariableElement(ElementKind.FIELD), this::build);
      case Method m -> element(cl, m, () -> new org.microbean.lang.element.ExecutableElement(kind(m)), this::build);
      case Module m -> element(cl, m, () -> new org.microbean.lang.element.ModuleElement(m.getDescriptor().isOpen()), this::build); // TODO: inherently sparse
      case Package p -> element(cl, p, org.microbean.lang.element.PackageElement::new, this::build); // TODO: inherently sparse
      case Parameter p -> element(cl, p, () -> new org.microbean.lang.element.VariableElement(ElementKind.PARAMETER), this::build);
      case ParameterizedType p -> element(p.getRawType(), cl); // RECURSIVE
      case java.lang.reflect.TypeVariable<?> tp -> element(cl, tp, org.microbean.lang.element.TypeParameterElement::new, this::build);
      default -> throw new IllegalArgumentException("k: " + k + "; k.getClass(): " + k.getClass());
      };
    }
    return r;
  }

  private final Element element(final Module m, final Package p, final ClassLoader cl) throws ReflectiveOperationException {
    Element r = this.elements.get(p);
    if (r == null) {
      final org.microbean.lang.element.PackageElement e = new org.microbean.lang.element.PackageElement();
      r = this.elements.putIfAbsent(p, e);
      if (r == null) {
        this.build(cl, m, p, e);
        r = e;
      }
    }
    return r;
  }


  /*
   * Types.
   */


  public final TypeMirror type(final Object k, final ClassLoader cl) throws ReflectiveOperationException {
    if (k == null) {
      return org.microbean.lang.type.NoType.NONE;
    } else if (k == void.class) {
      return org.microbean.lang.type.NoType.VOID;
    }
    TypeMirror r = this.typeMirrors.get(k);
    if (r == null) {
      return switch (k) {

      case Class<?> c when c.isPrimitive() -> this.typeMirrors.get(c); // already in there
      case Class<?> c when c.isArray() -> type(cl, c, org.microbean.lang.type.ArrayType::new, this::build);
      case Class<?> c -> type(cl, c, org.microbean.lang.type.DeclaredType::new, this::build);

      case ParameterizedType p -> type(cl, p, org.microbean.lang.type.DeclaredType::new, this::build);

      case GenericArrayType g -> type(cl, g, org.microbean.lang.type.ArrayType::new, this::build);

      case java.lang.reflect.TypeVariable<?> t -> type(cl, t, org.microbean.lang.type.TypeVariable::new, this::build);

      case java.lang.reflect.WildcardType w -> type(cl, w, org.microbean.lang.type.WildcardType::new, this::build);

      case AnnotatedArrayType a -> type(cl, a, org.microbean.lang.type.ArrayType::new, this::build);

      case AnnotatedParameterizedType a -> type(cl, a, org.microbean.lang.type.DeclaredType::new, this::build);

      case AnnotatedTypeVariable a -> type(cl, a, org.microbean.lang.type.TypeVariable::new, this::build);

      case AnnotatedWildcardType a -> type(cl, a, org.microbean.lang.type.WildcardType::new, this::build);

      // Not sure this is even possible?
      case AnnotatedType a when a.getType() instanceof Class<?> c && c.isPrimitive() ->
        type(cl, a, () -> new org.microbean.lang.type.PrimitiveType(typeKindForPrimitive(c)), this::build);
      // Not sure this is even possible?
      case AnnotatedType a when a.getType() instanceof Class<?> c && c.isArray() ->
        type(cl, a, org.microbean.lang.type.ArrayType::new, this::build);
      case AnnotatedType a when a.getType() instanceof Class<?> c ->
        type(cl, a, org.microbean.lang.type.DeclaredType::new, this::build);

      case Constructor<?> c -> type(cl, c, org.microbean.lang.type.ExecutableType::new, this::build);
      case Enum e -> type(e.getDeclaringClass(), cl); // RECURSIVE and kind of cheesy
      case Field f when f.isEnumConstant() -> throw new UnsupportedOperationException("TODO: treat this as Enum<?>?");
      case Field f -> type(f.getAnnotatedType(), cl); // RECURSIVE and also kind of cheesy
      case Method m -> type(cl, m, org.microbean.lang.type.ExecutableType::new, this::build);
      case Module m -> org.microbean.lang.type.NoType.MODULE;
      case Package p -> org.microbean.lang.type.NoType.PACKAGE;
      case Parameter p -> type(p.getAnnotatedType(), cl); // RECURSIVE and kind of cheesy
      case CharSequence s -> type(Class.forName(s.toString(), false, cl), cl); // RECURSIVE
      case TypeMirror t -> t;
      default -> throw new IllegalArgumentException("k: " + k);
      };
    }
    return r;
  }


  /*
   * Element builders.
   */


  private final void build(final ClassLoader cl,
                           final Module m,
                           final org.microbean.lang.element.ModuleElement e)
    throws ReflectiveOperationException {
    final ModuleDescriptor md = m.getDescriptor();
    e.setSimpleName(md.name());
    e.setType(type(m, cl));
  }

  private final void build(final ClassLoader cl,
                           final Package p,
                           final org.microbean.lang.element.PackageElement e)
  throws ReflectiveOperationException {
    this.build(cl, null, p, e);
  }

  private final void build(final ClassLoader cl,
                           final Module m,
                           final Package p,
                           final org.microbean.lang.element.PackageElement pe)
    throws ReflectiveOperationException {
    pe.setSimpleName(p.getName());
    pe.setType(type(p, cl));
    pe.setEnclosingElement(element(m, cl));
  }

  private final void build(final ClassLoader cl,
                           final Class<?> c,
                           final org.microbean.lang.element.TypeElement e)
    throws ReflectiveOperationException {
    e.setSimpleName(c.getSimpleName());
    @SuppressWarnings("unchecked")
    final DefineableType<TypeElement> t = (DefineableType<TypeElement>)type(c, cl);
    e.setType(t);
    t.setDefiningElement(e);

    final int modifiers = c.getModifiers();
    if (java.lang.reflect.Modifier.isAbstract(modifiers)) {
      e.addModifier(Modifier.ABSTRACT);
    } else if (java.lang.reflect.Modifier.isFinal(modifiers)) {
      e.addModifier(Modifier.FINAL);
    }
    if (java.lang.reflect.Modifier.isPrivate(modifiers)) {
      e.addModifier(Modifier.PRIVATE);
    } else if (java.lang.reflect.Modifier.isProtected(modifiers)) {
      e.addModifier(Modifier.PROTECTED);
    } else if (java.lang.reflect.Modifier.isPublic(modifiers)) {
      e.addModifier(Modifier.PUBLIC);
    }
    if (c.isSealed()) {
      e.addModifier(Modifier.SEALED);
      for (final Class<?> sc : c.getPermittedSubclasses()) {
        e.addPermittedSubclass(type(sc, cl));
      }
    }
    if (java.lang.reflect.Modifier.isStatic(modifiers)) {
      e.addModifier(Modifier.STATIC);
    }

    final Element enclosingElement;
    switch (e.getNestingKind()) {
    case ANONYMOUS:
    case LOCAL:
      Object enclosingObject = c.getEnclosingMethod();
      if (enclosingObject == null) {
        enclosingObject = c.getEnclosingConstructor();
      }
      // TODO: Note: we don't want to call c.getEnclosingClass(), because that will jump the hierarchy. An anonymous
      // class is lexically enclosed only by an non-static executable.  (I think. :-) (What about private Gorp gorp =
      // new Gorp() {};?))
      enclosingElement = element(enclosingObject, cl);
      break;
    case MEMBER:
      enclosingElement = element(c.getDeclaringClass(), cl);
      break;
    case TOP_LEVEL:
      enclosingElement = element(c.getModule(), c.getPackage(), cl);
      break;
    default:
      throw new AssertionError();
    }
    e.setEnclosingElement(enclosingElement);

    e.setSuperclass(type(c.getAnnotatedSuperclass(), cl));

    for (final AnnotatedType ai : c.getAnnotatedInterfaces()) {
      e.addInterface(((TypeElement)element(ai, cl)).asType()); // experimental
      // e.addInterface(type(ai, cl));
    }
    buildTypeParameters(cl,
                        c,
                        e::addTypeParameter);
    e.addAnnotationMirrors(annotations(c, cl));
  }

  private final void build(final ClassLoader cl,
                           final java.lang.reflect.TypeVariable<?> tv,
                           final org.microbean.lang.element.TypeParameterElement tp)
    throws ReflectiveOperationException {
    tp.setSimpleName(tv.getName());
    @SuppressWarnings("unchecked")
    final DefineableType<TypeParameterElement> t = (DefineableType<TypeParameterElement>)type(tv, cl);
    tp.setType(t);
    t.setDefiningElement(tp);
  }

  private final void build(final ClassLoader cl,
                           final Enum<?> e,
                           final org.microbean.lang.element.VariableElement v)
    throws ReflectiveOperationException {
    v.setSimpleName(e.name());
    v.setType(type(e.getDeclaringClass(), cl));
    // TODO: that's it, right?
  }

  private final void build(final ClassLoader cl,
                           final Constructor<?> c,
                           final org.microbean.lang.element.ExecutableElement e)
    throws ReflectiveOperationException {
    // (No setSimpleName() on purpose; already performed by ExecutableElement's constructor.)
    e.setType(type(c, cl));

    final int modifiers = c.getModifiers();
    if (java.lang.reflect.Modifier.isPrivate(modifiers)) {
      e.addModifier(Modifier.PRIVATE);
    } else if (java.lang.reflect.Modifier.isProtected(modifiers)) {
      e.addModifier(Modifier.PROTECTED);
    } else if (java.lang.reflect.Modifier.isPublic(modifiers)) {
      e.addModifier(Modifier.PUBLIC);
    }

    buildTypeParameters(cl, c, e::addTypeParameter);

    for (final Parameter p : c.getParameters()) {
      e.addParameter((VariableElement)element(p, cl));
    }

    e.addAnnotationMirrors(annotations(c, cl));
  }

  private final void build(final ClassLoader cl,
                           final Field f,
                           final org.microbean.lang.element.VariableElement e)
    throws ReflectiveOperationException {
    e.setSimpleName(f.getName());
    final AnnotatedType at = f.getAnnotatedType();
    e.setType(type(at, cl));

    boolean isFinal = false;
    final int modifiers = f.getModifiers();
    if (java.lang.reflect.Modifier.isAbstract(modifiers)) {
      e.addModifier(Modifier.ABSTRACT);
    } else if (java.lang.reflect.Modifier.isFinal(modifiers)) {
      e.addModifier(Modifier.FINAL);
      isFinal = true;
    }
    if (java.lang.reflect.Modifier.isPrivate(modifiers)) {
      e.addModifier(Modifier.PRIVATE);
    } else if (java.lang.reflect.Modifier.isProtected(modifiers)) {
      e.addModifier(Modifier.PROTECTED);
    } else if (java.lang.reflect.Modifier.isPublic(modifiers)) {
      e.addModifier(Modifier.PUBLIC);
    }
    if (java.lang.reflect.Modifier.isStatic(modifiers)) {
      e.addModifier(Modifier.STATIC);
      if (isFinal && at.getType() instanceof Class<?> c && (c == String.class || c.isPrimitive()) && f.trySetAccessible()) {
        e.setConstantValue(f.get(null));
      }
    }
    if (java.lang.reflect.Modifier.isStrict(modifiers)) {
      e.addModifier(Modifier.STRICTFP);
    }
    if (java.lang.reflect.Modifier.isSynchronized(modifiers)) {
      e.addModifier(Modifier.SYNCHRONIZED);
    }
    if (java.lang.reflect.Modifier.isTransient(modifiers)) {
      e.addModifier(Modifier.TRANSIENT);
    }
    if (java.lang.reflect.Modifier.isVolatile(modifiers)) {
      e.addModifier(Modifier.VOLATILE);
    }
    e.addAnnotationMirrors(annotations(f, cl));
  }

  private final void build(final ClassLoader cl,
                           final Method m,
                           final org.microbean.lang.element.ExecutableElement e)
    throws ReflectiveOperationException {
    e.setSimpleName(m.getName());
    e.setType(type(m, cl));

    final int modifiers = m.getModifiers();
    if (m.isDefault()) {
      e.setDefault(true);
      e.addModifier(Modifier.DEFAULT);
    } else {
      e.setDefault(false);
      if (java.lang.reflect.Modifier.isAbstract(modifiers)) {
        e.addModifier(Modifier.ABSTRACT);
      } else if (java.lang.reflect.Modifier.isFinal(modifiers)) {
        e.addModifier(Modifier.FINAL);
      } else if (java.lang.reflect.Modifier.isNative(modifiers)) {
        e.addModifier(Modifier.NATIVE);
      }
      if (java.lang.reflect.Modifier.isStatic(modifiers)) {
        e.addModifier(Modifier.STATIC);
      }
      if (java.lang.reflect.Modifier.isSynchronized(modifiers)) {
        e.addModifier(Modifier.SYNCHRONIZED);
      }
    }
    if (java.lang.reflect.Modifier.isPrivate(modifiers)) {
      e.addModifier(Modifier.PRIVATE);
    } else if (java.lang.reflect.Modifier.isProtected(modifiers)) {
      e.addModifier(Modifier.PROTECTED);
    } else if (java.lang.reflect.Modifier.isPublic(modifiers)) {
      e.addModifier(Modifier.PUBLIC);
    }
    buildTypeParameters(cl, m, e::addTypeParameter);

    for (final Parameter p : m.getParameters()) {
      e.addParameter((VariableElement)element(p, cl));
    }
    
    e.addAnnotationMirrors(annotations(m, cl));
  }

  private final void build(final ClassLoader cl,
                           final Parameter p,
                           final org.microbean.lang.element.VariableElement e)
    throws ReflectiveOperationException {
    e.setType(type(p, cl));
    e.addAnnotationMirrors(annotations(p, cl));
  }

  @SuppressWarnings("unchecked")
  private final <TP extends javax.lang.model.element.TypeParameterElement & Encloseable> void buildTypeParameters(final ClassLoader cl,
                                                                                                                  final GenericDeclaration gd,
                                                                                                                  final Consumer<? super TP> c)
    throws ReflectiveOperationException {
    for (final java.lang.reflect.TypeVariable<?> tp : gd.getTypeParameters()) {
      c.accept((TP)element(tp, cl));
    }
  }


  /*
   * Unannotated type builders.
   */


  private final void build(final ClassLoader cl,
                           final Class<?> c,
                           final org.microbean.lang.type.ArrayType t)
    throws ReflectiveOperationException {
    assert c.isArray() : "c: " + c;
    t.setComponentType(this.type(c.getComponentType(), cl));
    // (No annotations.)
  }

  private final void build(final ClassLoader cl,
                           final Class<?> c,
                           final org.microbean.lang.type.DeclaredType t)
    throws ReflectiveOperationException {
    assert c != void.class && !c.isPrimitive() && !c.isArray() : "c: " + c;
    final org.microbean.lang.element.TypeElement e = (org.microbean.lang.element.TypeElement)element(c, cl);
    e.setType(t); // TODO: maybe assert that element() does this already instead?
    t.setDefiningElement(e); // TODO: ditto?
    t.setEnclosingType(this.type(c.getEnclosingClass(), cl));
    for (final java.lang.reflect.TypeVariable<?> tp : c.getTypeParameters()) {
      t.addTypeArgument(type(tp, cl));
    }
    // (No annotations.)
  }

  private final void build(final ClassLoader cl,
                           final ParameterizedType p,
                           final org.microbean.lang.type.DeclaredType t)
    throws ReflectiveOperationException {
    t.setEnclosingType(this.type(p.getOwnerType(), cl));
    for (final java.lang.reflect.Type ta : p.getActualTypeArguments()) {
      t.addTypeArgument(this.type(ta, cl));
    }
    // (No annotations.)
  }

  private final void build(final ClassLoader cl,
                           final GenericArrayType g,
                           final org.microbean.lang.type.ArrayType t)
    throws ReflectiveOperationException {
    t.setComponentType(this.type(g.getGenericComponentType(), cl));
    // (No annotations.)
  }

  private final void build(final ClassLoader cl,
                           final java.lang.reflect.TypeVariable<?> tv,
                           final org.microbean.lang.type.TypeVariable t)
    throws ReflectiveOperationException {
    final AnnotatedType[] bounds = tv.getAnnotatedBounds();
    switch (bounds.length) {
    case 0:
      break;
    case 1:
      t.setUpperBound(this.type(bounds[0], cl));
      break;
    default:
      final org.microbean.lang.type.IntersectionType upperBound = new org.microbean.lang.type.IntersectionType();
      for (final AnnotatedType bound : bounds) {
        upperBound.addBound(this.type(bound, cl));
      }
      t.setUpperBound(upperBound);
      break;
    }
    final TypeParameterElement e = (TypeParameterElement)element(tv, cl);
    t.setDefiningElement(e);
    // (No annotations; although java.lang.reflect.TypeVariable can yield them, because java.lang.reflect.TypeVariable
    // represents both a type and an element they are element annotations, not type use annotations.)
  }

  private final void build(final ClassLoader cl,
                           final java.lang.reflect.WildcardType w,
                           final org.microbean.lang.type.WildcardType t)
    throws ReflectiveOperationException {
    final Type[] superBounds = w.getLowerBounds();
    switch (superBounds.length) {
    case 0:
      break;
    case 1:
      t.setSuperBound(type(superBounds[0], cl));
      break;
    default:
      throw new IllegalArgumentException("w: " + w);
    }
    final Type[] extendsBounds = w.getUpperBounds();
    switch (extendsBounds.length) {
    case 0:
      break;
    case 1:
      t.setExtendsBound(type(extendsBounds[0], cl));
      break;
    default:
      throw new IllegalArgumentException("w: " + w);
    }
    // (No annotations.)
  }

  private final void build(final ClassLoader cl,
                           final java.lang.reflect.Constructor<?> c,
                           final org.microbean.lang.type.ExecutableType e)
    throws ReflectiveOperationException {
    for (final AnnotatedType at : c.getAnnotatedParameterTypes()) {
      e.addParameterType(type(at, cl));
    }
    e.setReceiverType(type(c.getAnnotatedReceiverType(), cl));
    // e.setReturnType(type(c.getAnnotatedReturnType(), cl)); // not sure about this
    for (final AnnotatedType at : c.getAnnotatedExceptionTypes()) {
      e.addThrownType(type(at, cl));
    }
    for (final java.lang.reflect.TypeVariable<?> tv : c.getTypeParameters()) {
      e.addTypeVariable((TypeVariable)type(tv, cl));
    }
    e.addAnnotationMirrors(annotations(c, cl));
  }

  private final void build(final ClassLoader cl,
                           final java.lang.reflect.Method m,
                           final org.microbean.lang.type.ExecutableType e)
    throws ReflectiveOperationException {
    for (final AnnotatedType at : m.getAnnotatedParameterTypes()) {
      e.addParameterType(type(at, cl));
    }
    e.setReceiverType(type(m.getAnnotatedReceiverType(), cl));
    e.setReturnType(type(m.getAnnotatedReturnType(), cl));
    for (final AnnotatedType at : m.getAnnotatedExceptionTypes()) {
      e.addThrownType(type(at, cl));
    }
    for (final java.lang.reflect.TypeVariable<?> tv : m.getTypeParameters()) {
      e.addTypeVariable((TypeVariable)type(tv, cl));
    }
    e.addAnnotationMirrors(annotations(m, cl));
  }


  /*
   * Annotated type builders.
   */


  private final void build(final ClassLoader cl,
                           final AnnotatedArrayType a,
                           final org.microbean.lang.type.ArrayType t)
    throws ReflectiveOperationException {
    t.setComponentType(type(a.getAnnotatedGenericComponentType(), cl));
    t.addAnnotationMirrors(annotations(a, cl));
  }

  // (Not sure this is even possible.)
  @Deprecated
  private final void build(final ClassLoader cl,
                           final AnnotatedType a,
                           final org.microbean.lang.type.ArrayType t)
    throws ReflectiveOperationException {
    assert a != null;
    assert !(a instanceof AnnotatedArrayType) : "a: " + a;
    assert !(a instanceof AnnotatedParameterizedType) : "a: " + a;
    assert !(a instanceof AnnotatedTypeVariable) : "a: " + a;
    assert !(a instanceof AnnotatedWildcardType) : "a: " + a;
    final Class<?> c = (Class<?>)a.getType();
    assert c.isArray() : "c: " + c;
    // Is this really a thing?
    throw new UnsupportedOperationException();
  }

  private final void build(final ClassLoader cl,
                           final AnnotatedType a,
                           final org.microbean.lang.type.DeclaredType t)
    throws ReflectiveOperationException {
    assert a != null;
    assert !(a instanceof AnnotatedArrayType) : "a: " + a;
    assert !(a instanceof AnnotatedParameterizedType) : "a: " + a;
    assert !(a instanceof AnnotatedTypeVariable) : "a: " + a;
    assert !(a instanceof AnnotatedWildcardType) : "a: " + a;
    final Class<?> c = (Class<?>)a.getType();
    if (c.isPrimitive() || c.isArray()) {
      throw new IllegalArgumentException("a: " + a);
    }
    t.setDefiningElement((TypeElement)((DeclaredType)type(c, cl)).asElement());
    t.setEnclosingType(type(a.getAnnotatedOwnerType(), cl));
    t.addAnnotationMirrors(annotations(a, cl));
  }

  // (Not sure this is even possible.)
  @Deprecated
  private final void build(final ClassLoader cl,
                           final AnnotatedType a,
                           final org.microbean.lang.type.PrimitiveType t)
    throws ReflectiveOperationException {
    assert a != null;
    assert !(a instanceof AnnotatedArrayType) : "a: " + a;
    assert !(a instanceof AnnotatedParameterizedType) : "a: " + a;
    assert !(a instanceof AnnotatedTypeVariable) : "a: " + a;
    assert !(a instanceof AnnotatedWildcardType) : "a: " + a;
    final Class<?> c = (Class<?>)a.getType();
    assert c.isPrimitive() : "c: " + c;
    // Is this really a thing?
    throw new UnsupportedOperationException();
  }

  private final void build(final ClassLoader cl,
                           final AnnotatedParameterizedType a,
                           final org.microbean.lang.type.DeclaredType t)
    throws ReflectiveOperationException {
    t.setEnclosingType(type(a.getAnnotatedOwnerType(), cl));
    for (final AnnotatedType ata : a.getAnnotatedActualTypeArguments()) {
      t.addTypeArgument(type(ata, cl));
    }
    t.addAnnotationMirrors(annotations(a, cl));
  }

  private final void build(final ClassLoader cl,
                           final AnnotatedTypeVariable a,
                           final org.microbean.lang.type.TypeVariable t)
    throws ReflectiveOperationException {
    final AnnotatedType[] bounds = a.getAnnotatedBounds();
    switch (bounds.length) {
    case 0:
      break;
    case 1:
      t.setUpperBound(this.type(bounds[0], cl));
      break;
    default:
      final org.microbean.lang.type.IntersectionType upperBound = new org.microbean.lang.type.IntersectionType();
      for (final AnnotatedType bound : bounds) {
        upperBound.addBound(this.type(bound, cl));
      }
      t.setUpperBound(upperBound);
      break;
    }
    t.addAnnotationMirrors(annotations(a, cl));
  }

  private final void build(final ClassLoader cl,
                           final AnnotatedWildcardType a,
                           final org.microbean.lang.type.WildcardType t)
    throws ReflectiveOperationException {
    final AnnotatedType[] superBounds = a.getAnnotatedLowerBounds();
    switch (superBounds.length) {
    case 0:
      break;
    case 1:
      t.setSuperBound(type(superBounds[0], cl));
      break;
    default:
      throw new IllegalArgumentException("a: " + a);
    }
    final AnnotatedType[] extendsBounds = a.getAnnotatedUpperBounds();
    switch (extendsBounds.length) {
    case 0:
      break;
    case 1:
      t.setExtendsBound(type(extendsBounds[0], cl));
      break;
    default:
      throw new IllegalArgumentException("a: " + a);
    }
    t.addAnnotationMirrors(annotations(a, cl));
  }


  /*
   * Annotations.
   */


  private final void build(final ClassLoader cl,
                           final Annotation a,
                           final org.microbean.lang.element.AnnotationMirror am) throws ReflectiveOperationException {
    final Class<?> at = a.annotationType();
    am.setAnnotationType((DeclaredType)type(at, cl));
    final Map<ExecutableElement, AnnotationValue> values = new HashMap<>();
    for (final Method ae : at.getDeclaredMethods()) {
        assert ae.getParameterCount() == 0;
        assert ae.getReturnType() != void.class;
        assert !"toString".equals(ae.getName());
        assert !"hashCode()".equals(ae.getName());
        values.put(DelegatingElement.of(element(ae, cl), EQUALITY_NO_ANNOTATIONS),
                   annotationValue(ae.invoke(a), cl));
      }
  }


  /*
   * Housekeeping.
   */


  private final <K, E extends Element> Element element(final ClassLoader cl,
                                                       final K k,
                                                       final Supplier<? extends E> s,
                                                       final Builder<? super K, ? super E> c)
    throws ReflectiveOperationException {
    final E e = s.get();
    Element r = this.elements.putIfAbsent(k, e);
    if (r == null) {
      // e was put in the map
      c.build(cl, k, e);
      r = e;
    }
    return r;
  }

  private final <K, T extends TypeMirror> TypeMirror type(final ClassLoader cl,
                                                          final K k,
                                                          final Supplier<? extends T> s,
                                                          final Builder<? super K, ? super T> c)
    throws ReflectiveOperationException {
    final T t = s.get();
    TypeMirror r = this.typeMirrors.putIfAbsent(k, t);
    if (r == null) {
      // t was put in the map
      c.build(cl, k, t);
      r = t;
    }
    return r;
  }


  /*
   * Static methods.
   */


  private static final ElementKind kind(final Class<?> c) {
    if (c.isAnnotation()) {
      return ElementKind.ANNOTATION_TYPE;
    } else if (c.isInterface()) {
      return ElementKind.INTERFACE;
    } else if (c.isEnum()) {
      return ElementKind.ENUM;
    } else if (c.isRecord()) {
      return ElementKind.RECORD;
    } else {
      return ElementKind.CLASS;
    }
  }

  private static final ElementKind kind(final Constructor<?> c) {
    return ElementKind.CONSTRUCTOR;
  }

  private static final ElementKind kind(final Method m) {
    return m.getName().equals("<clinit>") ? ElementKind.STATIC_INIT : ElementKind.METHOD;
  }

  private static final NestingKind nestingKind(final Class<?> c) {
    if (c.isAnonymousClass()) {
      return NestingKind.ANONYMOUS;
    } else if (c.isLocalClass()) {
      return NestingKind.LOCAL;
    } else if (c.isMemberClass()) {
      return NestingKind.MEMBER;
    } else {
      return NestingKind.TOP_LEVEL;
    }
  }

  private static final TypeKind typeKindForPrimitive(final Class<?> c) {
    if (c == boolean.class) {
      return TypeKind.BOOLEAN;
    } else if (c == byte.class) {
      return TypeKind.BYTE;
    } else if (c == char.class) {
      return TypeKind.CHAR;
    } else if (c == double.class) {
      return TypeKind.DOUBLE;
    } else if (c == float.class) {
      return TypeKind.FLOAT;
    } else if (c == int.class) {
      return TypeKind.INT;
    } else if (c == long.class) {
      return TypeKind.LONG;
    } else if (c == short.class) {
      return TypeKind.SHORT;
    } else {
      throw new IllegalArgumentException("c: " + c);
    }
  }



  /*
   * Inner and nested classes.
   */


  @FunctionalInterface
  private static interface Builder<K, T> {

    public void build(final ClassLoader cl, final K k, final T t) throws ReflectiveOperationException;

  }

  private static final class UncheckedReflectiveOperationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private UncheckedReflectiveOperationException(final ReflectiveOperationException e) {
      super(e.getMessage(), e);
    }

    @Override
    public final ReflectiveOperationException getCause() {
      return (ReflectiveOperationException)super.getCause();
    }

  }

}
