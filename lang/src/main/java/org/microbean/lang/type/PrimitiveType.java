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
package org.microbean.lang.type;

import javax.lang.model.element.AnnotationMirror;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;

public non-sealed class PrimitiveType extends TypeMirror implements javax.lang.model.type.PrimitiveType {


  /*
   * Static fields.
   */


  public static final PrimitiveType BOOLEAN = new PrimitiveType(TypeKind.BOOLEAN) {
      @Override
      public void addAnnotationMirror(final AnnotationMirror a) {
        throw new UnsupportedOperationException();
      }
    };

  public static final PrimitiveType BYTE = new PrimitiveType(TypeKind.BYTE) {
      @Override
      public void addAnnotationMirror(final AnnotationMirror a) {
        throw new UnsupportedOperationException();
      }
    };

  public static final PrimitiveType CHAR = new PrimitiveType(TypeKind.CHAR) {
      @Override
      public void addAnnotationMirror(final AnnotationMirror a) {
        throw new UnsupportedOperationException();
      }
    };

  public static final PrimitiveType DOUBLE = new PrimitiveType(TypeKind.DOUBLE) {
      @Override
      public void addAnnotationMirror(final AnnotationMirror a) {
        throw new UnsupportedOperationException();
      }
    };

  public static final PrimitiveType FLOAT = new PrimitiveType(TypeKind.FLOAT) {
      @Override
      public void addAnnotationMirror(final AnnotationMirror a) {
        throw new UnsupportedOperationException();
      }
    };

  public static final PrimitiveType INT = new PrimitiveType(TypeKind.INT) {
      @Override
      public void addAnnotationMirror(final AnnotationMirror a) {
        throw new UnsupportedOperationException();
      }
    };

  public static final PrimitiveType LONG = new PrimitiveType(TypeKind.LONG) {
      @Override
      public void addAnnotationMirror(final AnnotationMirror a) {
        throw new UnsupportedOperationException();
      }
    };

  public static final PrimitiveType SHORT = new PrimitiveType(TypeKind.SHORT) {
      @Override
      public void addAnnotationMirror(final AnnotationMirror a) {
        throw new UnsupportedOperationException();
      }
    };


  /*
   * Constructors.
   */


  public PrimitiveType(final TypeKind kind) {
    super(kind);
  }


  /*
   * Instance methods.
   */


  @Override // TypeMirror
  public final <R, P> R accept(final TypeVisitor<R, P> v, final P p) {
    return v.visitPrimitive(this, p);
  }

  @Override
  public final String toString() {
    return this.getKind().toString().toLowerCase();
  }

  @Override
  protected final TypeKind validateKind(final TypeKind kind) {
    switch (kind) {
    case BOOLEAN:
    case BYTE:
    case CHAR:
    case DOUBLE:
    case FLOAT:
    case INT:
    case LONG:
    case SHORT:
      return kind;
    default:
      throw new IllegalArgumentException("kind: " + kind);
    }
  }


  /*
   * Static methods.
   */


  public static PrimitiveType of(final javax.lang.model.type.TypeMirror t) {
    if (t instanceof PrimitiveType p) {
      return p;
    }
    return of(t.getKind());
  }

  public static PrimitiveType of(final TypeKind kind) {
    switch (kind) {
    case BOOLEAN:
      return BOOLEAN;
    case BYTE:
      return BYTE;
    case CHAR:
      return CHAR;
    case DOUBLE:
      return DOUBLE;
    case FLOAT:
      return FLOAT;
    case INT:
      return INT;
    case LONG:
      return LONG;
    case SHORT:
      return SHORT;
    default:
      throw new IllegalArgumentException("kind: " + kind);
    }
  }

}
