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

import java.lang.annotation.Annotation;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;

import javax.lang.model.type.TypeKind;

import org.microbean.lang.ElementSource;

public final class Types {

  private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

  // @see #asElement(TypeMirror, boolean)
  // @GuardedBy("itself")
  private static final WeakHashMap<javax.lang.model.type.TypeMirror, javax.lang.model.element.Element> syntheticElements = new WeakHashMap<>();

  private final ElementSource es;

  public Types(final ElementSource elementSource) {
    super();
    this.es = Objects.requireNonNull(elementSource, "elementSource");
  }

  public final javax.lang.model.type.TypeMirror extendsBound(final javax.lang.model.type.TypeMirror t) {
    // javac's "wildUpperBound".
    // See
    // https://github.com/openjdk/jdk/blob/jdk-20+12/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L130-L143
    switch (t.getKind()) {
    case WILDCARD:
      final javax.lang.model.type.WildcardType w = (javax.lang.model.type.WildcardType)t;
      javax.lang.model.type.TypeMirror superBound = w.getSuperBound();
      if (superBound == null) {
        // Unbounded or upper-bounded.
        final javax.lang.model.type.TypeMirror extendsBound = w.getExtendsBound();
        if (extendsBound == null) {
          // Unbounded, so upper bound is Object.
          return this.es.element("java.lang.Object").asType();
        } else {
          // Upper-bounded.
          assert
            extendsBound.getKind() == TypeKind.ARRAY ||
            extendsBound.getKind() == TypeKind.DECLARED ||
            extendsBound.getKind() == TypeKind.TYPEVAR :
          "extendsBound kind: " + extendsBound.getKind();
          return extendsBound;
        }
      } else {
        // See
        // https://github.com/openjdk/jdk/blob/jdk-20+12/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L138.
        // A (javac) WildcardType's bound field is NOT the same as its type field.  The lang model only exposes the type
        // field.
        //
        // Consider some context like this:
        //
        //   interface Foo<T extends Serializable> {}
        //
        // And then a wildcard like this:
        //
        //   Foo<? super String> f;
        //
        // The (javac) WildcardType's bound field will be initialized to T extends Serializable.  Its type field will be
        // initialized to String.  wildUpperBound(thisWildcardType) will return Serializable.class, not Object.class.
        //
        // The lang model makes this impossible, because bound is not exposed, and getSuperBound() doesn't do anything
        // fancy to return it.
        //
        // Dan Smith writes:
        //
        // "It turns out 'bound' is used to represent the *corresponding type parameter* of the wildcard, where
        // additional bounds can be found (for things like capture conversion), not anything about the wildcard itself."
        //
        // And:
        //
        // "Honestly, it's a little sketchy that the compiler internals are doing this at all. I'm not totally sure
        // that, for example, uses of 'wildUpperBound' aren't violating the language spec somewhere. For lang.model, no,
        // the 'bound' field is not at all part of the specified API, so shouldn't have any impact on API behavior.
        //
        // "(The right thing to do, per the language spec, to incorporate the corresponding type parameter bounds, is to
        // perform capture on the wildcard's enclosing parameterized type, and then work with the resulting capture type
        // variables.)"
        //
        // So bound gets set to T extends Serializable.  There is no way to extract T extends Serializable from a
        // javax.lang.model.type.WildcardType, and without that ability we have no other information, so we must return
        // Object.class.
        return this.es.element("java.lang.Object").asType();
      }
    default:
      return t;
    }
  }

  public final javax.lang.model.type.TypeMirror superBound(final javax.lang.model.type.TypeMirror t) {
    // See
    // https://github.com/openjdk/jdk/blob/jdk-20+12/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L157-L167
    return switch (t.getKind()) {
    case WILDCARD -> {
      final javax.lang.model.type.TypeMirror superBound = ((javax.lang.model.type.WildcardType)t).getSuperBound();
      yield superBound == null ? org.microbean.lang.type.NullType.INSTANCE : superBound;
    }
    default -> t;
    };
  }

  // Not visitor-based in javac
  public static final List<? extends javax.lang.model.type.TypeMirror> allTypeArguments(final javax.lang.model.type.TypeMirror t) {
    return t == null ? List.of() : switch (t.getKind()) {
    case ARRAY -> allTypeArguments(((javax.lang.model.type.ArrayType)t).getComponentType()); // RECURSIVE
    case DECLARED -> allTypeArguments((javax.lang.model.type.DeclaredType)t);
    default -> List.of();
    };
  }

  // com.foo.Foo<Bar>.Baz<String> will yield [Bar, String] when handed the DeclaredType denoted by Baz<String>
  public static final List<? extends javax.lang.model.type.TypeMirror> allTypeArguments(final javax.lang.model.type.DeclaredType t) {
    if (t == null) {
      return List.of();
    } else if (t.getKind() != TypeKind.DECLARED) {
      throw new IllegalArgumentException("t: " + t);
    }
    final List<? extends javax.lang.model.type.TypeMirror> enclosingTypeTypeArguments = allTypeArguments(t.getEnclosingType()); // RECURSIVE
    final List<? extends javax.lang.model.type.TypeMirror> typeArguments = t.getTypeArguments();
    if (enclosingTypeTypeArguments.isEmpty()) {
      return typeArguments.isEmpty() ? List.of() : typeArguments;
    } else if (typeArguments.isEmpty()) {
      return enclosingTypeTypeArguments;
    } else {
      final List<javax.lang.model.type.TypeMirror> list = new ArrayList<>(enclosingTypeTypeArguments.size() + typeArguments.size());
      list.addAll(enclosingTypeTypeArguments);
      list.addAll(typeArguments);
      return Collections.unmodifiableList(list);
    }
  }

  public static final javax.lang.model.element.Element asElement(final javax.lang.model.type.TypeMirror t, final boolean generateSyntheticElements) {
    // TypeMirror#asElement() says:
    //
    //   "Returns the element corresponding to a type. The type may be a DeclaredType or TypeVariable. Returns null if
    //   the type is not one with a corresponding element."
    //
    // This does not correspond at *all* to the innards of javac, where nearly every type has an associated element,
    // even where it makes no sense.  For example, error types, intersection types, executable types (!), primitive
    // types and wildcard types (which aren't even types!) all have elements somehow, but these are not in the lang
    // model.
    //
    // Although, see
    // https://github.com/openjdk/jdk/blob/jdk-20%2B12/src/jdk.compiler/share/classes/com/sun/tools/javac/model/JavacTypes.java#L76,
    // which *is* in the lang model and will return an element for an intersection type.  What a mess.
    //
    // Much of javac's algorithmic behavior is based on most types having elements, even where the elements make no
    // sense.  In fact, the only types in javac that have no elements at all are:
    //
    // * no type
    // * void type
    // * null type
    // * unknown type
    //
    // Symbols in javac do not override their equals()/hashCode() methods, so no two symbols are ever the same.
    //
    // We blend all these facts together and set up synthetic elements for types that, in the lang model, don't have
    // them, but do have them behind the scenes in javac.  We use a WeakHashMap to associate TypeMirror instances with
    // their synthetic elements.
    if (t == null) {
      return null;
    }
    switch (t.getKind()) {

    case DECLARED:
      return ((javax.lang.model.type.DeclaredType)t).asElement();

    case TYPEVAR:
      return ((javax.lang.model.type.TypeVariable)t).asElement();

    case ARRAY:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler uses exactly one synthetic element for all
          // array types.
          return syntheticElements.computeIfAbsent(t, arrayType -> SyntheticArrayElement.INSTANCE);
        }
      }
      return null;

    case EXECUTABLE:
      // This is really problematic.  There *is* an ExecutableElement in the lang model, and an ExecutableType, but they
      // aren't related in the way that, say, DeclaredType and TypeElement are. javac seems to use a singleton synthetic
      // ClassType (!)  for all method symbols.  I'm not sure what to do here.  I'm going to leave it null for now.
      /*
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          return syntheticElements.computeIfAbsent(t, s -> SyntheticExecutableElement.INSTANCE);
        }
      }
      */
      return null;

    case WILDCARD:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for all wildcard types.
          return syntheticElements.computeIfAbsent(t, wildcardType -> SyntheticWildcardElement.INSTANCE);
        }
      }
      return null;

    case BOOLEAN:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for a given kind of primitive type.
          return syntheticElements.computeIfAbsent(t, booleanType -> SyntheticPrimitiveElement.BOOLEAN);
        }
      }
      return null;

    case BYTE:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for a given kind of primitive type.
          return syntheticElements.computeIfAbsent(t, byteType -> SyntheticPrimitiveElement.BYTE);
        }
      }
      return null;

    case CHAR:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for a given kind of primitive type.
          return syntheticElements.computeIfAbsent(t, charType -> SyntheticPrimitiveElement.CHAR);
        }
      }
      return null;

    case DOUBLE:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for a given kind of primitive type.
          return syntheticElements.computeIfAbsent(t, doubleType -> SyntheticPrimitiveElement.DOUBLE);
        }
      }
      return null;

    case FLOAT:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for a given kind of primitive type.
          return syntheticElements.computeIfAbsent(t, floatType -> SyntheticPrimitiveElement.FLOAT);
        }
      }
      return null;

    case INT:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for a given kind of primitive type.
          return syntheticElements.computeIfAbsent(t, intType -> SyntheticPrimitiveElement.INT);
        }
      }
      return null;

    case LONG:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for a given kind of primitive type.
          return syntheticElements.computeIfAbsent(t, longType -> SyntheticPrimitiveElement.LONG);
        }
      }
      return null;

    case SHORT:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler users exactly one synthetic element for a given kind of primitive type.
          return syntheticElements.computeIfAbsent(t, shortType -> SyntheticPrimitiveElement.SHORT);
        }
      }
      return null;

    case INTERSECTION:
    case MODULE:
    case PACKAGE:
      if (generateSyntheticElements) {
        synchronized (syntheticElements) {
          // The compiler uses one instance of a bogus element for each instance of one of these types.
          return syntheticElements.computeIfAbsent(t, SyntheticElement::new);
        }
      }
      return null;

    case OTHER:
    case NONE:
    case NULL:
    case VOID:
      return null;

    case ERROR:
    case UNION:
    default:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  // Return the javax.lang.model.type.TypeMirror representing the declaration whose type may currently be being used.
  // E.g. given a type denoted by List<String>, return the type denoted by List<E> (from List<String>'s usage of List<E>)
  //
  // If it is passed something funny, it just returns what it was passed instead. The compiler does this a lot and I
  // think it's confusing.
  //
  // I don't like this name.
  @SuppressWarnings("unchecked")
  public static final <T extends javax.lang.model.type.TypeMirror> T typeDeclaration(final T t) {
    final javax.lang.model.element.Element e = asElement(t, false /* don't generate synthetic elements */);
    return e == null ? t : (T)e.asType();
  }

  public static final boolean hasTypeArguments(final javax.lang.model.type.TypeMirror t) {
    // This is modeled after javac's allparams() method.  javac frequently confuses type parameters and type arguments
    // in its terminology.  This implementation could probably be made more efficient. See
    // https://github.com/openjdk/jdk/blob/67ecd30327086c5d7628c4156f8d9dcccb0f4d09/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Type.java#L1137
    return switch (t.getKind()) {
    case ARRAY, DECLARED -> !allTypeArguments(t).isEmpty();
    default -> false;
    };
  }

  public static final boolean isInterface(final javax.lang.model.element.Element e) {
    return e.getKind().isInterface();
  }

  public static final boolean isInterface(final javax.lang.model.type.TypeMirror t) {
    return t.getKind() == TypeKind.DECLARED && ((javax.lang.model.type.DeclaredType)t).asElement().getKind().isInterface();
  }

  public static final boolean isStatic(final javax.lang.model.element.Element e) {
    return e.getModifiers().contains(javax.lang.model.element.Modifier.STATIC);
  }

  // See
  // https://github.com/openjdk/jdk/blob/67ecd30327086c5d7628c4156f8d9dcccb0f4d09/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Type.java#L1154-L1164
  public final boolean raw(final javax.lang.model.type.TypeMirror t) {
    return switch (t.getKind()) {
    case ARRAY -> raw(((ArrayType)t).getComponentType());
    case DECLARED -> {
      final javax.lang.model.type.TypeMirror typeDeclaration = typeDeclaration(t);
      yield
        t != typeDeclaration && // t is a parameterized type, i.e. a type usage, and
        hasTypeArguments(typeDeclaration) && // the type it parameterizes has type arguments (type variables declared by type parameters) and
        !hasTypeArguments(t); // t does not supply type arguments
    }
    default -> false;
    };
  }

  public static final javax.lang.model.type.WildcardType unboundedWildcardType() {
    return new org.microbean.lang.type.WildcardType();
  }

  public static final javax.lang.model.type.WildcardType unboundedWildcardType(final List<? extends AnnotationMirror> annotationMirrors) {
    org.microbean.lang.type.WildcardType t = new org.microbean.lang.type.WildcardType();
    t.addAnnotationMirrors(annotationMirrors);
    return t;
  }

  public static final javax.lang.model.type.WildcardType upperBoundedWildcardType(final javax.lang.model.type.TypeMirror upperBound,
                                                                                  final List<? extends AnnotationMirror> annotationMirrors) {
    org.microbean.lang.type.WildcardType t = new org.microbean.lang.type.WildcardType(upperBound);
    t.addAnnotationMirrors(annotationMirrors);
    return t;
  }


  /*
   * Inner and nested classes.
   */


  private static abstract class AbstractSyntheticElement implements javax.lang.model.element.Element {

    private final javax.lang.model.type.TypeMirror type;

    private final javax.lang.model.element.Name name;

    private AbstractSyntheticElement(final javax.lang.model.type.TypeMirror type, final javax.lang.model.element.Name name) {
      super();
      this.type = type;
      this.name = name;
    }

    @Override
    public <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
      return v.visitUnknown(this, p);
    }

    @Override
    public javax.lang.model.type.TypeMirror asType() {
      return this.type;
    }

    @Override
    public final <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
      return null;
    }

    @Override
    public final List<? extends AnnotationMirror> getAnnotationMirrors() {
      return List.of();
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <A extends Annotation> A[] getAnnotationsByType(final Class<A> annotationType) {
      return (A[])EMPTY_ANNOTATION_ARRAY;
    }

    @Override
    public final List<? extends javax.lang.model.element.Element> getEnclosedElements() {
      return List.of();
    }

    @Override
    public final javax.lang.model.element.Element getEnclosingElement() {
      return null;
    }

    @Override
    public ElementKind getKind() {
      return ElementKind.OTHER;
    }

    @Override
    public Set<javax.lang.model.element.Modifier> getModifiers() {
      return Set.of();
    }

    @Override
    public javax.lang.model.element.Name getSimpleName() {
      return this.name;
    }

  }

  private static final class SyntheticArrayElement extends AbstractSyntheticElement {

    private static final SyntheticArrayElement INSTANCE = new SyntheticArrayElement();

    private SyntheticArrayElement() {
      super(new org.microbean.lang.type.DeclaredType(), org.microbean.lang.element.Name.of("Array")); // emulate javac
    }

    @Override
    public final ElementKind getKind() {
      return ElementKind.CLASS; // emulate javac
    }

  }

  private static final class SyntheticElement extends AbstractSyntheticElement {

    private final Reference<javax.lang.model.type.TypeMirror> type;

    private SyntheticElement(final javax.lang.model.type.TypeMirror t) {
      super(null, generateName(t));
      this.type = new WeakReference<>(t);
    }

    @Override
    public final javax.lang.model.type.TypeMirror asType() {
      final javax.lang.model.type.TypeMirror t = this.type.get();
      return t == null ? org.microbean.lang.type.NoType.NONE : t;
    }

    private static final javax.lang.model.element.Name generateName(final javax.lang.model.type.TypeMirror t) {
      return org.microbean.lang.element.Name.of(); // TODO if it turns out to be important
    }

  }

  private static final class SyntheticPrimitiveElement extends AbstractSyntheticElement {


    /*
     * Static fields.
     */


    private static final SyntheticPrimitiveElement BOOLEAN = new SyntheticPrimitiveElement(org.microbean.lang.element.Name.of("boolean"), org.microbean.lang.type.PrimitiveType.BOOLEAN);

    private static final SyntheticPrimitiveElement BYTE = new SyntheticPrimitiveElement(org.microbean.lang.element.Name.of("byte"), org.microbean.lang.type.PrimitiveType.BYTE);

    private static final SyntheticPrimitiveElement CHAR = new SyntheticPrimitiveElement(org.microbean.lang.element.Name.of("char"), org.microbean.lang.type.PrimitiveType.CHAR);

    private static final SyntheticPrimitiveElement DOUBLE = new SyntheticPrimitiveElement(org.microbean.lang.element.Name.of("double"), org.microbean.lang.type.PrimitiveType.DOUBLE);

    private static final SyntheticPrimitiveElement FLOAT = new SyntheticPrimitiveElement(org.microbean.lang.element.Name.of("float"), org.microbean.lang.type.PrimitiveType.FLOAT);

    private static final SyntheticPrimitiveElement INT = new SyntheticPrimitiveElement(org.microbean.lang.element.Name.of("int"), org.microbean.lang.type.PrimitiveType.INT);

    private static final SyntheticPrimitiveElement LONG = new SyntheticPrimitiveElement(org.microbean.lang.element.Name.of("long"), org.microbean.lang.type.PrimitiveType.LONG);

    private static final SyntheticPrimitiveElement SHORT = new SyntheticPrimitiveElement(org.microbean.lang.element.Name.of("short"), org.microbean.lang.type.PrimitiveType.SHORT);


    /*
     * Constructors.
     */


    private SyntheticPrimitiveElement(final javax.lang.model.element.Name name, final javax.lang.model.type.PrimitiveType type) {
      super(validateType(type), name);
    }

    private static final javax.lang.model.type.TypeMirror validateType(final javax.lang.model.type.TypeMirror t) {
      if (!t.getKind().isPrimitive()) {
        throw new IllegalArgumentException("t: " + t);
      }
      return t;
    }

  }

  private static final class SyntheticWildcardElement extends AbstractSyntheticElement {

    private static final SyntheticWildcardElement INSTANCE = new SyntheticWildcardElement();

    private SyntheticWildcardElement() {
      super(new org.microbean.lang.type.DeclaredType(),
            org.microbean.lang.element.Name.of("Bound")); // emulate javac
    }

    @Override
    public final ElementKind getKind() {
      return ElementKind.CLASS; // emulate javac
    }

  }

}
