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
package org.microbean.lang;

import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

public interface TypeAndElementSource {

  public default TypeElement typeElement(final Class<?> c) {
    final Module m = c.getModule();
    return m == null ? this.typeElement(c.getCanonicalName()) : this.typeElement(m.getName(), c.getCanonicalName());
  }

  public default TypeElement typeElement(final CharSequence canonicalName) {
    return this.typeElement(null, canonicalName);
  }

  public default DeclaredType declaredType(final TypeElement typeElement, final TypeMirror... typeArguments) {
    return this.declaredType(null, typeElement, typeArguments);
  }

  public default DeclaredType declaredType(final Class<?> c) {
    final Module m = c.getModule();
    return m == null ? this.declaredType(c.getCanonicalName()) : this.declaredType(m.getName(), c.getCanonicalName());
  }

  public default DeclaredType declaredType(final CharSequence moduleName, final CharSequence canonicalName) {
    return (DeclaredType)this.typeElement(moduleName, canonicalName).asType();
  }

  public default DeclaredType declaredType(final CharSequence canonicalName) {
    return (DeclaredType)this.typeElement(canonicalName).asType();
  }

  public TypeElement typeElement(final CharSequence moduleName, final CharSequence canonicalName);

  public DeclaredType declaredType(final DeclaredType containingType,
                                   final TypeElement typeElement,
                                   final TypeMirror... typeArguments);

  /*
  public default Element element(final Class<?> c) {
    final String canonicalName = c.getCanonicalName();
    return canonicalName == null ? null : this.element(c.getModule().getName(), canonicalName);
  }

  public default Element element(final String name) {
    return this.element(null, name);
  }

  public Element element(final String moduleName, final String name);
  */

}
