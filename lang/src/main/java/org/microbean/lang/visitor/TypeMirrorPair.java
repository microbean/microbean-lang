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
package org.microbean.lang.visitor;

import java.util.List;
import java.util.Objects;

import javax.lang.model.element.Element;
import javax.lang.model.element.Name;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import org.microbean.lang.type.Types;

import static org.microbean.lang.type.Types.asElement;

final class TypeMirrorPair {

  private final Types types;
  
  private final IsSameTypeVisitor isSameTypeVisitor;
  
  private final TypeMirror t;
  
  private final TypeMirror s;

  TypeMirrorPair(final Types types,
                 final IsSameTypeVisitor isSameTypeVisitor,
                 final TypeMirror t,
                 final TypeMirror s) {
    super();
    this.types = Objects.requireNonNull(types, "types");
    this.isSameTypeVisitor = Objects.requireNonNull(isSameTypeVisitor, "isSameTypeVisitor");
    this.t = Objects.requireNonNull(t, "t");
    this.s = Objects.requireNonNull(s, "s");
  }

  @Override
  public final int hashCode() {
    return 127 * hashCode(this.t) + hashCode(this.s);
  }

  @Override
  public final boolean equals(final Object other) {
    if (this == other) {
      return true;
    } else if (other != null && this.getClass() == other.getClass()) {
      final TypeMirrorPair her = (TypeMirrorPair)other;
      return
        this.isSameTypeVisitor.visit(this.t, her.t) &&
        this.isSameTypeVisitor.visit(this.s, her.s);
    } else {
      return false;
    }
  }

  private final int hashCode(final TypeMirror t) {
    if (t == null) {
      return 0;
    }
    final TypeKind tk = t.getKind();
    switch (tk) {
    case ARRAY:
      return this.hashCode((ArrayType)t);
    case DECLARED:
    case INTERSECTION:
      // return this.hashCode0(t);
      int result = 127 * hashCode(enclosingType(t));
      final Element e = asElement(t, true);
      if (e != null) {
        result += flatName(e).hashCode();
      }
      for (final TypeMirror typeArgument : typeArguments(t)) {
        result = 127 * result + hashCode(typeArgument);
      }
      return result;
    case EXECUTABLE:
      return this.hashCode((ExecutableType)t);
    case TYPEVAR:
      return this.hashCode((TypeVariable)t);
    case WILDCARD:
      return this.hashCode((WildcardType)t);
    default:
      return tk.ordinal();
    }
  }

  private final int hashCode(final ArrayType t) {
    assert t.getKind() == TypeKind.ARRAY;
    return hashCode(t.getComponentType()) + 12;
  }

  // Involved:
  // * kind
  // * parameter types
  // * return type
  private final int hashCode(final ExecutableType t) {
    final TypeKind tk = t.getKind();
    assert tk == TypeKind.EXECUTABLE;
    int result = tk.ordinal();
    for (final TypeMirror pt : t.getParameterTypes()) {
      result = (result << 5) + hashCode(pt);
    }
    return (result << 5) + hashCode(t.getReturnType());
  }

  private final int hashCode(final TypeVariable t) {
    assert t.getKind() == TypeKind.TYPEVAR;
    // TODO: it sure would be nice if we could use Equality.hashCode(t) here; maybe we can?
    return System.identityHashCode(t);
  }

  // https://github.com/openjdk/jdk/blob/jdk-20+16/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L4204-L4212
  private final int hashCode(final WildcardType t) {
    assert t.getKind() == TypeKind.WILDCARD;
    // javac's Types#HashcodeVisitor starts with the hashcode of the wildcard's BoundKind
    // (https://github.com/openjdk/jdk/blob/jdk-20+17/src/jdk.compiler/share/classes/com/sun/tools/javac/code/BoundKind.java).
    //
    // BoundKind is:
    //
    // public enum BoundKind {
    //  EXTENDS,
    //  SUPER,
    //  UNBOUND
    // }
    //
    // ...so doesn't override the default enum hashcode calculation.  Each constant in the enum therefore has a constant
    // hashcode value of System.identityHashCode(CONSTANT).  We'll use 0, 1 and 2 instead.
    //
    // Other odd things:
    //
    // * the wildcard type denoted by ? extends Object has a different hashcode according to Types#HashcodeVisitor than
    //   does ?.  Is this deliberate? does it matter? not sure.
    final TypeMirror superBound = t.getSuperBound();
    final TypeMirror extendsBound = t.getExtendsBound();
    final int result;
    if (superBound == null) {
      result = extendsBound == null ? 254 /* 2 (UNBOUND) * 127 */ : hashCode(extendsBound) /* 0 (EXTENDS) * 127 + hashCode(extendsBound) */;
    } else if (extendsBound == null) {
      result = 127 + hashCode(superBound); // i.e. 1 (SUPER) * 127 + hashCode(superBound)
    } else {
      throw new IllegalArgumentException("t: " + t);
    }
    return result;
  }

  private static final TypeMirror enclosingType(final TypeMirror t) {
    switch (t.getKind()) {
    case DECLARED:
      return ((DeclaredType)t).getEnclosingType();
    default:
      return org.microbean.lang.type.NoType.NONE;
    }
  }

  private static final Name flatName(final Element e) {
    /*
        // form a fully qualified name from a name and an owner, after
        // converting to flat representation
        public static Name formFlatName(Name name, Symbol owner) {
            if (owner == null || owner.kind.matches(KindSelector.VAL_MTH) ||
                (owner.kind == TYP && owner.type.hasTag(TYPEVAR))
                ) return name;
            char sep = owner.kind == TYP ? '$' : '.';
            Name prefix = owner.flatName();
            if (prefix == null || prefix == prefix.table.names.empty)
                return name;
            else return prefix.append(sep, name);
        }
    */
    final Element enclosingElement = e.getEnclosingElement();
    if (enclosingElement == null) {
      return e.getSimpleName();
    }
    char separator;
    switch (enclosingElement.getKind()) {
    case ANNOTATION_TYPE:
    case CLASS:
    case ENUM:
    case INTERFACE:
    case RECORD:
      switch (enclosingElement.asType().getKind()) {
      case TYPEVAR:
        return e.getSimpleName();
      default:
        separator = '$';
        break;
      }
      break;
    case CONSTRUCTOR:
    case METHOD:
    case BINDING_VARIABLE:
    case ENUM_CONSTANT:
    case EXCEPTION_PARAMETER:
    case FIELD:
    case LOCAL_VARIABLE:
    case PARAMETER:
    case RESOURCE_VARIABLE:
      return e.getSimpleName();
    default:
      separator = '.';
      break;
    }
    final Name prefix = flatName(enclosingElement); // RECURSIVE
    if (prefix == null || prefix.isEmpty()) {
      return e.getSimpleName();
    }
    return org.microbean.lang.element.Name.of(new StringBuilder(prefix).append(separator).append(e.getSimpleName()).toString());
  }

  private static final List<? extends TypeMirror> typeArguments(final TypeMirror t) {
    return switch (t.getKind()) {
    case DECLARED -> ((DeclaredType)t).getTypeArguments();
    default -> List.of();
    };
  }
  
}
