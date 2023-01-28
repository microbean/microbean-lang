/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2022–2023 microBean™.
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
package org.microbean.lang.element;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;

import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public final class PackageElement extends Element implements javax.lang.model.element.PackageElement {


  /*
   * Constructors.
   */


  public PackageElement() {
    super(ElementKind.PACKAGE);
  }


  /*
   * Instance methods.
   */


  @Override // Element
  public final <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    return v.visitPackage(this, p);
  }

  @Override // QualifiedNameable
  public final javax.lang.model.element.Name getQualifiedName() {
    return this.getSimpleName();
  }
  
  @Override // Element
  protected final TypeMirror validateType(final TypeMirror type) {
    if (type.getKind() == TypeKind.PACKAGE && type instanceof NoType) {
      return type;
    }
    throw new IllegalArgumentException("type: " + type);
  }

}
