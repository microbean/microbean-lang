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
package org.microbean.lang.bytebuddy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.lang.model.element.Name;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;

import net.bytebuddy.pool.TypePool;

import org.microbean.lang.CompletionLock;

import static net.bytebuddy.description.type.TypeDescription.Generic.Builder;

import static org.microbean.lang.Lang.generic;

public final class ByteBuddy2 {

  private final TypePool typePool;

  public ByteBuddy2(final TypePool typePool) {
    super();
    this.typePool = Objects.requireNonNull(typePool, "typePool");
  }

  public final TypePool typePool() {
    return this.typePool;
  }

  public final TypeDescription typeDescription(final TypeMirror t) {
    if (t == null) {
      return null;
    }
    CompletionLock.acquire();
    try {
      return switch (t.getKind()) {
      case NONE -> null;

      case BOOLEAN -> TypeDefinition.Sort.describe(boolean.class).asErasure();
      case BYTE -> TypeDefinition.Sort.describe(byte.class).asErasure();
      case CHAR -> TypeDefinition.Sort.describe(char.class).asErasure();
      case DOUBLE -> TypeDefinition.Sort.describe(double.class).asErasure();
      case FLOAT -> TypeDefinition.Sort.describe(float.class).asErasure();
      case INT -> TypeDefinition.Sort.describe(int.class).asErasure();
      case LONG -> TypeDefinition.Sort.describe(long.class).asErasure();
      case SHORT -> TypeDefinition.Sort.describe(short.class).asErasure();

      case VOID -> TypeDefinition.Sort.describe(void.class).asErasure();

      case ARRAY -> TypeDescription.ArrayProjection.of(typeDescription(((ArrayType)t).getComponentType()));

      case DECLARED -> typeDescription((QualifiedNameable)((DeclaredType)t).asElement());

      default -> throw new IllegalArgumentException("t: " + t + "; kind: " + t.getKind());
      };
    } finally {
      CompletionLock.release();
    }
  }

  public final TypeDescription typeDescription(final QualifiedNameable qn) {
    return qn == null ? null : this.typeDescription(qn.getQualifiedName().toString());
  }

  public final TypeDescription typeDescription(final String name) { // TODO: better named name; maybe canonical name following JLS?
    return name == null ? null : this.typePool.describe(name).resolve();
  }

  public final TypeDescription.Generic typeDescriptionGeneric(final TypeMirror t) {
    if (t == null) {
      return null;
    }
    CompletionLock.acquire();
    try {
      return switch (t.getKind()) {

      case NONE -> null;

        // Primitives are easy. ByteBuddy caches them.
      case BOOLEAN -> TypeDefinition.Sort.describe(boolean.class);
      case BYTE -> TypeDefinition.Sort.describe(byte.class);
      case CHAR -> TypeDefinition.Sort.describe(char.class);
      case DOUBLE -> TypeDefinition.Sort.describe(double.class);
      case FLOAT -> TypeDefinition.Sort.describe(float.class);
      case INT -> TypeDefinition.Sort.describe(int.class);
      case LONG -> TypeDefinition.Sort.describe(long.class);
      case SHORT -> TypeDefinition.Sort.describe(short.class);

      // void is easy.
      case VOID -> TypeDefinition.Sort.describe(void.class);

      // Arrays are easy.
      case ARRAY -> Builder.of(typeDescriptionGeneric(((ArrayType)t).getComponentType())).asArray().build();

      // Certain declared types are easy because ByteBuddy caches them, so the classes are loaded already. We add a few
      // simple ones to the list.
      case DECLARED -> {
        final DeclaredType dt = (DeclaredType)t;
        final TypeElement te = (TypeElement)dt.asElement();
        final String n = te.getQualifiedName().toString();
        yield switch (n) {
        case "java.lang.Boolean" -> TypeDefinition.Sort.describe(Boolean.class);
        case "java.lang.Byte" -> TypeDefinition.Sort.describe(Byte.class);
        case "java.lang.Character" -> TypeDefinition.Sort.describe(Character.class);
        case "java.lang.Class" -> TypeDefinition.Sort.describe(Class.class);
        case "java.lang.Double" -> TypeDefinition.Sort.describe(Double.class);
        case "java.lang.Float" -> TypeDefinition.Sort.describe(Float.class);
        case "java.lang.Integer" -> TypeDefinition.Sort.describe(Integer.class);
        case "java.lang.Long" -> TypeDefinition.Sort.describe(Long.class);
        case "java.lang.Object" -> TypeDefinition.Sort.describe(Object.class);
        case "java.lang.Short" -> TypeDefinition.Sort.describe(Short.class);
        case "java.lang.String" -> TypeDefinition.Sort.describe(String.class);
        case "java.lang.Throwable" -> TypeDefinition.Sort.describe(Throwable.class);
        // case "java.lang.Void" -> TypeDefinition.Sort.describe(Void.class); // ByteBuddy doesn't cache this for some reason
        case "java.lang.annotation.Annotation" -> TypeDefinition.Sort.describe(java.lang.annotation.Annotation.class);
        case "net.bytebuddy.dynamic.TargetType" -> TypeDefinition.Sort.describe(net.bytebuddy.dynamic.TargetType.class);
        default -> {
          // Some other declared type
          final TypeDescription rawType = typeDescription(n);
          if (generic(te)) {
            final TypeDescription.Generic enclosingType = typeDescriptionGeneric(dt.getEnclosingType());
            final List<? extends TypeMirror> typeArgumentMirrors = dt.getTypeArguments();
            if (typeArgumentMirrors.isEmpty()) {
              yield Builder.parameterizedType(rawType, enclosingType == null ? TypeDescription.Generic.UNDEFINED : enclosingType).build();
            }
            final List<TypeDefinition> typeArguments = new ArrayList<>(typeArgumentMirrors.size());
            for (final TypeMirror typeArgumentMirror : typeArgumentMirrors) {
              typeArguments.add(typeDescriptionGeneric(typeArgumentMirror));
            }
            yield Builder.parameterizedType(rawType, enclosingType == null ? TypeDescription.Generic.UNDEFINED : enclosingType, typeArguments).build();
          } else {
            yield rawType.asGenericType();
          }
        }
        };
      }

      case TYPEVAR -> Builder.typeVariable(((TypeVariable)t).asElement().getSimpleName().toString()).build();

      case WILDCARD -> {
        final WildcardType w = (WildcardType)t;
        final TypeMirror extendsBound = w.getExtendsBound();
        final TypeMirror superBound = w.getSuperBound();
        if (superBound == null) {
          if (extendsBound == null) {
            yield Builder.unboundWildcard();
          }
          yield Builder.of(typeDescriptionGeneric(extendsBound)).asWildcardUpperBound();
        } else if (extendsBound == null) {
          yield Builder.of(typeDescriptionGeneric(superBound)).asWildcardLowerBound();
        } else {
          throw new AssertionError();
        }
      }

      case ERROR, EXECUTABLE, INTERSECTION, MODULE, NULL, OTHER, PACKAGE, UNION -> throw new IllegalArgumentException("t: " + t + "; kind: " + t.getKind());
      };
    } finally {
      CompletionLock.release();
    }
  }

}
