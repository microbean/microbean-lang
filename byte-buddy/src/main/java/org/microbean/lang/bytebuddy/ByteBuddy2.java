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
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import javax.lang.model.util.Elements;

import net.bytebuddy.description.modifier.EnumerationState;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.modifier.SyntheticState;
import net.bytebuddy.description.modifier.TypeManifestation;
import net.bytebuddy.description.modifier.Visibility;

import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;

import net.bytebuddy.dynamic.DynamicType;

import net.bytebuddy.dynamic.scaffold.InstrumentedType;

import net.bytebuddy.pool.TypePool;

import static org.microbean.lang.Lang.origin;
import static org.microbean.lang.Lang.wrap;
import static org.microbean.lang.Lang.unwrap;

public final class ByteBuddy2 {

  private final net.bytebuddy.ByteBuddy bb;

  final TypePool typePool;
  
  final ConcurrentMap<String, TypeDescription> cache;
  
  public ByteBuddy2() {
    super();
    this.cache = new ConcurrentHashMap<>();
    this.typePool = new TypePool.LazyFacade(new TypePool.Explicit(this.cache));
    this.bb = new net.bytebuddy.ByteBuddy();
  }

  public final TypeDefinition typeDefinition(TypeMirror t) {
    t = wrap(t);
    return switch (t.getKind()) {
    case ARRAY -> throw new UnsupportedOperationException();
    case BOOLEAN -> TypeDescription.ForLoadedType.of(boolean.class);
    case BYTE -> TypeDescription.ForLoadedType.of(byte.class);
    case CHAR -> TypeDescription.ForLoadedType.of(char.class);
    case DECLARED -> this.typeDefinition((DeclaredType)t);
    case DOUBLE -> TypeDescription.ForLoadedType.of(double.class);
    case ERROR -> null;
    case EXECUTABLE -> throw new UnsupportedOperationException();
    case FLOAT -> TypeDescription.ForLoadedType.of(float.class);
    case INT -> TypeDescription.ForLoadedType.of(int.class);
    case INTERSECTION -> throw new UnsupportedOperationException();
    case LONG -> TypeDescription.ForLoadedType.of(long.class);
    case MODULE -> throw new UnsupportedOperationException();
    case NONE -> null;
    case NULL -> null;
    case OTHER -> null;
    case PACKAGE -> throw new UnsupportedOperationException();
    case SHORT -> TypeDescription.ForLoadedType.of(short.class);
    case TYPEVAR -> throw new UnsupportedOperationException();
    case UNION -> throw new UnsupportedOperationException();
    case VOID -> TypeDescription.ForLoadedType.of(void.class);
    case WILDCARD -> throw new UnsupportedOperationException();
    };
  }

  private final TypeDefinition typeDefinition(final DeclaredType t) {
    if (t.getKind() != TypeKind.DECLARED) {
      throw new IllegalArgumentException("t: " + t);
    }
    final TypeElement e = (TypeElement)t.asElement();
    if (e.getQualifiedName().contentEquals("java.lang.Object")) {
      return TypeDescription.ForLoadedType.of(Object.class);
    }
    throw new UnsupportedOperationException();
  }
    
  public final TypeList.Generic typeListGeneric(final Collection<? extends TypeMirror> ts) {
    final List<TypeDefinition> list = new ArrayList<>(ts.size());
    for (final TypeMirror t : ts) {
      list.add(typeDefinition(t));
    }
    return new TypeList.Generic.Explicit(list);
  }


  /*
   * Static methods.
   */

  
  private static final boolean generic(Element e) {
    // e = wrap(e);
    return e != null && e.getKind().isDeclaredType() && !((TypeElement)e).getTypeParameters().isEmpty();
  }
  
  private static final boolean generic(TypeMirror t) {
    // t = wrap(t);
    return t != null && t.getKind() == TypeKind.DECLARED && generic(((DeclaredType)t).asElement());
  }

  private static final Collection<ModifierContributor.ForType> typeModifiers(final TypeElement e) {
    final Set<? extends Modifier> modifiers = e.getModifiers();
    final List<ModifierContributor.ForType> builderModifiers = new ArrayList<>();

    // Handle EnumerationState.ENUMERATION.
    switch (e.getKind()) {
    case ENUM:
      builderModifiers.add(EnumerationState.ENUMERATION);
      break;
    default:
      builderModifiers.add(EnumerationState.PLAIN);
      break;
    }

    switch (e.getKind()) {
    case ANNOTATION_TYPE:
      builderModifiers.add(TypeManifestation.ANNOTATION);
      break;
    case INTERFACE:
      builderModifiers.add(TypeManifestation.INTERFACE);
      break;
    }

    if (origin(e) == Elements.Origin.SYNTHETIC) {
      builderModifiers.add(SyntheticState.SYNTHETIC);
    } else {
      builderModifiers.add(SyntheticState.PLAIN);
    }

    boolean abst = false;
    boolean fnl = false;
    boolean nested = false;
    for (final Modifier m : modifiers) {
      switch (m) {
      case ABSTRACT:
        abst = true;
        builderModifiers.add(TypeManifestation.ABSTRACT);
        break;
      case FINAL:
        fnl = true;
        builderModifiers.add(TypeManifestation.FINAL);
        break;
      case NON_SEALED:
        // Byte Buddy doesn't seem to account for this?
        break;
      case PRIVATE:
        builderModifiers.add(Visibility.PRIVATE);
        break;
      case PROTECTED:
        builderModifiers.add(Visibility.PROTECTED);
        break;
      case PUBLIC:
        builderModifiers.add(Visibility.PUBLIC);
        break;
      case SEALED:
        // Byte Buddy doesn't seem to account for this?
        break;
      case STATIC:
        nested = true;
        builderModifiers.add(Ownership.STATIC);
        break;
      default:
        throw new IllegalArgumentException("modifiers: " + modifiers + "; m: " + m);
      }

      if (!nested && e.getNestingKind() != NestingKind.TOP_LEVEL) {
        // It's local, anonymous or a member; Byte Buddy collapses these
        builderModifiers.add(Ownership.MEMBER);
      }

      if (!abst && !fnl) {
        builderModifiers.add(TypeManifestation.PLAIN);
      }
    }
    return Set.copyOf(builderModifiers);
  }


  private static final <T> DynamicType.Builder<T> typeModifiers(DynamicType.Builder<T> builder, final TypeElement e) {
    final Set<? extends Modifier> modifiers = e.getModifiers();
    final List<ModifierContributor.ForType> builderModifiers = new ArrayList<>();

    // Handle EnumerationState.ENUMERATION.
    switch (e.getKind()) {
    case ENUM:
      builderModifiers.add(EnumerationState.ENUMERATION);
      break;
    default:
      builderModifiers.add(EnumerationState.PLAIN);
      break;
    }

    switch (e.getKind()) {
    case ANNOTATION_TYPE:
      builderModifiers.add(TypeManifestation.ANNOTATION);
      break;
    case INTERFACE:
      builderModifiers.add(TypeManifestation.INTERFACE);
      break;
    }

    if (origin(e) == Elements.Origin.SYNTHETIC) {
      builderModifiers.add(SyntheticState.SYNTHETIC);
    } else {
      builderModifiers.add(SyntheticState.PLAIN);
    }

    boolean abst = false;
    boolean fnl = false;
    boolean nested = false;
    for (final Modifier m : modifiers) {
      switch (m) {
      case ABSTRACT:
        abst = true;
        builderModifiers.add(TypeManifestation.ABSTRACT);
        break;
      case FINAL:
        fnl = true;
        builderModifiers.add(TypeManifestation.FINAL);
        break;
      case NON_SEALED:
        // Byte Buddy doesn't seem to account for this?
        break;
      case PRIVATE:
        builderModifiers.add(Visibility.PRIVATE);
        break;
      case PROTECTED:
        builderModifiers.add(Visibility.PROTECTED);
        break;
      case PUBLIC:
        builderModifiers.add(Visibility.PUBLIC);
        break;
      case SEALED:
        // Byte Buddy doesn't seem to account for this?
        break;
      case STATIC:
        nested = true;
        builderModifiers.add(Ownership.STATIC);
        break;
      default:
        throw new IllegalArgumentException("modifiers: " + modifiers + "; m: " + m);
      }

      if (!nested && e.getNestingKind() != NestingKind.TOP_LEVEL) {
        // It's local, anonymous or a member; Byte Buddy collapses these
        builderModifiers.add(Ownership.MEMBER);
      }

      if (!abst && !fnl) {
        builderModifiers.add(TypeManifestation.PLAIN);
      }
    }
    return builder.modifiers(builderModifiers);
  }
  
}
