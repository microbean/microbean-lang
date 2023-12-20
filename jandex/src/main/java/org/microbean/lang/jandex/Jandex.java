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
import java.util.List;
import java.util.Objects;

import java.util.function.BiFunction;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassInfo.EnclosingMethodInfo;
import org.jboss.jandex.ClassInfo.NestingType;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.ModuleInfo;
import org.jboss.jandex.ModuleInfo.ExportedPackageInfo;
import org.jboss.jandex.ModuleInfo.OpenedPackageInfo;
import org.jboss.jandex.ModuleInfo.ProvidedServiceInfo;
import org.jboss.jandex.ModuleInfo.RequiredModuleInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.RecordComponentInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeParameterTypeTarget;

import org.microbean.lang.Modeler;

public final class Jandex extends Modeler {

  private static final Type[] EMPTY_TYPE_ARRAY = new Type[0];

  private final IndexView i;

  private final BiFunction<? super String, ? super IndexView, ? extends ClassInfo> unindexedClassnameFunction;

  public Jandex(final IndexView i) {
    this(i, (n, j) -> {
        throw new IllegalArgumentException("class " + n + " not found in IndexView " + j);
      });
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
    case null -> null;
    case AnnotationInstance a -> this.annotationValue(a);
    case AnnotationMirror a -> this.annotationValue(a);
    case javax.lang.model.element.AnnotationValue a -> this.annotationValue(a);
    case org.jboss.jandex.AnnotationValue a -> this.annotationValue(a);
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
    case long[] o -> this.annotationValue(o);
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

  public final <V extends javax.lang.model.element.AnnotationValue> V annotationValue(final V v) {
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


  /*
   * Element methods.
   */


  public final Element element(final DotName n) {
    final ClassInfo ci = this.classInfoFor(n);
    return ci == null ? null : this.element(ci);
  }

  @Override // ElementSource
  public final TypeElement typeElement(final CharSequence m, final CharSequence n) {
    return this.typeElement(n);
  }

  @Override // ElementSource
  public final TypeElement typeElement(final CharSequence n) {
    final ClassInfo ci = this.classInfoFor(n.toString());
    return
      ci == null || ci.isModule() ? null :
      (TypeElement)this.element(ci);
  }

  public final PackageElement packageElement(final DotName n) {
    return n == null ? null : this.element(new PackageInfo(n));
  }

  public final PackageElement packageElement(final String n) {
    return n == null ? null : this.packageElement(DotName.createSimple(n));
  }

  public final ModuleElement element(final ModuleInfo mi) {
    return mi == null ? null : this.element(mi, () -> new org.microbean.lang.element.ModuleElement(mi.isOpen()), this::build);
  }

  public final PackageElement element(final PackageInfo pi) {
    return pi == null ? null : this.element(pi, org.microbean.lang.element.PackageElement::new, this::build);
  }

  public final TypeElement element(final AnnotationInstance ai) {
    return ai == null ? null : (TypeElement)this.element(this.classInfoFor(ai.name()));
  }

  public final Element element(final ClassInfo ci) {
    if (ci == null) {
      return null;
    } else if (ci.isModule()) {
      return this.element(ci.module());
    } else {
      return this.element(ci, () -> new org.microbean.lang.element.TypeElement(kind(ci), nestingKind(ci)), this::build);
    }
  }

  public final Element element(final ClassType ct) {
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


  /*
   * Type methods.
   */


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

  public final TypeMirror type(final FieldInfo fi) {
    if (fi == null) {
      return null;
    }
    final Type t = fi.type();
    return switch (t.kind()) {
    case ARRAY -> this.type(fi, org.microbean.lang.type.ArrayType::new, this::build);
    case CLASS, PARAMETERIZED_TYPE -> this.type(fi, org.microbean.lang.type.DeclaredType::new, this::build);
    case PRIMITIVE -> this.type(fi, () -> new org.microbean.lang.type.PrimitiveType(kind(t.asPrimitiveType())), this::build);
    case TYPE_VARIABLE -> this.type(fi, () -> new org.microbean.lang.type.TypeVariable(this), this::build);
    default -> throw new IllegalStateException("t: " + t);
    };
  }

  public final ExecutableType type(final MethodInfo mi) {
    return mi == null ? null : this.type(mi, org.microbean.lang.type.ExecutableType::new, this::build);
  }

  public final TypeMirror type(final MethodParameterInfo mpi) {
    if (mpi == null) {
      return null;
    }
    final Type t = mpi.type();
    return switch (t.kind()) {
    case ARRAY -> this.type(mpi, org.microbean.lang.type.ArrayType::new, this::build);
    case CLASS, PARAMETERIZED_TYPE -> this.type(mpi, org.microbean.lang.type.DeclaredType::new, this::build);
    case PRIMITIVE -> this.type(mpi, () -> new org.microbean.lang.type.PrimitiveType(kind(t.asPrimitiveType())), this::build);
    case TYPE_VARIABLE -> this.type(mpi, () -> new org.microbean.lang.type.TypeVariable(this), this::build);
    default -> throw new IllegalStateException("t: " + t);
    };
  }

  public final TypeMirror type(final RecordComponentInfo rci) {
    if (rci == null) {
      return null;
    }
    final Type t = rci.type();
    return switch (t.kind()) {
    case ARRAY -> this.type(rci, org.microbean.lang.type.ArrayType::new, this::build);
    case CLASS, PARAMETERIZED_TYPE -> this.type(rci, org.microbean.lang.type.DeclaredType::new, this::build);
    case PRIMITIVE -> this.type(rci, () -> new org.microbean.lang.type.PrimitiveType(kind(t.asPrimitiveType())), this::build);
    case TYPE_VARIABLE -> this.type(rci, () -> new org.microbean.lang.type.TypeVariable(this), this::build);
    default -> throw new IllegalStateException("rci.type(): " + t);
    };
  }

  public final TypeMirror type(final TypeContext k) {
    final Type type = k == null ? null : k.type();
    return type == null ? null : switch (type.kind()) {
    case ARRAY -> this.type(k, org.microbean.lang.type.ArrayType::new, this::build);
    case CLASS, PARAMETERIZED_TYPE -> this.type(k, org.microbean.lang.type.DeclaredType::new, this::build);
    case PRIMITIVE -> this.type(type.asPrimitiveType(), () -> new org.microbean.lang.type.PrimitiveType(kind(type.asPrimitiveType())), this::build);
    case TYPE_VARIABLE -> this.type(this.typeParameterInfoFor(k));
    // k.kind() had better be TYPE_ARGUMENT below?
    case TYPE_VARIABLE_REFERENCE -> this.type(new TypeContext(k.context(), type.asTypeVariableReference().follow(), k.position(), k.kind()));
    case UNRESOLVED_TYPE_VARIABLE -> throw new AssertionError();
    case VOID -> org.microbean.lang.type.NoType.VOID;
    case WILDCARD_TYPE -> this.type(k, org.microbean.lang.type.WildcardType::new, this::build);
    };
  }

  public final <T extends TypeMirror> T type(final T t) {
    return t;
  }

  public final javax.lang.model.type.TypeVariable type(final TypeParameterInfo tpi) {
    return tpi == null ? null : this.type(tpi, () -> new org.microbean.lang.type.TypeVariable(this), this::build);
  }

  @Override // TypeAndElementSource
  public final javax.lang.model.type.ArrayType arrayTypeOf(final TypeMirror componentType) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override // TypeAndElementSource
  public final boolean assignable(final TypeMirror t, final TypeMirror s) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override // TypeAndElementSource
  public final TypeElement boxedClass(final javax.lang.model.type.PrimitiveType t) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override // TypeAndElementSource
  public final <T extends TypeMirror> T erasure(final T t) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override // TypeAndElementSource
  public final NoType noType(final TypeKind k) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override // TypeAndElementSource
  public final javax.lang.model.type.PrimitiveType primitiveType(final TypeKind k) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override // TypeAndElementSource
  public final DeclaredType declaredType(final DeclaredType containingType, final TypeElement typeElement, final TypeMirror... arguments) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override // TypeAndElementSource
  public final DeclaredType declaredType(final TypeElement typeElement, final TypeMirror... arguments) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override // TypeAndElementSource
  public final boolean sameType(final TypeMirror t, final TypeMirror s) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override // TypeAndElementSource
  public final TypeVariable typeVariable(final java.lang.reflect.TypeVariable<?> t) {
    throw new UnsupportedOperationException("TODO");
  }

  public final WildcardType wildcardType(final TypeMirror extendsBound, final TypeMirror superBound) {
    throw new UnsupportedOperationException("TODO");
  }




  /*
   * Annotation builders.
   */


  private final void build(final AnnotationInstance ai, final org.microbean.lang.element.AnnotationMirror am) {
    final ClassInfo annotationClass = this.classInfoFor(ai.name());

    final org.microbean.lang.type.DeclaredType t = (org.microbean.lang.type.DeclaredType)this.type(annotationClass);
    assert t.asElement() != null;
    am.setAnnotationType(t);

    for (final org.jboss.jandex.AnnotationValue v : ai.values()) {
      final javax.lang.model.element.ExecutableElement ee = this.element(this.annotationElementFor(annotationClass, v.name()));
      assert ee.getEnclosingElement() != null : "ee: " + ee + "; ai: " + ai + "; annotationClass: " + annotationClass + "; v: " + v;
      am.putElementValue(ee, this.annotationValue(v));
    }
  }


  /*
   * Element builders.
   */


  private final void build(final ModuleInfo mi, final org.microbean.lang.element.ModuleElement e) {
    // Simple name.
    e.setSimpleName(mi.name().toString());

    // Type.
    e.setType(org.microbean.lang.type.NoType.MODULE);

    for (final RequiredModuleInfo rmi : mi.requires()) {
      e.addDirective(new org.microbean.lang.element.ModuleElement.RequiresDirective(this.element(this.i.getModuleByName(rmi.name())),
                                                                                    rmi.isStatic(),
                                                                                    rmi.isTransitive()));
    }

    for (final ExportedPackageInfo epi : mi.exports()) {
      final List<? extends DotName> epiTargets = epi.targets();
      final List<ModuleElement> targets = new ArrayList<>(epiTargets.size());
      for (final DotName epiTarget : epiTargets) {
        targets.add(this.element(this.i.getModuleByName(epiTarget)));
      }
      e.addDirective(new org.microbean.lang.element.ModuleElement.ExportsDirective(this.packageElement(epi.source()), targets));
    }

    for (final OpenedPackageInfo opi : mi.opens()) {
      final List<? extends DotName> opiTargets = opi.targets();
      final List<ModuleElement> targets = new ArrayList<>(opiTargets.size());
      for (final DotName opiTarget : opiTargets) {
        targets.add(this.element(this.i.getModuleByName(opiTarget)));
      }
      e.addDirective(new org.microbean.lang.element.ModuleElement.OpensDirective(this.packageElement(opi.source()), targets));
    }

    for (final ProvidedServiceInfo psi : mi.provides()) {
      final List<? extends DotName> psiProviders = psi.providers();
      final List<TypeElement> providers = new ArrayList<>(psiProviders.size());
      for (final DotName psiTarget : psiProviders) {
        providers.add((TypeElement)this.element(this.classInfoFor(psiTarget)));
      }
      e.addDirective(new org.microbean.lang.element.ModuleElement.ProvidesDirective((TypeElement)this.element(psi.service()), providers));
    }

    e.setEnclosedElementsGenerator(() -> {
        for (final DotName pn : mi.packages()) {
          this.packageElement(pn);
        }
      });

    for (final AnnotationInstance ai : mi.annotations()) {
      e.addAnnotationMirror(this.annotation(ai));
    }

  }

  private final void build(final PackageInfo pi, final org.microbean.lang.element.PackageElement e) {
    final DotName pn = pi.name();

    // Simple name.
    e.setSimpleName(pn.toString());

    // Type.
    e.setType(org.microbean.lang.type.NoType.PACKAGE);

    // Enclosing element.
    e.setEnclosingElement(this.element(this.i.getKnownModules().stream()
                                       .filter(m -> m.packages().contains(pn))
                                       .findFirst()
                                       .orElse(null)));

    e.setEnclosedElementsGenerator(() -> this.i.getClassesInPackage(pn).forEach(this::element));
  }

  private final void build(final ClassInfo ci, final org.microbean.lang.element.TypeElement e) {
    // Simple name.
    e.setSimpleName(ci.name().local());

    // Type. Note that we haven't done type parameters yet, so the type arguments belonging to the type won't have
    // corresponding type parameters yet.
    final org.microbean.lang.type.DeclaredType t = (org.microbean.lang.type.DeclaredType)this.type(ci);
    e.setType(t);

    // Defining element.
    t.setDefiningElement(e);

    e.setEnclosingElement(switch (e.getNestingKind()) {
    // Anonymous and local classes are effectively ignored in the javax.lang.model.* hierarchy. The documentation for
    // javax.lang.model.element.Element#getEnclosedElements() says, in part: "A class or interface is considered to
    // enclose the fields, methods, constructors, record components, and member classes and interfaces that it directly
    // declares. A package encloses the top-level classes and interfaces within it, but is not considered to enclose
    // subpackages. A module encloses packages within it. Enclosed elements may include implicitly declared mandated
    // elements. Other kinds of elements are not currently considered to enclose any elements; however, that may change
    // as this API or the programming language evolves."
    //
    // Additionally, Jandex provides no access to local or anonymous classes at all.
    case ANONYMOUS, LOCAL -> null;
    case MEMBER -> this.element(this.classInfoFor(ci.enclosingClass()));
    case TOP_LEVEL -> this.packageElement(ci.name().prefix());
      });

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
    int position = 0;
    final Type superclassType = ci.superClassType();
    if (superclassType != null) {
      assert superclassType.kind() == Type.Kind.CLASS || superclassType.kind() == Type.Kind.PARAMETERIZED_TYPE;
      e.setSuperclass(this.type(new TypeContext(ci, superclassType, position++, TypeContext.Kind.EXTENDS)));
    }
    for (final Type iface : ci.interfaceTypes()) {
      e.addInterface(this.type(new TypeContext(ci, iface, position++, TypeContext.Kind.EXTENDS)));
    }
    position = 0;

    // Type parameters.
    for (final org.jboss.jandex.TypeVariable tp : ci.typeParameters()) {
      final org.microbean.lang.element.TypeParameterElement tpe =
        (org.microbean.lang.element.TypeParameterElement)this.element(new TypeParameterInfo(ci, tp));
      assert ((javax.lang.model.type.TypeVariable)tpe.asType()).asElement() == tpe :
        "tpe.asType(): " + tpe.asType() +
        "; tpe.asType().asElement(): " + ((javax.lang.model.type.TypeVariable)tpe.asType()).asElement();
      e.addTypeParameter(tpe);
    }
    assert e.getTypeParameters().size() == ci.typeParameters().size();

    e.setEnclosedElementsGenerator(() -> {
        ci.constructors().forEach(this::element);
        ci.unsortedRecordComponents().forEach(this::element);
        ci.unsortedFields().forEach(this::element);
        ci.unsortedMethods().forEach(this::element);
        ci.memberClasses().forEach(this::element);
      });

    for (final AnnotationInstance ai : ci.declaredAnnotations()) {
      e.addAnnotationMirror(this.annotation(ai));
    }
  }

  private final void build(final FieldInfo fi, final org.microbean.lang.element.VariableElement e) {
    // Simple name.
    e.setSimpleName(fi.name());

    // Type.
    e.setType(this.type(fi));

    // Enclosing element.
    e.setEnclosingElement(this.element(fi.declaringClass()));

    // Annotations.
    for (final AnnotationInstance ai : fi.declaredAnnotations()) {
      e.addAnnotationMirror(this.annotation(ai));
    }
  }

  private final void build(final MethodInfo mi, final org.microbean.lang.element.ExecutableElement e) {
    // Simple name.
    if (!mi.isConstructor()) {
      e.setSimpleName(mi.name());
    }

    // Type.
    e.setType(this.type(mi));

    // Enclosing element.
    final TypeElement ee = (TypeElement)this.element(mi.declaringClass());
    assert ee != null;
    e.setEnclosingElement(ee);
    assert e.getEnclosingElement() == ee : "e: " + e + "; ee: " + ee;

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

    for (final AnnotationInstance ai : mi.declaredAnnotations()) {
      e.addAnnotationMirror(this.annotation(ai));
    }
  }

  private final void build(final MethodParameterInfo mpi, final org.microbean.lang.element.VariableElement e) {
    // Simple name.
    String n = mpi.name();
    if (n == null) {
      n = "arg" + mpi.position();
    }
    e.setSimpleName(n);

    // Type.
    e.setType(this.type(mpi));

    // (No enclosing element.)
    // e.setEnclosingElement(this.element(mpi.method())); // interestingly not supported by the javax.lang.model.* api

    for (final AnnotationInstance ai : mpi.declaredAnnotations()) {
      e.addAnnotationMirror(this.annotation(ai));
    }
  }

  private final void build(final RecordComponentInfo r, final org.microbean.lang.element.RecordComponentElement e) {
    // Simple name.
    e.setSimpleName(r.name());

    // Type.
    e.setType(this.type(r));

    // Enclosing element.
    e.setEnclosingElement(this.element(r));

    e.setAccessor((org.microbean.lang.element.ExecutableElement)this.element(r.declaringClass().method(r.name())));

    for (final AnnotationInstance ai : r.declaredAnnotations()) {
      e.addAnnotationMirror(this.annotation(ai));
    }
  }

  private final void build(final TypeParameterInfo tpi, final org.microbean.lang.element.TypeParameterElement e) {
    // Simple name.
    e.setSimpleName(tpi.identifier());

    // Type.
    final org.microbean.lang.type.TypeVariable t = (org.microbean.lang.type.TypeVariable)this.type(tpi);
    e.setType(t);

    // Defining element.
    t.setDefiningElement(e);

    // Enclosing element.
    e.setEnclosingElement(switch (tpi.kind()) {
      case CLASS -> this.element(tpi.annotationTarget().asClass());
      case METHOD -> this.element(tpi.annotationTarget().asMethod());
      default -> throw new AssertionError();
      });

    for (final AnnotationInstance ai : tpi.typeVariable().annotations()) {
      // This is nice, in a way. We know all of these annotations will be type use annotations, because Jandex doesn't
      // really reify type parameters.
      //
      // Then we know that they can't be CLASS_EXTENDS, EMPTY, METHOD_PARAMETER, THROWS or TYPE_PARAMETER_BOUND, so they
      // must be TYPE_PARAMETER. We can't check, because ai.target() is guaranteed by javadoc to be null. But as you can
      // see we don't need to check.
      e.addAnnotationMirror(this.annotation(ai));
    }
  }


  /*
   * Type builders.
   */


  private final void build(final TypeContext tc, final org.microbean.lang.type.ArrayType t) {
    final org.jboss.jandex.ArrayType a = tc.type().asArrayType();
    final Type componentType = a.constituent();
    t.setComponentType(this.type(new TypeContext(tc.context(), componentType, 0, TypeContext.Kind.COMPONENT_TYPE)));
    for (final AnnotationInstance ai : a.annotations()) {
      t.addAnnotationMirror(this.annotation(ai));
    }
  }

  private final void build(final TypeContext tc, final org.microbean.lang.type.DeclaredType t) {
    final Type tct = tc.type();
    final ClassInfo ci = this.classInfoFor(tct);
    t.setDefiningElement((TypeElement)this.element(ci));
    switch (tct.kind()) {
    case CLASS:
      t.setEnclosingType(this.type(this.classInfoFor(ci.enclosingClass())));
      break;
    case PARAMETERIZED_TYPE:
      final ParameterizedType pt = tct.asParameterizedType();
      final Type ownerType = pt.owner();
      t.setEnclosingType(this.type(ownerType == null ? this.classInfoFor(ci.enclosingClass()) : this.classInfoFor(ownerType)));
      int position = 0;
      for (final Type arg : pt.arguments()) {
        t.addTypeArgument(this.type(new TypeContext(tc.context(), arg, position++, TypeContext.Kind.TYPE_ARGUMENT)));
      }
      break;
    default:
      throw new AssertionError();
    }
    for (final AnnotationInstance ai : tct.annotations()) {
      t.addAnnotationMirror(this.annotation(ai));
    }
  }

  private final void build(final TypeContext tc, final org.microbean.lang.type.WildcardType w) {
    final org.jboss.jandex.WildcardType tcType = tc.type().asWildcardType();
    Type bound = tcType.extendsBound();
    if (bound != null) {
      w.setExtendsBound(this.type(new TypeContext(tc.context(), bound, 0, TypeContext.Kind.EXTENDS)));
    }
    bound = tcType.superBound();
    if (bound != null) {
      w.setSuperBound(this.type(new TypeContext(tc.context(), bound, 0, TypeContext.Kind.EXTENDS)));
    }
    for (final AnnotationInstance ai : tcType.annotations()) {
      w.addAnnotationMirror(this.annotation(ai));
    }
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
    final List<? extends org.jboss.jandex.TypeVariable> tps = ci.typeParameters();
    for (int i = 0; i < tps.size(); i++) {
      t.addTypeArgument(this.type(new TypeContext(ci, tps.get(i), i, TypeContext.Kind.TYPE_ARGUMENT)));
    }

    // There isn't a way to get type use annotations on ci.

  }

  private final void build(final FieldInfo fi, final org.microbean.lang.type.ArrayType t) {
    final org.jboss.jandex.ArrayType ft = fi.type().asArrayType();
    final org.microbean.lang.element.VariableElement e = (org.microbean.lang.element.VariableElement)this.element(fi);
    e.setType(t);
    t.setComponentType(this.type(new TypeContext(fi, ft.constituent(), 0, TypeContext.Kind.COMPONENT_TYPE)));
    for (final AnnotationInstance ai : ft.annotations()) {
      t.addAnnotationMirror(this.annotation(ai));
    }
  }

  private final void build(final FieldInfo fi, final org.microbean.lang.type.DeclaredType t) {
    final org.jboss.jandex.Type ft = fi.type();
    final org.microbean.lang.element.VariableElement e = (org.microbean.lang.element.VariableElement)this.element(fi);
    e.setType(t);
    t.setDefiningElement((org.microbean.lang.element.TypeElement)this.element(ft.name()));
    for (final AnnotationInstance ai : ft.annotations()) {
      t.addAnnotationMirror(this.annotation(ai));
    }
  }

  private final void build(final FieldInfo fi, final org.microbean.lang.type.PrimitiveType t) {
    final org.microbean.lang.element.VariableElement e = (org.microbean.lang.element.VariableElement)this.element(fi);
    e.setType(t);
    // Primitive types cannot bear annotations.
  }

  private final void build(final FieldInfo fi, final org.microbean.lang.type.TypeVariable t) {
    final org.jboss.jandex.TypeVariable ft = fi.type().asTypeVariable();
    final org.microbean.lang.element.VariableElement e = (org.microbean.lang.element.VariableElement)this.element(fi);
    e.setType(t);
    t.setDefiningElement((org.microbean.lang.element.TypeParameterElement)this.element(typeParameterInfoFor(fi, ft)));
    for (final AnnotationInstance ai : ft.annotations()) {
      t.addAnnotationMirror(this.annotation(ai));
    }
  }

  private final void build(final MethodInfo mi, final org.microbean.lang.type.ExecutableType t) {
    for (final MethodParameterInfo mpi : mi.parameters()) {
      t.addParameterType(this.type(mpi));
    }

    final Type receiverType;
    if (mi.isConstructor()) {
      receiverType = mi.declaringClass().enclosingClass() == null ? null : mi.receiverType();
    } else {
      receiverType = java.lang.reflect.Modifier.isStatic(mi.flags()) ? null : mi.receiverType();
    }
    t.setReceiverType(receiverType == null ?
                      org.microbean.lang.type.NoType.NONE :
                      this.type(new TypeContext(mi, receiverType, 0, TypeContext.Kind.RECEIVER)));
    t.setReturnType(this.type(new TypeContext(mi, mi.returnType(), 0, TypeContext.Kind.RETURN)));

    int position = 0;
    for (final Type et : mi.exceptions()) {
      t.addThrownType(this.type(new TypeContext(mi, et, position++, TypeContext.Kind.THROWS)));
    }

    position = 0;
    for (final org.jboss.jandex.TypeVariable tv : mi.typeParameters()) {
      t.addTypeVariable((org.microbean.lang.type.TypeVariable)this.type(new TypeContext(mi, tv, position++, TypeContext.Kind.TYPE_ARGUMENT)));
    }
  }

  private final void build(final MethodParameterInfo mpi, final org.microbean.lang.type.ArrayType t) {
    final org.jboss.jandex.ArrayType mpit = mpi.type().asArrayType();
    final org.microbean.lang.element.VariableElement e = (org.microbean.lang.element.VariableElement)this.element(mpi);
    e.setType(t);
    t.setComponentType(this.type(new TypeContext(mpi, mpit.constituent(), 0, TypeContext.Kind.COMPONENT_TYPE)));
    for (final AnnotationInstance ai : mpit.annotations()) {
      t.addAnnotationMirror(this.annotation(ai));
    }
  }

  private final void build(final MethodParameterInfo mpi, final org.microbean.lang.type.DeclaredType t) {
    final org.jboss.jandex.Type mpit = mpi.type();
    final org.microbean.lang.element.VariableElement e = (org.microbean.lang.element.VariableElement)this.element(mpi);
    e.setType(t);
    t.setDefiningElement((org.microbean.lang.element.TypeElement)this.element(mpit.name()));
    for (final AnnotationInstance ai : mpit.annotations()) {
      t.addAnnotationMirror(this.annotation(ai));
    }
  }

  private final void build(final MethodParameterInfo mpi, final org.microbean.lang.type.PrimitiveType t) {
    final org.microbean.lang.element.VariableElement e = (org.microbean.lang.element.VariableElement)this.element(mpi);
    e.setType(t);
    // Primitive types cannot bear annotations.
  }

  private final void build(final MethodParameterInfo mpi, final org.microbean.lang.type.TypeVariable t) {
    final org.jboss.jandex.TypeVariable mpit = mpi.type().asTypeVariable();
    final org.microbean.lang.element.VariableElement e = (org.microbean.lang.element.VariableElement)this.element(mpi);
    e.setType(t);
    t.setDefiningElement((org.microbean.lang.element.TypeParameterElement)this.element(typeParameterInfoFor(mpi, mpit)));
    for (final AnnotationInstance ai : mpit.annotations()) {
      t.addAnnotationMirror(this.annotation(ai));
    }
  }

  private final void build(final RecordComponentInfo rci, final org.microbean.lang.type.ArrayType t) {
    final org.jboss.jandex.ArrayType rcit = rci.type().asArrayType();
    final org.microbean.lang.element.RecordComponentElement e = (org.microbean.lang.element.RecordComponentElement)this.element(rci);
    e.setType(t);
    t.setComponentType(this.type(new TypeContext(rci, rcit.constituent(), 0, TypeContext.Kind.COMPONENT_TYPE)));
    for (final AnnotationInstance ai : rcit.annotations()) {
      t.addAnnotationMirror(this.annotation(ai));
    }
  }

  private final void build(final RecordComponentInfo rci, final org.microbean.lang.type.DeclaredType t) {
    final org.jboss.jandex.Type rcit = rci.type();
    final org.microbean.lang.element.RecordComponentElement e = (org.microbean.lang.element.RecordComponentElement)this.element(rci);
    e.setType(t);
    t.setDefiningElement((org.microbean.lang.element.TypeElement)this.element(rcit.name()));
    for (final AnnotationInstance ai : rcit.annotations()) {
      t.addAnnotationMirror(this.annotation(ai));
    }
  }

  private final void build(final RecordComponentInfo rci, final org.microbean.lang.type.PrimitiveType t) {
    final org.microbean.lang.element.RecordComponentElement e = (org.microbean.lang.element.RecordComponentElement)this.element(rci);
    e.setType(t);
    // Primitive types cannot bear annotations.
  }

  private final void build(final RecordComponentInfo rci, final org.microbean.lang.type.TypeVariable t) {
    final org.jboss.jandex.TypeVariable rcit = rci.type().asTypeVariable();
    final org.microbean.lang.element.RecordComponentElement e = (org.microbean.lang.element.RecordComponentElement)this.element(rci);
    e.setType(t);
    t.setDefiningElement((org.microbean.lang.element.TypeParameterElement)this.element(typeParameterInfoFor(rci, rcit)));
    for (final AnnotationInstance ai : rcit.annotations()) {
      t.addAnnotationMirror(this.annotation(ai));
    }
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
      t.setUpperBound(this.type(new TypeContext(context, bounds.get(0), 0, TypeContext.Kind.BOUND)));
      break;
    default:
      final org.microbean.lang.type.IntersectionType upperBound = new org.microbean.lang.type.IntersectionType();
      int position = 0;
      for (final Type bound : bounds) {
        upperBound.addBound(this.type(new TypeContext(context, bound, position++, TypeContext.Kind.BOUND)));
      }
      t.setUpperBound(upperBound);
      break;
    }

    // I *believe* that when all is said and done, tpi.type().annotations() will reflect annotations on the type
    // parameter *element*, not the type use, and so there's no way to get type use annotations here in Jandex.

  }

  // Having PrimitiveType as a parameter here is OK because primitive types cannot bear annotations.
  private final void build(final PrimitiveType p, final org.microbean.lang.type.PrimitiveType t) {

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
    case TYPE -> throw new UnsupportedOperationException();
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

  final TypeParameterInfo typeParameterInfoFor(final TypeContext tc) {
    return this.typeParameterInfoFor(tc.context(), tc.type().asTypeVariable());
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


  public static final record PackageInfo(DotName name) {

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

  // Represents a "type context" in the parlance of
  // https://docs.oracle.com/javase/specs/jls/se19/html/jls-4.html#jls-4.11. Not all such type contexts are represented
  // here.
  public static final class TypeContext {

    private final TypeContext parent;

    private final AnnotationTarget context;

    private final Type type;

    private final int position;

    private final Kind kind;

    private TypeContext(final AnnotationTarget context, final Type type, final int position, final Kind kind) {
      this(null, context, type, position, kind);
    }

    private TypeContext(final TypeContext parent, final AnnotationTarget context, final Type type, final int position, final Kind kind) {
      super();
      validate(parent, context, type, position, kind);
      this.parent = parent;
      this.context = context;
      this.type = type;
      this.position = position;
      this.kind = kind;
    }

    private final TypeContext parent() {
      return this.parent;
    }

    private final AnnotationTarget context() {
      return this.context;
    }

    private final Type type() {
      return this.type;
    }

    private final int position() {
      return this.position;
    }

    private final Kind kind() {
      return this.kind;
    }

    @Override
    public final int hashCode() {
      int hashCode = 17;
      Object v = this.parent();
      int c = v == null ? 0 : v.hashCode();
      hashCode = 37 * hashCode + c;

      v = this.context();
      c = v == null ? 0 : v.hashCode();
      hashCode = 37 * hashCode + c;

      v = this.type();
      c = v == null ? 0 : v.hashCode();
      hashCode = 37 * hashCode + c;

      hashCode = 37 * hashCode + this.position();

      hashCode = 37 * hashCode + this.kind().hashCode();

      return hashCode;
    }

    @Override
    public final boolean equals(final Object other) {
      if (other == this) {
        return true;
      } else if (other != null && other.getClass() == this.getClass()) {
        final TypeContext her = (TypeContext)other;
        return
          Objects.equals(this.parent(), her.parent()) &&
          Objects.equals(this.context(), her.context()) &&
          Objects.equals(this.type(), her.type()) &&
          Objects.equals(this.position(), her.position()) &&
          Objects.equals(this.kind(), her.kind());
      } else {
        return false;
      }
    }

    private static final void validate(final TypeContext parent, final AnnotationTarget context, final Type type, final int position, final Kind kind) {
      if (position < 0) {
        throw new IndexOutOfBoundsException("position: " + position);
      }
    }

    // See relevant type contexts from https://docs.oracle.com/javase/specs/jls/se19/html/jls-4.html#jls-4.11
    private static enum Kind {

      // See also https://github.com/openjdk/jdk/blob/jdk-21%2B13/src/jdk.compiler/share/classes/com/sun/tools/javac/code/TargetType.java

      BOUND, // e.g. the "@Bar Glug" in "public class Foo<T extends @Bar Glug> {}"
      EXTENDS, // e.g. the "Glug", "Bar" or "Qux" in "public class Foo<T extends Glug> extends @Baz Bar implements Qux {}"
      RETURN, // method return type
      THROWS, // (method) throws type
      RECEIVER, // method receiver type
      TYPE_ARGUMENT, // e.g. the "String" in "public class Foo extends Bar<@Baz String> {}"
      COMPONENT_TYPE // e.g. "@Baz"-annotated "[]" in "@Qux String @Bar [] @Baz []"

    }

  }

}
