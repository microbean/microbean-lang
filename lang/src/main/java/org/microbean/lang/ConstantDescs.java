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
package org.microbean.lang;

import java.lang.constant.ClassDesc;

/**
 * A utility class containing {@link ClassDesc} instances describing classes in this package and related packages.
 *
 * @author <a href="https://about.me/lairdnelson/" target="_top">Laird Nelson</a>
 */
public final class ConstantDescs {

  public static final ClassDesc CD_ArrayType = ClassDesc.of("javax.lang.model.type.ArrayType");

  public static final ClassDesc CD_CharSequence = ClassDesc.of("java.lang.CharSequence");

  public static final ClassDesc CD_DeclaredType = ClassDesc.of("javax.lang.model.type.DeclaredType");

  public static final ClassDesc CD_DelegatingElement = ClassDesc.of("org.microbean.lang.element.DelegatingElement");

  public static final ClassDesc CD_DelegatingTypeMirror = ClassDesc.of("org.microbean.lang.type.DelegatingTypeMirror");

  public static final ClassDesc CD_Element = ClassDesc.of("javax.lang.model.element.Element");

  public static final ClassDesc CD_Equality = ClassDesc.of("org.microbean.lang.Equality");

  public static final ClassDesc CD_ExecutableElement = ClassDesc.of("javax.lang.model.element.ExecutableElement");

  public static final ClassDesc CD_Lang = ClassDesc.of("org.microbean.lang.Lang");

  public static final ClassDesc CD_ModuleElement = ClassDesc.of("javax.lang.model.element.ModuleElement");

  public static final ClassDesc CD_Name = ClassDesc.of("javax.lang.model.element.Name");

  public static final ClassDesc CD_NoType = ClassDesc.of("javax.lang.model.type.NoType");

  public static final ClassDesc CD_NullType = ClassDesc.of("javax.lang.model.type.NullType");

  public static final ClassDesc CD_PackageElement = ClassDesc.of("javax.lang.model.element.PackageElement");

  public static final ClassDesc CD_PrimitiveType = ClassDesc.of("javax.lang.model.type.PrimitiveType");

  public static final ClassDesc CD_SameTypeEquality = ClassDesc.of("org.microbean.lang.SameTypeEquality");

  public static final ClassDesc CD_TypeAndElementSource = ClassDesc.of("org.microbean.lang.TypeAndElementSource");

  public static final ClassDesc CD_TypeElement = ClassDesc.of("javax.lang.model.element.TypeElement");

  public static final ClassDesc CD_TypeKind = ClassDesc.of("javax.lang.model.type.TypeKind");

  public static final ClassDesc CD_TypeMirror = ClassDesc.of("javax.lang.model.type.TypeMirror");

  public static final ClassDesc CD_VariableElement = ClassDesc.of("javax.lang.model.element.VariableElement");

  public static final ClassDesc CD_WildcardType = ClassDesc.of("javax.lang.model.type.WildcardType");

  private ConstantDescs() {
    super();
  }

}
