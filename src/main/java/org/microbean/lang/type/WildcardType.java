/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2022 microBean™.
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
package org.microbean.lang.type;

import java.util.List;

import javax.lang.model.element.AnnotationMirror;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;

public final class WildcardType extends TypeMirror implements javax.lang.model.type.WildcardType {


  /*
   * Instance fields.
   */


  private javax.lang.model.type.TypeMirror extendsBound;

  private javax.lang.model.type.TypeMirror superBound;


  /*
   * Constructors.
   */


  public WildcardType() {
    super(TypeKind.WILDCARD);
  }
  
  public WildcardType(final javax.lang.model.type.TypeMirror extendsBound) {
    this();
    this.setExtendsBound(extendsBound);
  }
  
  public WildcardType(final javax.lang.model.type.TypeMirror extendsBound, final javax.lang.model.type.TypeMirror superBound) {
    this();
    this.setExtendsBound(extendsBound);
    this.setSuperBound(superBound);
  }


  /*
   * Instance methods.
   */


  @Override // TypeMirror
  public final <R, P> R accept(final TypeVisitor<R, P> v, P p) {
    return v.visitWildcard(this, p);
  }

  @Override // WildcardType
  public final javax.lang.model.type.TypeMirror getExtendsBound() {
    return this.extendsBound;
  }

  public final void setExtendsBound(final javax.lang.model.type.TypeMirror t) {
    final Object old = this.getExtendsBound();
    if (old == null) {
      if (t != null) {
        this.extendsBound = validateExtendsBound(t);
      }
    } else if (old != t) {
      throw new IllegalStateException();
    }
  }

  @Override // WildcardType
  public final javax.lang.model.type.TypeMirror getSuperBound() {
    return this.superBound;
  }

  public final void setSuperBound(final javax.lang.model.type.TypeMirror t) {
    final Object old = this.getSuperBound();
    if (old == null) {
      if (t != null) {
        this.superBound = validateSuperBound(t);
      }
    } else if (old != t) {
      throw new IllegalStateException();
    }
  }

  private final javax.lang.model.type.TypeMirror validateExtendsBound(final javax.lang.model.type.TypeMirror extendsBound) {
    if (extendsBound == this) {
      throw new IllegalArgumentException("extendsBound: " + extendsBound);
    }
    if (extendsBound != null) {
      switch (extendsBound.getKind()) {
        // See
        // https://docs.oracle.com/javase/specs/jls/se18/html/jls-4.html#jls-WildcardBounds
        // and
        // https://docs.oracle.com/javase/specs/jls/se18/html/jls-4.html#jls-ReferenceType
      case ARRAY:
      case DECLARED:
      case TYPEVAR:
        break;
      default:
        throw new IllegalArgumentException("extendsBound: " + extendsBound);
      }
    }
    return extendsBound;
  }

  private final javax.lang.model.type.TypeMirror validateSuperBound(final javax.lang.model.type.TypeMirror superBound) {
    if (superBound == this) {
      throw new IllegalArgumentException("superBound: " + superBound);
    }
    if (superBound != null) {
      switch (superBound.getKind()) {
        // See
        // https://docs.oracle.com/javase/specs/jls/se18/html/jls-4.html#jls-WildcardBounds
        // and
        // https://docs.oracle.com/javase/specs/jls/se18/html/jls-4.html#jls-ReferenceType
      case ARRAY:
      case DECLARED:
      case TYPEVAR:
        break;
      default:
        throw new IllegalArgumentException("superBound: " + superBound);
      }
    }
    return superBound;
  }

  public static WildcardType upperBoundedWildcardType(final javax.lang.model.type.TypeMirror extendsBound) {
    return new WildcardType(extendsBound, null);
  }

  public static WildcardType lowerBoundedWildcardType(final javax.lang.model.type.TypeMirror superBound) {
    return new WildcardType(null, superBound);
  }

  public static WildcardType unboundedWildcardType() {
    return new WildcardType(null, null);
  }  

  public static WildcardType of(final javax.lang.model.type.TypeMirror extendsBound,
                                final javax.lang.model.type.TypeMirror superBound) {
    if (extendsBound == null) {
      if (superBound == null) {
        return unboundedWildcardType();
      } else {
        return lowerBoundedWildcardType(superBound);
      }
    } else if (superBound == null) {
      return upperBoundedWildcardType(extendsBound);
    } else {
      throw new IllegalArgumentException("extendsBound: " + extendsBound + "; superBound: " + superBound);
    }
  }

}
