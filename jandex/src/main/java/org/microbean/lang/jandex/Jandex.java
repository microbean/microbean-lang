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
package org.microbean.lang.jandex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import java.util.function.BiFunction;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassInfo.EnclosingMethodInfo;
import org.jboss.jandex.ClassInfo.NestingType;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.EmptyTypeTarget;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.RecordComponentInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeParameterTypeTarget;
import org.jboss.jandex.TypeTarget;
import org.jboss.jandex.TypeVariableReference;
import org.jboss.jandex.VoidType;

import org.microbean.lang.Modeler;

public final class Jandex extends Modeler {

  private static final Type[] EMPTY_TYPE_ARRAY = new Type[0];

  private final IndexView i;

  private final BiFunction<? super String, ? super IndexView, ? extends ClassInfo> unindexedClassnameFunction;

  public Jandex(final IndexView i) {
    this(i, (n, j) -> { throw new IllegalArgumentException("class " + n + " not found in IndexView " + j); });
  }

  public Jandex(final IndexView i, final BiFunction<? super String, ? super IndexView, ? extends ClassInfo> unindexedClassnameFunction) {
    super();
    this.i = Objects.requireNonNull(i, "i");
    this.unindexedClassnameFunction = Objects.requireNonNull(unindexedClassnameFunction, "unindexedClassnameFunction");
  }

  public final AnnotationMirror annotation(final AnnotationInstance k) {
    return this.annotation(k, org.microbean.lang.element.AnnotationMirror::new, this::build);
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final Object v) {
    return switch (v) {
    case AnnotationInstance a -> this.annotationValue(a);
    case AnnotationMirror a -> this.annotationValue(a);
    case Boolean b -> this.annotationValue(b);
    case Byte b -> this.annotationValue(b);
    case CharSequence s -> this.annotationValue(s);
    case Character c -> this.annotationValue(c);
    case Collection<?> c -> this.annotationValue(c);
    case Double d -> this.annotationValue(d);
    case FieldInfo f -> this.annotationValue(f);
    case Float f -> this.annotationValue(f);
    case Integer i -> this.annotationValue(i);
    case Long l -> this.annotationValue(l);
    case Object[] o -> this.annotationValue(o);
    case Short s -> this.annotationValue(s);
    case TypeMirror t -> this.annotationValue(t);
    case VariableElement ve -> this.annotationValue(ve);
    case boolean[] o -> this.annotationValue(o);
    case byte[] o -> this.annotationValue(o);
    case char[] o -> this.annotationValue(o);
    case double[] o -> this.annotationValue(o);
    case float[] o -> this.annotationValue(o);
    case int[] o -> this.annotationValue(o);
    case javax.lang.model.element.AnnotationValue a -> this.annotationValue(a);
    case long[] o -> this.annotationValue(o);
    case null -> null;
    case org.jboss.jandex.AnnotationValue a -> this.annotationValue(a);
    case short[] o -> this.annotationValue(o);
    default -> throw new IllegalArgumentException("v: " + v);
    };
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final AnnotationMirror a) {
    return a == null ? null : new org.microbean.lang.element.AnnotationValue(a);
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final AnnotationInstance a) {
    return a == null ? null : this.annotationValue(this.annotation(a));
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final javax.lang.model.element.AnnotationValue v) {
    return v;
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final org.jboss.jandex.AnnotationValue v) {
    return v == null ? null : switch (v.kind()) {
    case ARRAY, BOOLEAN, BYTE, CHARACTER, CLASS, DOUBLE, FLOAT, INTEGER, LONG, NESTED, SHORT, STRING -> this.annotationValue(v.value());
    case ENUM -> this.annotationValue(this.classInfoFor(v.asEnumType()).field(v.asEnum()));
    case UNKNOWN -> new org.microbean.lang.element.AnnotationValue(List.of());
    };
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final Boolean b) {
    return b == null ? null : new org.microbean.lang.element.AnnotationValue(b);
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final Byte b) {
    return b == null ? null : new org.microbean.lang.element.AnnotationValue(b);
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final Character c) {
    return c == null ? null : new org.microbean.lang.element.AnnotationValue(c);
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final CharSequence s) {
    return new org.microbean.lang.element.AnnotationValue(s == null ? "" : s.toString());
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final Collection<?> c) {
    return new org.microbean.lang.element.AnnotationValue(this.annotationValues(c == null ? List.of() : c));
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final Double d) {
    return d == null ? null : new org.microbean.lang.element.AnnotationValue(d);
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final FieldInfo enumConstant) {
    if (enumConstant == null) {
      return null;
    } else if (enumConstant.isEnumConstant() && enumConstant.declaringClass().isEnum()) {
      return new org.microbean.lang.element.AnnotationValue(this.element(enumConstant));
    }
    throw new IllegalArgumentException("enumConstant: " + enumConstant);
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final Float f) {
    return f == null ? null : new org.microbean.lang.element.AnnotationValue(f);
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final Integer i) {
    return i == null ? null : new org.microbean.lang.element.AnnotationValue(i);
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final Long l) {
    return l == null ? null : new org.microbean.lang.element.AnnotationValue(l);
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final Object[] o) {
    return new org.microbean.lang.element.AnnotationValue(this.annotationValues(o));
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final Short s) {
    return s == null ? null : new org.microbean.lang.element.AnnotationValue(s);
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final TypeMirror t) {
    return t == null ? null : new org.microbean.lang.element.AnnotationValue(t);
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final VariableElement enumConstant) {
    if (enumConstant == null) {
      return null;
    } else if (enumConstant.getKind() == ElementKind.ENUM_CONSTANT) {
      return new org.microbean.lang.element.AnnotationValue(enumConstant);
    }
    throw new IllegalArgumentException("enumConstant: " + enumConstant);
  }

  public final List<? extends javax.lang.model.element.AnnotationValue> annotationValues(final Collection<?> c) {
    if (c == null || c.isEmpty()) {
      return List.of();
    }
    final List<javax.lang.model.element.AnnotationValue> list = new ArrayList<>(c.size());
    for (final Object value : c) {
      list.add(this.annotationValue(value));
    }
    return Collections.unmodifiableList(list);
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final boolean[] o) {
    return o == null ? null : this.annotationValue(this.annotationValues(o));
  }

  public final List<? extends javax.lang.model.element.AnnotationValue> annotationValues(final boolean[] o) {
    if (o == null || o.length <= 0) {
      return List.of();
    }
    final List<javax.lang.model.element.AnnotationValue> list = new ArrayList<>(o.length);
    for (final boolean value : o) {
      list.add(this.annotationValue(value));
    }
    return Collections.unmodifiableList(list);
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final byte[] o) {
    return o == null ? null : this.annotationValue(this.annotationValues(o));
  }

  public final List<? extends javax.lang.model.element.AnnotationValue> annotationValues(final byte[] o) {
    if (o == null || o.length <= 0) {
      return List.of();
    }
    final List<javax.lang.model.element.AnnotationValue> list = new ArrayList<>(o.length);
    for (final byte value : o) {
      list.add(this.annotationValue(value));
    }
    return Collections.unmodifiableList(list);
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final char[] o) {
    return o == null ? null : this.annotationValue(this.annotationValues(o));
  }

  public final List<? extends javax.lang.model.element.AnnotationValue> annotationValues(final char[] o) {
    if (o == null || o.length <= 0) {
      return List.of();
    }
    final List<javax.lang.model.element.AnnotationValue> list = new ArrayList<>(o.length);
    for (final char value : o) {
      list.add(this.annotationValue(value));
    }
    return Collections.unmodifiableList(list);
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final double[] o) {
    return o == null ? null : this.annotationValue(this.annotationValues(o));
  }

  public final List<? extends javax.lang.model.element.AnnotationValue> annotationValues(final double[] o) {
    if (o == null || o.length <= 0) {
      return List.of();
    }
    final List<javax.lang.model.element.AnnotationValue> list = new ArrayList<>(o.length);
    for (final double value : o) {
      list.add(this.annotationValue(value));
    }
    return Collections.unmodifiableList(list);
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final float[] o) {
    return o == null ? null : this.annotationValue(this.annotationValues(o));
  }

  public final List<? extends javax.lang.model.element.AnnotationValue> annotationValues(final float[] o) {
    if (o == null || o.length <= 0) {
      return List.of();
    }
    final List<javax.lang.model.element.AnnotationValue> list = new ArrayList<>(o.length);
    for (final float value : o) {
      list.add(this.annotationValue(value));
    }
    return Collections.unmodifiableList(list);
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final int[] o) {
    return o == null ? null : this.annotationValue(this.annotationValues(o));
  }

  public final List<? extends javax.lang.model.element.AnnotationValue> annotationValues(final int[] o) {
    if (o == null || o.length <= 0) {
      return List.of();
    }
    final List<javax.lang.model.element.AnnotationValue> list = new ArrayList<>(o.length);
    for (final int value : o) {
      list.add(this.annotationValue(value));
    }
    return Collections.unmodifiableList(list);
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final long[] o) {
    return o == null ? null : this.annotationValue(this.annotationValues(o));
  }

  public final List<? extends javax.lang.model.element.AnnotationValue> annotationValues(final long[] o) {
    if (o == null || o.length <= 0) {
      return List.of();
    }
    final List<javax.lang.model.element.AnnotationValue> list = new ArrayList<>(o.length);
    for (final long value : o) {
      list.add(this.annotationValue(value));
    }
    return Collections.unmodifiableList(list);
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final short[] o) {
    return o == null ? null : this.annotationValue(this.annotationValues(o));
  }

  public final List<? extends javax.lang.model.element.AnnotationValue> annotationValues(final short[] o) {
    if (o == null || o.length <= 0) {
      return List.of();
    }
    final List<javax.lang.model.element.AnnotationValue> list = new ArrayList<>(o.length);
    for (final short value : o) {
      list.add(this.annotationValue(value));
    }
    return Collections.unmodifiableList(list);
  }

  public final List<? extends javax.lang.model.element.AnnotationValue> annotationValues(final Object[] o) {
    if (o == null || o.length <= 0) {
      return List.of();
    }
    final List<javax.lang.model.element.AnnotationValue> list = new ArrayList<>(o.length);
    for (final Object value : o) {
      list.add(this.annotationValue(value));
    }
    return Collections.unmodifiableList(list);
  }

  public final TypeElement element(final DotName n) {
    final ClassInfo ci = this.classInfoFor(n);
    return ci == null ? null : this.element(ci);
  }

  public final TypeElement element(final String n) {
    final ClassInfo ci = this.classInfoFor(n);
    return ci == null ? null : this.element(ci);
  }

  public final TypeElement element(final AnnotationInstance ai) {
    return ai == null ? null : this.element(this.classInfoFor(ai.name()));
  }

  public final TypeElement element(final ClassInfo ci) {
    if (ci == null) {
      return null;
    } else if (ci.isModule()) {
      throw new UnsupportedOperationException("TODO: implement");
    } else {
      return this.element(ci, () -> new org.microbean.lang.element.TypeElement(kind(ci), nestingKind(ci)), this::build);
    }
  }

  public final TypeElement element(final ClassType ct) {
    return ct == null ? null : this.element(this.classInfoFor(ct));
  }

  public final <E extends Element> E element(final E e) {
    return e;
  }

  public final ExecutableElement element(final EnclosingMethodInfo emi) {
    return emi == null ? null : this.element(this.classInfoFor(emi.enclosingClass()).method(emi.name(), emi.parameters().toArray(EMPTY_TYPE_ARRAY)));
  }

  public final VariableElement element(final FieldInfo fi) {
    return fi == null ? null : this.element(fi, () -> new org.microbean.lang.element.VariableElement(kind(fi)), this::build);
  }

  public final ExecutableElement element(final MethodInfo mi) {
    return mi == null ? null : this.element(mi, () -> new org.microbean.lang.element.ExecutableElement(kind(mi)), this::build);
  }

  public final VariableElement element(final MethodParameterInfo mpi) {
    return mpi == null ? null : this.element(mpi, () -> new org.microbean.lang.element.VariableElement(ElementKind.PARAMETER), this::build);
  }

  public final RecordComponentElement element(final RecordComponentInfo rci) {
    return rci == null ? null : this.element(rci, org.microbean.lang.element.RecordComponentElement::new, this::build);
  }

  public final TypeParameterElement element(final TypeParameterInfo tpi) {
    return tpi == null ? null : this.element(tpi, org.microbean.lang.element.TypeParameterElement::new, this::build);
  }

  public final TypeParameterElement element(final TypeParameterTypeTarget tt) {
    return tt == null ? null : this.element(new TypeParameterInfo(tt.enclosingTarget(), tt.target().asTypeVariable()));
  }

  public final DeclaredType type(final AnnotationInstance ai) {
    return ai == null ? null : this.type(this.classInfoFor(ai));
  }

  public final DeclaredType type(final ClassInfo ci) {
    if (ci == null) {
      return null;
    } else if (ci.isModule()) {
      throw new UnsupportedOperationException("Not yet handled");
    } else {
      return this.type(ci, org.microbean.lang.type.DeclaredType::new, this::build);
    }
  }

  public final DeclaredType type(final ClassType ct) {
    return ct == null ? null : this.type(this.classInfoFor(ct));
  }

  public final TypeMirror type(final FieldInfo fi) {
    return fi == null ? null : this.type(new TypeContext<>(fi, fi.type()));
  }

  public final ExecutableType type(final MethodInfo mi) {
    return mi == null ? null : this.type(mi, org.microbean.lang.type.ExecutableType::new, this::build);
  }

  public final javax.lang.model.type.PrimitiveType type(final org.jboss.jandex.PrimitiveType t) {
    return t == null ? null : this.type(t, () -> new org.microbean.lang.type.PrimitiveType(kind(t)), this::build);
  }

  public final DeclaredType type(final RecordComponentInfo rci) {
    return rci == null ? null : (DeclaredType)this.type(new TypeContext<>(rci, rci.type()));
  }

  public final NoType type(final VoidType vt) {
    return vt == null ? null : org.microbean.lang.type.NoType.VOID;
  }

  @SuppressWarnings("unchecked")
  public final <T extends Type> TypeMirror type(final TypeContext<T> k) {
    if (k == null) {
      return null;
    }
    final T type = k.type();
    if (type == null) {
      return null;
    }
    switch (type.kind()) {
    case ARRAY:
      return this.type((TypeContext<ArrayType>)k, org.microbean.lang.type.ArrayType::new, this::build);
    case CLASS:
      return this.type(type.asClassType());
    case PARAMETERIZED_TYPE:
      return this.type((TypeContext<ParameterizedType>)k, org.microbean.lang.type.DeclaredType::new, this::build);
    case PRIMITIVE:
      return this.type(k.type().asPrimitiveType(), () -> new org.microbean.lang.type.PrimitiveType(kind(k.type().asPrimitiveType())), this::build);
    case TYPE_VARIABLE:
      return this.type(this.typeParameterInfoFor((TypeContext<org.jboss.jandex.TypeVariable>)k));
    case TYPE_VARIABLE_REFERENCE:
      return this.type(new TypeContext<>(k.context(), type.asTypeVariableReference().follow()));
    case UNRESOLVED_TYPE_VARIABLE:
      throw new AssertionError();
    case VOID:
      return org.microbean.lang.type.NoType.VOID;
    case WILDCARD_TYPE:
      return this.type((TypeContext<org.jboss.jandex.WildcardType>)k, org.microbean.lang.type.WildcardType::new, this::build);
    default:
      throw new AssertionError();
    }
  }

  public final <T extends TypeMirror> T type(final T t) {
    return t;
  }

  public final javax.lang.model.type.TypeVariable type(final TypeParameterInfo tpi) {
    return tpi == null ? null : (org.microbean.lang.type.TypeVariable)this.type(tpi, org.microbean.lang.type.TypeVariable::new, this::build);
  }


  /*
   * Annotation builders.
   */


  private final void build(final AnnotationInstance ai, final org.microbean.lang.element.AnnotationMirror am) {
    final ClassInfo ci = this.classInfoFor(ai.name());

    final org.microbean.lang.type.DeclaredType t = (org.microbean.lang.type.DeclaredType)this.type(ci);
    assert t.asElement() != null;
    am.setAnnotationType(t);

    for (final org.jboss.jandex.AnnotationValue v : ai.values()) {
      final javax.lang.model.element.ExecutableElement ee = this.element(this.annotationElementFor(ci, v.name()));
      assert ee.getEnclosingElement() != null : "ee: " + ee + "; ai: " + ai + "; ci: " + ci + "; v: " + v;
      am.putElementValue(ee, this.annotationValue(v));
    }
  }


  /*
   * Element builders.
   */


  private final void build(final ClassInfo ci, final org.microbean.lang.element.TypeElement e) {
    // Simple name.
    e.setSimpleName(ci.name().local());

    // Enclosing element.
    final Element enclosingElement;
    switch (e.getNestingKind()) {
    case ANONYMOUS:
    case LOCAL:
      // Anonymous and local classes are effectively ignored in the javax.lang.model.* hierarchy. The documentation for
      // javax.lang.model.element.Element#getEnclosedElements() says, in part: "A class or interface is considered to
      // enclose the fields, methods, constructors, record components, and member classes and interfaces that it
      // directly declares. A package encloses the top-level classes and interfaces within it, but is not considered to
      // enclose subpackages. A module encloses packages within it. Enclosed elements may include implicitly declared
      // mandated elements. Other kinds of elements are not currently considered to enclose any elements; however, that
      // may change as this API or the programming language evolves."
      //
      // Additionally, Jandex provides no access to local or anonymous classes at all.
      enclosingElement = null;
      break;
    case MEMBER:
      enclosingElement = this.element(this.classInfoFor(ci.enclosingClass()));
      break;
    case TOP_LEVEL:
      enclosingElement = null; // TODO: need to do package, but Jandex doesn't do it
      break;
    default:
      throw new AssertionError();
    }
    e.setEnclosingElement(enclosingElement);

    // Type.
    final org.microbean.lang.type.DeclaredType t = (org.microbean.lang.type.DeclaredType)this.type(ci);
    e.setType(t);

    // Defining element.
    t.setDefiningElement(e);

    for (final RecordComponentInfo rc : ci.unsortedRecordComponents()) {
      e.addEnclosedElement((org.microbean.lang.element.RecordComponentElement)this.element(rc));
    }

    // Modifiers.
    final short modifiers = ci.flags();
    if (java.lang.reflect.Modifier.isAbstract(modifiers)) {
      e.addModifier(javax.lang.model.element.Modifier.ABSTRACT);
    } else if (java.lang.reflect.Modifier.isFinal(modifiers)) {
      e.addModifier(javax.lang.model.element.Modifier.FINAL);
    }
    if (java.lang.reflect.Modifier.isPrivate(modifiers)) {
      e.addModifier(javax.lang.model.element.Modifier.PRIVATE);
    } else if (java.lang.reflect.Modifier.isProtected(modifiers)) {
      e.addModifier(javax.lang.model.element.Modifier.PROTECTED);
    } else if (java.lang.reflect.Modifier.isPublic(modifiers)) {
      e.addModifier(javax.lang.model.element.Modifier.PUBLIC);
    }
    // TODO: no way to tell if a ClassInfo is sealed. See https://github.com/smallrye/jandex/issues/167.
    if (java.lang.reflect.Modifier.isStatic(modifiers)) {
      e.addModifier(javax.lang.model.element.Modifier.STATIC);
    }

    // Supertypes.
    final Type superclassType = ci.superClassType();
    if (superclassType != null) {
      assert superclassType.kind() == Type.Kind.CLASS || superclassType.kind() == Type.Kind.PARAMETERIZED_TYPE;
      e.setSuperclass(this.type(new TypeContext<>(ci, superclassType)));
    }
    for (final Type iface : ci.interfaceTypes()) {
      e.addInterface(this.type(new TypeContext<>(ci, iface)));
    }

    // Type parameters.
    for (final org.jboss.jandex.TypeVariable tp : ci.typeParameters()) {
      e.addTypeParameter((org.microbean.lang.element.TypeParameterElement)this.element(new TypeParameterInfo(ci, tp)));
    }

    // TODO: enclosed elements

    // TODO: annotations.
    for (final AnnotationInstance a : ci.annotations()) {
      final AnnotationTarget annotationTarget = a.target();
      switch (annotationTarget.kind()) {
      case CLASS:
        assert annotationTarget.asClass().nestingType() == NestingType.TOP_LEVEL;
        e.addAnnotationMirror(this.annotation(a));
        break;
      case FIELD:
        ((org.microbean.lang.element.VariableElement)this.element(annotationTarget.asField())).addAnnotationMirror(this.annotation(a));
        break;
      case METHOD:
        ((org.microbean.lang.element.ExecutableElement)this.element(annotationTarget.asMethod())).addAnnotationMirror(this.annotation(a));
        break;
      case METHOD_PARAMETER:
        ((org.microbean.lang.element.VariableElement)this.element(annotationTarget.asMethodParameter())).addAnnotationMirror(this.annotation(a));
        break;
      case RECORD_COMPONENT:
        ((org.microbean.lang.element.RecordComponentElement)this.element(annotationTarget.asRecordComponent())).addAnnotationMirror(this.annotation(a));
        break;
      case TYPE:
        // This is where things get stupid.
        final TypeTarget tt = annotationTarget.asType();
        assert tt.enclosingTarget() == annotationTarget;
        switch (tt.usage()) {
        case CLASS_EXTENDS:
          // Genuine type usage. Maybe go ahead and apply it here.
          System.out.println("*** tt: " + tt);
          ((org.microbean.lang.type.DeclaredType)this.type(tt.asClass())).addAnnotationMirror(this.annotation(a));
          break;
        case EMPTY:
          // Genuine type usage on a field type, a method return type, or a method receiver type. (What about a thrown
          // type?) Maybe go ahead and apply it here.
          final EmptyTypeTarget ett = tt.asEmpty();
          
          break;
        case METHOD_PARAMETER:
          // Genuine type usage WITHIN the type of a method parameter, e.g. blatz(Foo<@Bar String> baz). Maybe go ahead
          // and apply it here.
          break;
        case THROWS:
          // Genuine type usage. Maybe go ahead and apply it here.
          break;
        case TYPE_PARAMETER:
          // Stupid. Actually an *element* annotation. Definitely apply it here.
          ((org.microbean.lang.element.TypeParameterElement)this.element(tt.asTypeParameter())).addAnnotationMirror(this.annotation(a));
          break;
        case TYPE_PARAMETER_BOUND:
          // Genuine type usage WITHIN the type underlying a type parameter element. Maybe go ahead and apply it here.
          break;
        default:
          throw new AssertionError();
        }
        break;
      default:
        throw new AssertionError();
      }
    }
  }

  private final void build(final FieldInfo fi, final org.microbean.lang.element.VariableElement e) {
    e.setSimpleName(fi.name());
    e.setEnclosingElement(this.element(fi.declaringClass()));
    e.setType(this.type(fi));

    // We don't do annotations here because they are handled "up" at the class level (Jandex is first and foremost an
    // annotation index, so annotation information about all members is available "up there".  See #build(ClassInfo,
    // TypeElement).
  }

  private final void build(final MethodInfo mi, final org.microbean.lang.element.ExecutableElement e) {
    // Simple name.
    if (!mi.isConstructor()) {
      e.setSimpleName(mi.name());
    }

    // Enclosing element.
    final TypeElement ee = this.element(mi.declaringClass());
    assert ee != null;
    e.setEnclosingElement(ee);
    assert e.getEnclosingElement() == ee : "e: " + e + "; ee: " + ee;

    // Type.
    e.setType(this.type(mi));

    // Modifiers.
    final short modifiers = mi.flags();
    if (isDefault(mi)) {
      e.setDefault(true);
      e.addModifier(javax.lang.model.element.Modifier.DEFAULT);
    } else {
      e.setDefault(false);
      if (java.lang.reflect.Modifier.isAbstract(modifiers)) {
        e.addModifier(javax.lang.model.element.Modifier.ABSTRACT);
      } else if (java.lang.reflect.Modifier.isFinal(modifiers)) {
        e.addModifier(javax.lang.model.element.Modifier.FINAL);
      } else if (java.lang.reflect.Modifier.isNative(modifiers)) {
        e.addModifier(javax.lang.model.element.Modifier.NATIVE);
      }
      if (java.lang.reflect.Modifier.isStatic(modifiers)) {
        e.addModifier(javax.lang.model.element.Modifier.STATIC);
      }
      if (java.lang.reflect.Modifier.isSynchronized(modifiers)) {
        e.addModifier(javax.lang.model.element.Modifier.SYNCHRONIZED);
      }
    }
    if (java.lang.reflect.Modifier.isPrivate(modifiers)) {
      e.addModifier(javax.lang.model.element.Modifier.PRIVATE);
    } else if (java.lang.reflect.Modifier.isProtected(modifiers)) {
      e.addModifier(javax.lang.model.element.Modifier.PROTECTED);
    } else if (java.lang.reflect.Modifier.isPublic(modifiers)) {
      e.addModifier(javax.lang.model.element.Modifier.PUBLIC);
    }

    // Type parameters.
    for (final org.jboss.jandex.TypeVariable tp : mi.typeParameters()) {
      e.addTypeParameter((org.microbean.lang.element.TypeParameterElement)this.element(new TypeParameterInfo(mi, tp)));
    }

    // Parameters.
    for (final MethodParameterInfo p : mi.parameters()) {
      e.addParameter((org.microbean.lang.element.VariableElement)this.element(p));
    }

    // We don't do annotations here because they are handled "up" at the class level (Jandex is first and foremost an
    // annotation index, so annotation information about all members is available "up there".  See #build(ClassInfo,
    // TypeElement).
  }

  private final void build(final MethodParameterInfo mpi, final org.microbean.lang.element.VariableElement e) {
    // Simple name.
    String n = mpi.name();
    if (n == null) {
      n = "arg" + mpi.position();
    }
    e.setSimpleName(n);

    // (No enclosing element.)
    // e.setEnclosingElement(this.element(mpi.method())); // interestingly not supported by the javax.lang.model.* api
    
    // Type.
    e.setType(this.type(new TypeContext<>(mpi, mpi.type())));

    // We don't do annotations here because they are handled "up" at the class level (Jandex is first and foremost an
    // annotation index, so annotation information about all members is available "up there".  See #build(ClassInfo,
    // TypeElement).
  }

  private final void build(final RecordComponentInfo r, final org.microbean.lang.element.RecordComponentElement e) {
    // Simple name.
    e.setSimpleName(r.name());

    // Enclosing element.
    e.setEnclosingElement(this.element(r));

    // Type.
    e.setType(this.type(r));
    
    e.setAccessor((org.microbean.lang.element.ExecutableElement)this.element(r.declaringClass().method(r.name())));

    // We don't do annotations here because they are handled "up" at the class level (Jandex is first and foremost an
    // annotation index, so annotation information about all members is available "up there".  See #build(ClassInfo,
    // TypeElement).
  }

  private final void build(final TypeParameterInfo tpi, final org.microbean.lang.element.TypeParameterElement e) {
    // Simple name.
    e.setSimpleName(tpi.identifier());

    // Enclosing element.
    switch (tpi.kind()) {
    case CLASS:
      e.setEnclosingElement(this.element(tpi.annotationTarget().asClass()));
      break;
    case METHOD:
      e.setEnclosingElement(this.element(tpi.annotationTarget().asMethod()));
      break;
    default:
      throw new AssertionError();
    }

    // Type.
    final org.microbean.lang.type.TypeVariable t = (org.microbean.lang.type.TypeVariable)this.type(tpi);
    e.setType(t);

    // Defining element.
    t.setDefiningElement(e);

    // We don't do annotations here because they are handled "up" at the class level (Jandex is first and foremost an
    // annotation index, so annotation information about all members is available "up there".  See #build(ClassInfo,
    // TypeElement).
  }


  /*
   * Type builders.
   */


  private final void build(final TypeContext<ArrayType> tc, final org.microbean.lang.type.ArrayType t) {
    t.setComponentType(this.type(new TypeContext<>(tc.context(), tc.type().component())));
  }

  private final void build(final ClassInfo ci, final org.microbean.lang.type.DeclaredType t) {
    final org.microbean.lang.element.TypeElement e = (org.microbean.lang.element.TypeElement)this.element(ci);

    // Type.
    e.setType(t);

    // Defining element.
    t.setDefiningElement(e);

    // Need to do enclosing type, if there is one
    final ClassInfo enclosingClass = this.classInfoEnclosing(ci);
    if (enclosingClass != null) {
      t.setEnclosingType(this.type(enclosingClass));
    }

    // Now type arguments (which will be type variables), if there are any.
    for (final org.jboss.jandex.TypeVariable tp : ci.typeParameters()) {
      t.addTypeArgument(this.type(new TypeContext<>(ci, tp)));
    }

    // We don't do annotations here because they are handled "up" at the class level (Jandex is first and foremost an
    // annotation index, so annotation information about all members is available "up there".  See #build(ClassInfo,
    // TypeElement).
  }

  private final void build(final MethodInfo mi, final org.microbean.lang.type.ExecutableType t) {
    for (final Type pt : mi.parameterTypes()) {
      t.addParameterType(this.type(new TypeContext<>(mi, pt)));
    }
    t.setReceiverType(this.type(new TypeContext<>(mi, mi.receiverType())));
    if (mi.isConstructor()) {

    } else {
      t.setReturnType(this.type(new TypeContext<>(mi, mi.returnType())));
    }
    for (final Type et : mi.exceptions()) {
      t.addThrownType(this.type(new TypeContext<>(mi, et)));
    }
    for (final org.jboss.jandex.TypeVariable tv : mi.typeParameters()) {
      t.addTypeVariable((org.microbean.lang.type.TypeVariable)this.type(new TypeContext<>(mi, tv)));
    }

    // We don't do annotations here because they are handled "up" at the class level (Jandex is first and foremost an
    // annotation index, so annotation information about all members is available "up there".  See #build(ClassInfo,
    // TypeElement).
  }

  private final void build(final TypeContext<ParameterizedType> tc, final org.microbean.lang.type.DeclaredType t) {
    final ParameterizedType p = tc.type();
    final ClassInfo ci = this.classInfoFor(p);

    // ci represents the element.
    t.setDefiningElement(this.element(ci));

    // Will be either null, a ClassType, or a ParameterizedType.
    final Type ownerType = p.owner();
    t.setEnclosingType(this.type(ownerType == null ? this.classInfoFor(ci.enclosingClass()) : this.classInfoFor(ownerType)));

    for (final Type arg : p.arguments()) {
      t.addTypeArgument(this.type(new TypeContext<>(tc.context(), arg)));
    }

    // We don't do annotations here because they are handled "up" at the class level (Jandex is first and foremost an
    // annotation index, so annotation information about all members is available "up there".  See #build(ClassInfo,
    // TypeElement).
  }

  private final void build(final PrimitiveType p, final org.microbean.lang.type.PrimitiveType t) {
    // We don't do annotations here because they are handled "up" at the class level (Jandex is first and foremost an
    // annotation index, so annotation information about all members is available "up there".  See #build(ClassInfo,
    // TypeElement).
  }

  private final void build(final TypeParameterInfo tpi, final org.microbean.lang.type.TypeVariable t) {
    final org.microbean.lang.element.TypeParameterElement e = (org.microbean.lang.element.TypeParameterElement)this.element(tpi);

    // Type.
    e.setType(t);

    // Defining element.
    t.setDefiningElement(e);

    final AnnotationTarget context = tpi.annotationTarget();
    final List<? extends Type> bounds = tpi.typeVariable().bounds();
    switch (bounds.size()) {
    case 0:
      break;
    case 1:
      t.setUpperBound(this.type(new TypeContext<>(context, bounds.get(0))));
      break;
    default:
      final org.microbean.lang.type.IntersectionType upperBound = new org.microbean.lang.type.IntersectionType();
      for (final Type bound : bounds) {
        upperBound.addBound(this.type(new TypeContext<>(context, bound)));
      }
      t.setUpperBound(upperBound);
      break;
    }

  }

  private final void build(final TypeContext<org.jboss.jandex.WildcardType> tc, final org.microbean.lang.type.WildcardType w) {
    final org.jboss.jandex.WildcardType tcType = tc.type();
    Type bound = tcType.extendsBound();
    if (bound != null) {
      w.setExtendsBound(this.type(new TypeContext<>(tc.context(), bound)));
    }
    bound = tcType.superBound();
    if (bound != null) {
      w.setSuperBound(this.type(new TypeContext<>(tc.context(), bound)));
    }
  }


  /*
   * Housekeeping.
   */


  private final MethodInfo annotationElementFor(final ClassInfo ci, final String name) {
    if (ci.isAnnotation()) {
      return ci.method(name);
    }
    throw new IllegalArgumentException("ci: " + ci);
  }


  private final ClassInfo classInfoFor(final AnnotationInstance ai) {
    return ai == null ? null : this.classInfoFor(ai.name());
  }

  private final ClassInfo classInfoFor(final Type t) {
    return t == null ? null : switch(t.kind()) {
    case CLASS -> this.classInfoFor(t.asClassType());
    case PARAMETERIZED_TYPE -> this.classInfoFor(t.asParameterizedType());
    default -> null;
    };
  }

  private final ClassInfo classInfoFor(final ClassType t) {
    return t == null ? null : this.classInfoFor(t.name());
  }

  private final ClassInfo classInfoFor(final ParameterizedType t) {
    return t == null ? null : this.classInfoFor(t.name());
  }

  private final ClassInfo classInfoFor(final DotName n) {
    if (n == null) {
      return null;
    }
    final ClassInfo ci = this.i.getClassByName(n);
    return ci == null ? this.unindexedClassnameFunction.apply(n.toString(), this.i) : ci;
  }

  private final ClassInfo classInfoFor(final String n) {
    if (n == null) {
      return null;
    }
    final ClassInfo ci = this.i.getClassByName(n);
    return ci == null ? this.unindexedClassnameFunction.apply(n, this.i) : ci;
  }

  private final ClassInfo classInfoEnclosing(final ClassInfo ci) {
    return ci == null ? null : this.classInfoFor(ci.enclosingClass());
  }

  final TypeParameterInfo typeParameterInfoFor(final AnnotationTarget context, final org.jboss.jandex.TypeVariable tv) {
    return switch (context.kind()) {
    case CLASS -> this.typeParameterInfoFor(context.asClass(), tv);
    case FIELD -> this.typeParameterInfoFor(context.asField(), tv);
    case METHOD -> this.typeParameterInfoFor(context.asMethod(), tv);
    case METHOD_PARAMETER -> this.typeParameterInfoFor(context.asMethodParameter(), tv);
    case RECORD_COMPONENT -> this.typeParameterInfoFor(context.asRecordComponent(), tv);
    case TYPE -> throw new UnsupportedOperationException("TODO: implement?");
    };
  }

  final TypeParameterInfo typeParameterInfoFor(final ClassInfo context, final org.jboss.jandex.TypeVariable tv) {
    final String id = tv.identifier();
    for (final org.jboss.jandex.TypeVariable tp : context.typeParameters()) {
      if (tp.identifier().equals(id)) {
        return new TypeParameterInfo(context, tp);
      }
    }
    final EnclosingMethodInfo enclosingMethod = context.enclosingMethod();
    return enclosingMethod == null ? this.typeParameterInfoFor(context.enclosingClass(), tv) : this.typeParameterInfoFor(enclosingMethod, tv);
  }

  final TypeParameterInfo typeParameterInfoFor(final DotName context, final org.jboss.jandex.TypeVariable tv) {
    return this.typeParameterInfoFor(this.classInfoFor(context), tv);
  }

  final TypeParameterInfo typeParameterInfoFor(final EnclosingMethodInfo context, final org.jboss.jandex.TypeVariable tv) {
    return this.typeParameterInfoFor(this.classInfoFor(context.enclosingClass()).method(context.name(), context.parameters().toArray(new Type[0])), tv);
  }

  final TypeParameterInfo typeParameterInfoFor(final FieldInfo context, final org.jboss.jandex.TypeVariable tv) {
    return this.typeParameterInfoFor(context.declaringClass(), tv);
  }

  final TypeParameterInfo typeParameterInfoFor(final MethodInfo context, final org.jboss.jandex.TypeVariable tv) {
    final String id = tv.identifier();
    for (final org.jboss.jandex.TypeVariable tp : context.typeParameters()) {
      if (tp.identifier().equals(id)) {
        return new TypeParameterInfo(context, tp);
      }
    }
    return this.typeParameterInfoFor(context.declaringClass(), tv);
  }

  final TypeParameterInfo typeParameterInfoFor(final MethodParameterInfo context, final org.jboss.jandex.TypeVariable tv) {
    return this.typeParameterInfoFor(context.method(), tv);
  }

  final TypeParameterInfo typeParameterInfoFor(final RecordComponentInfo context, final org.jboss.jandex.TypeVariable tv) {
    return this.typeParameterInfoFor(context.declaringClass(), tv);
  }

  final TypeParameterInfo typeParameterInfoFor(final TypeContext<org.jboss.jandex.TypeVariable> tc) {
    return this.typeParameterInfoFor(tc.context(), tc.type());
  }


  /*
   * Static methods.
   */


  private static final boolean isDefault(final MethodInfo mi) {
    if (mi.declaringClass().isInterface()) {
      final short flags = mi.flags();
      return !java.lang.reflect.Modifier.isStatic(flags) && !java.lang.reflect.Modifier.isAbstract(flags);
    }
    return false;
  }

  private static final ElementKind kind(final ClassInfo ci) {
    return
      ci.isAnnotation() ? ElementKind.ANNOTATION_TYPE :
      ci.isEnum() ? ElementKind.ENUM :
      ci.isInterface() ? ElementKind.INTERFACE :
      ci.isModule() ? ElementKind.MODULE :
      ci.isRecord() ? ElementKind.RECORD :
      ElementKind.CLASS;
  }

  private static final ElementKind kind(final FieldInfo f) {
    return f.isEnumConstant() ? ElementKind.ENUM_CONSTANT : ElementKind.FIELD;
  }

  private static final ElementKind kind(final MethodInfo m) {
    return
      m.isConstructor() ? ElementKind.CONSTRUCTOR :
      m.name().equals("<clinit>") ? ElementKind.STATIC_INIT :
      m.name().equals("<init>") ? ElementKind.INSTANCE_INIT :
      ElementKind.METHOD;
  }

  private static final TypeKind kind(final PrimitiveType p) {
    return kind(p.primitive());
  }

  private static final TypeKind kind(final PrimitiveType.Primitive p) {
    return switch (p) {
    case BOOLEAN -> TypeKind.BOOLEAN;
    case BYTE -> TypeKind.BYTE;
    case CHAR -> TypeKind.CHAR;
    case DOUBLE -> TypeKind.DOUBLE;
    case FLOAT -> TypeKind.FLOAT;
    case INT -> TypeKind.INT;
    case LONG -> TypeKind.LONG;
    case SHORT -> TypeKind.SHORT;
    };
  }

  private static final NestingKind nestingKind(final ClassInfo ci) {
    return nestingKind(ci.nestingType());
  }

  private static final NestingKind nestingKind(final NestingType n) {
    return switch (n) {
    case ANONYMOUS -> NestingKind.ANONYMOUS; // In fact, Jandex will never supply this.
    case INNER -> NestingKind.MEMBER;
    case LOCAL -> NestingKind.LOCAL; // In fact, Jandex will never supply this.
    case TOP_LEVEL -> NestingKind.TOP_LEVEL;
    };
  }


  /*
   * Inner and nested classes.
   */


  private static final record TypeContext<T extends Type>(AnnotationTarget context, T type) {

    TypeContext {
      Objects.requireNonNull(context, "context");
    }

  }

  public static final record TypeParameterInfo(AnnotationTarget annotationTarget, org.jboss.jandex.TypeVariable typeVariable) {

    public TypeParameterInfo {
      switch (annotationTarget.kind()) {
      case CLASS:
      case METHOD:
        break;
      default:
        throw new IllegalArgumentException("annotationTarget: " + annotationTarget);
      }
      Objects.requireNonNull(typeVariable, "typeVariable");
    }

    /**
     * Returns the value of the {@code annotationTarget} record component, which will either be a {@link ClassInfo} or a
     * {@link MethodInfo}.
     *
     * @return the value of the {@code annotationTarget} record component, which will either be a {@link ClassInfo} or a
     * {@link MethodInfo}
     */
    @Override
    public final AnnotationTarget annotationTarget() {
      return this.annotationTarget;
    }

    public final AnnotationTarget.Kind kind() {
      return this.annotationTarget().kind();
    }

    public final String identifier() {
      return this.typeVariable().identifier();
    }

  }

}
