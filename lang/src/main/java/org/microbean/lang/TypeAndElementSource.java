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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.lang.model.element.TypeElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

public interface TypeAndElementSource {

  public default ArrayType arrayType(final Class<?> arrayClass) {
    return arrayClass == null || !arrayClass.isArray() ? null : arrayTypeOf(type(arrayClass.getComponentType()));
  }

  public default ArrayType arrayType(final GenericArrayType g) {
    return g == null ? null : arrayTypeOf(type(g.getGenericComponentType()));
  }

  public ArrayType arrayTypeOf(final TypeMirror componentType);

  public boolean assignable(final TypeMirror payload, final TypeMirror receiver);

  public TypeElement boxedClass(final PrimitiveType t);

  public default DeclaredType declaredType(final CharSequence canonicalName) {
    return this.declaredType(this.typeElement(canonicalName));
  }

  public default DeclaredType declaredType(final CharSequence moduleName, final CharSequence canonicalName) {
    return this.declaredType(this.typeElement(moduleName, canonicalName));
  }

  public default DeclaredType declaredType(final Class<?> c) {
    return c == null ? null : this.declaredType(this.declaredType(c.getEnclosingClass()), typeElement(c));
    // return m == null ? this.declaredType(c.getCanonicalName()) : this.declaredType(m.getName(), c.getCanonicalName());
  }

  public DeclaredType declaredType(final DeclaredType enclosingType,
                                   final TypeElement typeElement,
                                   final TypeMirror... typeArguments);

  public default DeclaredType declaredType(final ParameterizedType pt) {
    return pt == null ? null : 
      this.declaredType(this.declaredType(pt.getOwnerType()),
                        this.typeElement(pt.getRawType()),
                        typeArray(pt.getActualTypeArguments()));
  }

  public default DeclaredType declaredType(final Type t) {
    return switch (t) {
    case null -> null;
    case Class<?> c -> declaredType(c);
    case ParameterizedType p -> declaredType(p);
    default -> null;
    };
  }

  public DeclaredType declaredType(final TypeElement typeElement, final TypeMirror... typeArguments);

  public <T extends TypeMirror> T erasure(final T t);

  public NoType noType(final TypeKind k);

  public default PrimitiveType primitiveType(final Class<?> c) {
    if (c == null || !c.isPrimitive()) {
      return null;
    }
    return primitiveType(TypeKind.valueOf(c.getName().toUpperCase()));
  }

  public PrimitiveType primitiveType(final TypeKind k);

  public boolean sameType(final TypeMirror t, final TypeMirror s);

  public default TypeMirror type(final Type t) {
    return switch (t) {
    case null -> null;
    case Class<?> c when c == void.class -> noType(TypeKind.VOID);
    case Class<?> c when c.isArray() -> arrayType(c);
    case Class<?> c when c.isPrimitive() -> primitiveType(c);
    case Class<?> c -> declaredType(declaredType(typeElement(c.getEnclosingClass())), typeElement(c));
    case ParameterizedType pt -> declaredType((DeclaredType)type(pt.getOwnerType()), typeElement((Class<?>)pt.getRawType()), typeArray(pt.getActualTypeArguments()));
    case GenericArrayType g -> arrayType(g);
    case java.lang.reflect.TypeVariable<?> tv -> typeVariable(tv);
    case java.lang.reflect.WildcardType w -> wildcardType(w);
    default -> null;
    };
  }

  public default TypeMirror[] typeArray(final Type[] ts) {
    if (ts == null || ts.length <= 0) {
      return new TypeMirror[0];
    }
    final TypeMirror[] rv = new TypeMirror[ts.length];
    for (int i = 0; i < ts.length; i++) {
      rv[i] = type(ts[i]);
    }
    return rv;
  }

  public default TypeElement typeElement(final CharSequence canonicalName) {
    return this.typeElement(null, canonicalName);
  }

  public TypeElement typeElement(final CharSequence moduleName, final CharSequence canonicalName);

  public default TypeElement typeElement(final Class<?> c) {
    final Module m = c.getModule();
    return m == null ? this.typeElement(c.getCanonicalName()) : this.typeElement(m.getName(), c.getCanonicalName());
  }

  public default TypeElement typeElement(final Type t) {
    return switch (t) {
    case null -> null;
    case Class<?> c -> typeElement(c);
    case ParameterizedType pt -> typeElement(pt.getRawType());
    default -> null;
    };
  }

  public TypeVariable typeVariable(final java.lang.reflect.TypeVariable<?> t);

  public WildcardType wildcardType(TypeMirror extendsBound, TypeMirror superBound);

  public default WildcardType wildcardType(final java.lang.reflect.WildcardType t) {
    if (t == null) {
      return null;
    }
    final Type[] lowerBounds = t.getLowerBounds();
    final Type lowerBound = lowerBounds.length <= 0 ? null : lowerBounds[0];
    final Type upperBound = t.getUpperBounds()[0];
    return wildcardType(type(upperBound), type(lowerBound));
  }

}
