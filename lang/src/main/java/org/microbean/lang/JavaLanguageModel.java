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

import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.List;
import java.util.Optional;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import org.microbean.lang.ElementSource;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;

public final class JavaLanguageModel implements Constable, ElementSource {

  private static final ClassDesc CD_JavaLanguageModel = ClassDesc.of("org.microbean.lang.JavaLanguageModel");

  public JavaLanguageModel() {
    super();
  }

  @Override // Constable
  public final Optional<DynamicConstantDesc<JavaLanguageModel>> describeConstable() {
    return Optional.of(DynamicConstantDesc.of(BSM_INVOKE,
                                              MethodHandleDesc.ofConstructor(CD_JavaLanguageModel)));
  }

  @Override // ElementSource
  public final Element element(final String moduleName, final String n) {
    return this.typeElement(this.moduleElement(moduleName), n);
  }

  @Override // ElementSource
  public final Element element(final String n) {
    return this.typeElement(n);
  }

  public final ModuleElement moduleElement(final CharSequence n) {
    return Lang.moduleElement(n);
  }

  public final ModuleElement moduleElement(final Class<?> c) {
    return Lang.moduleElement(c);
  }

  public final ModuleElement moduleElement(final Module m) {
    return Lang.moduleElement(m);
  }

  public final ModuleElement moduleElement(final ModuleDescriptor m) {
    return m == null ? null : this.moduleElement(m.name());
  }

  public final ModuleElement moduleElement(final ModuleReference m) {
    return this.moduleElement(m == null ? (ModuleDescriptor)null : m.descriptor());
  }

  public final ModuleElement moduleElement(final ResolvedModule m) {
    return m == null ? null : this.moduleElement(m.name());
  }

  public final PackageElement packageElement(final CharSequence n) {
    return Lang.packageElement(n);
  }

  public final PackageElement packageElement(final Package p) {
    return Lang.packageElement(p);
  }

  public final PackageElement packageElement(final Class<?> c) {
    return Lang.packageElement(c);
  }

  public final PackageElement packageElement(final ModuleElement m, final Package p) {
    return Lang.packageElement(m, p == null ? "" : p.getName());
  }

  public final PackageElement packageElement(final Module m, final Package p) {
    return Lang.packageElement(m, p);
  }

  public final TypeElement typeElement(final Module m, final CharSequence n) {
    return Lang.typeElement(m, n);
  }

  public final TypeElement typeElement(final ModuleElement m, final CharSequence n) {
    return Lang.typeElement(m, n);
  }

  public final TypeElement typeElement(final CharSequence n) {
    return Lang.typeElement(n);
  }

  public final TypeElement typeElement(final Type t) {
    return Lang.typeElement(t);
  }

  public final TypeElement typeElement(final Class<?> c) {
    return Lang.typeElement(c);
  }

  public final ExecutableElement executableElement(final Executable e) {
    return Lang.executableElement(e);
  }

  public final ExecutableElement executableElement(final Constructor<?> c) {
    return Lang.executableElement(c);
  }

  public final ExecutableElement executableElement(final Class<?> declaringClass, final CharSequence name, final MethodHandle mh) {
    return this.executableElement(this.typeElement(declaringClass), name, mh);
  }

  public final ExecutableElement executableElement(final TypeElement declaringClass, final CharSequence name, final MethodHandle mh) {
    return mh == null ? null : this.executableElement(declaringClass, name, mh.type());
  }

  public final ExecutableElement executableElement(final Class<?> declaringClass, final CharSequence name, final MethodType mt) {
    return this.executableElement(this.typeElement(declaringClass), name, mt);
  }

  public final ExecutableElement executableElement(final TypeElement declaringClass, final CharSequence name, final MethodType mt) {
    return mt == null ? null :
      this.executableElement(declaringClass,
                             name,
                             this.type(mt.returnType()),
                             mt.parameterCount() <= 0 ? List.of() : Lang.typeList(mt.parameterList()));
  }

  public final ExecutableElement executableElement(final TypeElement declaringClass, final CharSequence name, final TypeMirror returnType) {
    return this.executableElement(declaringClass, name, returnType, List.of());
  }

  public final ExecutableElement executableElement(final TypeElement declaringClass,
                                                   final CharSequence name,
                                                   TypeMirror returnType,
                                                   final List<? extends TypeMirror> parameterTypes) {
    return Lang.executableElement(declaringClass, name, returnType, parameterTypes);
  }

  public final ExecutableElement executableElement(final Method m) {
    return Lang.executableElement(m);
  }

  public final VariableElement variableElement(final Field f) {
    return Lang.variableElement(f);
  }

  public final TypeMirror type(final Type t) {
    return Lang.type(t);
  }

  public final DeclaredType declaredType(final Type ownerType,
                                         final Type rawType,
                                         final Type... typeArguments) {
    return Lang.declaredType(ownerType, rawType, typeArguments);
  }

  public final TypeMirror type(final Field f) {
    return Lang.type(f);
  }

  public final ArrayType arrayTypeOf(final TypeMirror componentType) {
    return Lang.arrayTypeOf(componentType);
  }

  public final ArrayType arrayType(final Class<?> arrayClass) {
    return Lang.arrayType(arrayClass);
  }

  public final ArrayType arrayType(final GenericArrayType g) {
    return Lang.arrayType(g);
  }

  public final DeclaredType declaredType(final TypeElement e, final TypeMirror... ta) {
    return Lang.declaredType(e, ta);
  }

  public final DeclaredType declaredType(final Type t) {
    return Lang.declaredType(t);
  }

  public final DeclaredType declaredType(final Class<?> c) {
    return Lang.declaredType(c);
  }

  public final DeclaredType declaredType(final ParameterizedType pt) {
    return Lang.declaredType(pt);
  }

  public final ExecutableType executableType(final Executable e) {
    return Lang.executableType(e);
  }

  public final PrimitiveType primitiveType(final TypeKind k) {
    return Lang.primitiveType(k);
  }

  public final PrimitiveType primitiveType(final Class<?> c) {
    return Lang.primitiveType(c);
  }

  public final TypeParameterElement typeParameterElement(final java.lang.reflect.TypeVariable<?> t) {
    return Lang.typeParameterElement(t);
  }

  public final Parameterizable parameterizable(final GenericDeclaration gd) {
    return Lang.parameterizable(gd);
  }

  public final TypeVariable typeVariable(final java.lang.reflect.TypeVariable<?> t) {
    return Lang.typeVariable(t);
  }

  public final WildcardType wildcardType(final java.lang.reflect.WildcardType t) {
    return Lang.wildcardType(t);
  }

  public final WildcardType wildcardType() {
    return Lang.wildcardType();
  }

  public final boolean assignable(final TypeMirror payload, final TypeMirror receiver) {
    return Lang.assignable(payload, receiver);
  }

  public final boolean subtype(final TypeMirror payload, final TypeMirror receiver) {
    return Lang.subtype(payload, receiver);
  }

}
