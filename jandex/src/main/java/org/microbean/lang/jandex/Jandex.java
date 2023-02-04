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
package org.microbean.lang.jandex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import java.util.function.BiFunction;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassInfo.EnclosingMethodInfo;
import org.jboss.jandex.ClassInfo.NestingType;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.RecordComponentInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeTarget;
import org.jboss.jandex.TypeVariableReference;
import org.jboss.jandex.VoidType;

import org.microbean.lang.Modeler;

public final class Jandex extends Modeler {

  private final Map<Object, AnnotationMirror> annotations;
  
  private final IndexView i;

  private final BiFunction<? super String, ? super IndexView, ? extends ClassInfo> unindexedClassnameFunction;

  public Jandex(final IndexView i) {
    this(i, (n, j) -> null);
  }
  
  public Jandex(final IndexView i, final BiFunction<? super String, ? super IndexView, ? extends ClassInfo> unindexedClassnameFunction) {
    super();
    this.i = Objects.requireNonNull(i, "i");
    this.unindexedClassnameFunction = Objects.requireNonNull(unindexedClassnameFunction, "unindexedClassnameFunction");
    this.annotations = new HashMap<>();
  }

  public final AnnotationMirror annotation(final AnnotationInstance k) {
    AnnotationMirror r = this.annotations.get(k);
    if (r == null) {
      final org.microbean.lang.element.AnnotationMirror am = new org.microbean.lang.element.AnnotationMirror();
      r = this.annotations.putIfAbsent(k, am);
      if (r == null) {
        this.build(k, am);
        r = am;
      }
    }
    return r;
  }

  public final javax.lang.model.element.AnnotationValue annotationValue(final Object v) {
    return switch (v) {
    case AnnotationInstance a -> this.annotationValue(this.annotation(a)); // RECURSIVE
    case AnnotationMirror a -> new org.microbean.lang.element.AnnotationValue(a);
    case javax.lang.model.element.AnnotationValue a -> a;
    case org.jboss.jandex.AnnotationValue a -> this.annotationValue(a);
    case Boolean b -> new org.microbean.lang.element.AnnotationValue(b);
    case Byte b -> new org.microbean.lang.element.AnnotationValue(b);
    case Character c -> new org.microbean.lang.element.AnnotationValue(c);
    case Collection<?> c -> new org.microbean.lang.element.AnnotationValue(this.annotationValues(c));
    case Double d -> new org.microbean.lang.element.AnnotationValue(d);
    case FieldInfo f when f.isEnumConstant() && f.declaringClass().isEnum() -> new org.microbean.lang.element.AnnotationValue(this.element(f));
    case Float f -> new org.microbean.lang.element.AnnotationValue(f);
    case Integer i -> new org.microbean.lang.element.AnnotationValue(i);
    case Long l -> new org.microbean.lang.element.AnnotationValue(l);
    case Object[] o -> new org.microbean.lang.element.AnnotationValue(this.annotationValues(o));
    case Short s -> new org.microbean.lang.element.AnnotationValue(s);
    case String s -> new org.microbean.lang.element.AnnotationValue(s);
    case Type t -> new org.microbean.lang.element.AnnotationValue(this.type(t));
    case TypeMirror t -> new org.microbean.lang.element.AnnotationValue(t);
    case VariableElement ve when ve.getKind() == ElementKind.ENUM_CONSTANT -> new org.microbean.lang.element.AnnotationValue(ve);
    case boolean[] o -> new org.microbean.lang.element.AnnotationValue(this.annotationValues(o));
    case byte[] o -> new org.microbean.lang.element.AnnotationValue(this.annotationValues(o));
    case char[] o -> new org.microbean.lang.element.AnnotationValue(this.annotationValues(o));
    case double[] o -> new org.microbean.lang.element.AnnotationValue(this.annotationValues(o));
    case float[] o -> new org.microbean.lang.element.AnnotationValue(this.annotationValues(o));
    case int[] o -> new org.microbean.lang.element.AnnotationValue(this.annotationValues(o));
    case long[] o -> new org.microbean.lang.element.AnnotationValue(this.annotationValues(o));
    case null -> null;
    case short[] o -> new org.microbean.lang.element.AnnotationValue(this.annotationValues(o));
    default -> throw new IllegalArgumentException("v: " + v);
    };
  }

  private final javax.lang.model.element.AnnotationValue annotationValue(final org.jboss.jandex.AnnotationValue v) {
    if (v == null) {
      return null;
    }
    return switch (v.kind()) {
    case ARRAY, BOOLEAN, BYTE, CHARACTER, CLASS, DOUBLE, FLOAT, INTEGER, LONG, NESTED, SHORT, STRING -> this.annotationValue(v.value());
    case ENUM -> this.annotationValue(this.classInfoFor(v.asEnumType()).field(v.asEnum()));
    case UNKNOWN -> new org.microbean.lang.element.AnnotationValue(List.of());
    };
  }

  private final TypeElement typeElement(final DotName n) {
    final ClassInfo ci = this.classInfoFor(n);
    return ci == null ? null : (TypeElement)this.element(ci);
  }

  public final TypeElement typeElement(final String n) {
    final ClassInfo ci = this.classInfoFor(n);
    return ci == null ? null : (TypeElement)this.element(ci);
  }

  public final Element element(final Object k) {
    if (k == null) {
      return null;
    }
    Element r = this.elements.get(k);
    if (r == null) {
      r = switch (k) {

      case AnnotationInstance ai -> this.element(classInfoFor(ai.name())); // RECURSIVE

      case ClassInfo ci when ci.kind() == AnnotationTarget.Kind.CLASS && !ci.isModule() -> this.element(ci, () -> new org.microbean.lang.element.TypeElement(kind(ci), nestingKind(ci)), this.elements::putIfAbsent, this::build);
      case ClassType c when c.kind() == Type.Kind.CLASS -> this.element(classInfoFor(c)); // RECURSIVE

      case Element e -> e;

      case FieldInfo f when f.kind() == AnnotationTarget.Kind.FIELD -> this.element(f, () -> new org.microbean.lang.element.VariableElement(kind(f)), this.elements::putIfAbsent, this::build);

      case MethodInfo m when m.kind() == AnnotationTarget.Kind.METHOD -> this.element(m, () -> new org.microbean.lang.element.ExecutableElement(kind(m)), this.elements::putIfAbsent, this::build);

      case MethodParameterInfo p when p.kind() == AnnotationTarget.Kind.METHOD_PARAMETER -> this.element(p, () -> new org.microbean.lang.element.VariableElement(ElementKind.PARAMETER), this.elements::putIfAbsent, this::build);

      case RecordComponentInfo rci when rci.kind() == AnnotationTarget.Kind.RECORD_COMPONENT -> this.element(rci, org.microbean.lang.element.RecordComponentElement::new, this.elements::putIfAbsent, this::build);

      case TypeParameterInfo tpi -> this.element(tpi, org.microbean.lang.element.TypeParameterElement::new, this.elements::putIfAbsent, this::build);
      
      // case org.jboss.jandex.TypeVariable t when t.kind() == Type.Kind.TYPE_VARIABLE -> this.element(t, org.microbean.lang.element.TypeParameterElement::new, this.elements::putIfAbsent, this::build);

      default -> throw new IllegalArgumentException("k: " + k + "; k.getClass(): " + k.getClass());
      };
    }
    return r;
  }

  public final TypeMirror type(final Object k) {
    if (k == null) {
      // e.g. java.lang.Object's superclass
      return null;
    }
    TypeMirror r = this.types.get(k);
    if (r == null) {
      r = switch (k) {

      case AnnotationInstance ai -> this.type(classInfoFor(ai.name())); // RECURSIVE
        
      case ArrayType a when a.kind() == Type.Kind.ARRAY -> this.type(a, org.microbean.lang.type.ArrayType::new, this.types::putIfAbsent, this::build);

      case ClassInfo ci when ci.kind() == AnnotationTarget.Kind.CLASS && !ci.isModule() -> this.type(ci, org.microbean.lang.type.DeclaredType::new, this.types::putIfAbsent, this::build);
      case ClassType c when c.kind() == Type.Kind.CLASS -> this.type(classInfoFor(c)); // RECURSIVE

      case FieldInfo fi -> this.type(fi.type()); // RECURSIVE

      case MethodInfo mi -> this.type(mi, org.microbean.lang.type.ExecutableType::new, this.types::putIfAbsent, this::build);
      
      case ParameterizedType p when p.kind() == Type.Kind.PARAMETERIZED_TYPE -> this.type(p, org.microbean.lang.type.DeclaredType::new, this.types::putIfAbsent, this::build);

      case PrimitiveType p when p.kind() == Type.Kind.PRIMITIVE -> this.type(p, () -> new org.microbean.lang.type.PrimitiveType(kind(p)), this.types::putIfAbsent, this::build);

      case RecordComponentInfo rci when rci.kind() == AnnotationTarget.Kind.RECORD_COMPONENT -> this.type(rci.type()); // RECURSIVE
      
      case TypeMirror t -> t;
      
      // case org.jboss.jandex.TypeVariable t when t.kind() == Type.Kind.TYPE_VARIABLE -> this.type(t, org.microbean.lang.type.TypeVariable::new, this.types::putIfAbsent, this::build);
      case TypeParameterInfo tpi when tpi.typeVariable().kind() == Type.Kind.TYPE_VARIABLE -> this.type(tpi, org.microbean.lang.type.TypeVariable::new, this.types::putIfAbsent, this::build);

      case TypeVariableReference t when t.kind() == Type.Kind.TYPE_VARIABLE_REFERENCE -> this.type(t.follow()); // RECURSIVE

      case VoidType t when t.kind() == Type.Kind.VOID -> org.microbean.lang.type.NoType.VOID;

      case org.jboss.jandex.WildcardType t when t.kind() == Type.Kind.WILDCARD_TYPE -> this.type(t, org.microbean.lang.type.WildcardType::new, this.types::putIfAbsent, this::build);
      
      default -> throw new IllegalArgumentException("k: " + k + "; k.getClass(): " + k.getClass());
      };
    }
    return r;
  }

  private final TypeMirror type(final AnnotationTarget at, final Type t) {
    // TODO: this is all messed up. If t is an ARRAY, maybe a CLASS (because it might be an inner class declaring type
    // parameters that extend its declaring class' type variables), a PARAMETERIZED_TYPE, a TYPE_VARIABLE, a
    // TYPE_VARIABLE_REFERENCE or a WILDCARD_TYPE, we need to maintain the _context_ where it was encountered.
    //
    // For example, given the array type designated by T[]: where did T "come from"? Which MethodInfo or ClassInfo declared the type parameter named T?
    //
    // Similarly: given ? extends T, where did T come from?
    //
    // Or just T as a return type from a MethodInfo.
    //
    switch (t.kind()) {
    case TYPE_VARIABLE:
      return this.type(new TypeParameterInfo(at, t.asTypeVariable()));
    default:
      return this.type(t);
    }
  }

  
  /*
   * Annotation builders.
   */

  
  private final void build(final AnnotationInstance ai, final org.microbean.lang.element.AnnotationMirror am) {
    final ClassInfo ci = classInfoFor(ai.name());
    if (ci == null) {
      return;
    }
    final org.microbean.lang.type.DeclaredType t = (org.microbean.lang.type.DeclaredType)this.type(ci);
    assert t.asElement() != null;
    am.setAnnotationType(t);
    
    for (final org.jboss.jandex.AnnotationValue v : ai.values()) {
      final javax.lang.model.element.ExecutableElement ee = (javax.lang.model.element.ExecutableElement)this.element(annotationElementFor(ci, v.name()));
      final javax.lang.model.element.AnnotationValue av = this.annotationValue(v);
      am.putElementValue(ee, av);
    }
  }
  

  /*
   * Element builders.
   */


  private final void build(final ClassInfo ci, final org.microbean.lang.element.TypeElement e) {
    e.setSimpleName(ci.name().local());
    final org.microbean.lang.type.DeclaredType t = (org.microbean.lang.type.DeclaredType)this.type(ci);
    // Note: we must set type first, then defining element.
    e.setType(t);
    t.setDefiningElement(e);

    for (final RecordComponentInfo rc : ci.unsortedRecordComponents()) {
      e.addEnclosedElement((org.microbean.lang.element.RecordComponentElement)this.element(rc));
    }

    // Modifiers.
    final short modifiers = ci.flags();
    if (java.lang.reflect.Modifier.isAbstract(modifiers)) {
      e.addModifier(javax.lang.model.element.Modifier.ABSTRACT);
    } else if (java.lang.reflect.Modifier.isFinal(modifiers)) {
      e.addModifier(javax.lang.model.element.Modifier.FINAL);
    }
    if (java.lang.reflect.Modifier.isPrivate(modifiers)) {
      e.addModifier(javax.lang.model.element.Modifier.PRIVATE);
    } else if (java.lang.reflect.Modifier.isProtected(modifiers)) {
      e.addModifier(javax.lang.model.element.Modifier.PROTECTED);
    } else if (java.lang.reflect.Modifier.isPublic(modifiers)) {
      e.addModifier(javax.lang.model.element.Modifier.PUBLIC);
    }
    // TODO: no way to tell if a ClassInfo is sealed. See https://github.com/smallrye/jandex/issues/167.
    if (java.lang.reflect.Modifier.isStatic(modifiers)) {
      e.addModifier(javax.lang.model.element.Modifier.STATIC);
    }

    // Enclosing element.
    final Element enclosingElement;
    switch (e.getNestingKind()) {
    case ANONYMOUS:
    case LOCAL:
      Object enclosingObject = ci.enclosingMethod();
      if (enclosingObject == null) {
        enclosingObject = this.classInfoFor(ci.enclosingClass());
      }
      enclosingElement = this.element(enclosingObject);
      break;
    case MEMBER:
      enclosingElement = this.element(this.classInfoFor(ci.enclosingClass()));
      break;
    case TOP_LEVEL:
      enclosingElement = null; // TODO: need to do package, but Jandex doesn't do it
      break;
    default:
      throw new AssertionError();
    }
    e.setEnclosingElement(enclosingElement);

    // Supertypes.
    e.setSuperclass(this.type(ci.superClassType()));
    for (final Type iface : ci.interfaceTypes()) {
      e.addInterface(this.type(iface));
    }

    // Type parameters.
    for (final org.jboss.jandex.TypeVariable tp : ci.typeParameters()) {
      // TODO: we need to figure out how to store type parameters as keys in the elements Map.  Because Jandex doesn't
      // model type parameters as elements, and represents them with type variables whose identity is determined solely
      // from their name (!) (types shouldn't have names) and their bounds, it is possible to have two occurrences of a
      // Jandex TypeVariable, F extends Frob, that "belong" to two different classes, so their enclosing elements are
      // different even thou—you get the idea.
      //
      // TODO: if you're reading this, go over to TestJandex and look for tests that exercise Jandex identity and
      // equality.

      
      /*
      final TypeParameterElement prior = (TypeParameterElement)this.elements.get(tp);
      if (prior != null) {
        throw new AssertionError("Shared type parameter: " + tp + "; ClassInfo: " + ci + "; prior association: " + prior);
      }
      */
      // e.addTypeParameter((org.microbean.lang.element.TypeParameterElement)this.element(tp));
      e.addTypeParameter((org.microbean.lang.element.TypeParameterElement)this.element(new TypeParameterInfo(ci, tp)));
    }

    // TODO: enclosed elements

    // TODO: annotations.
    for (final AnnotationInstance a : ci.annotations()) {
      final AnnotationTarget target = a.target();
      switch (target.kind()) {
      case CLASS:
        e.addAnnotationMirror(this.annotation(a));
        break;
      case FIELD:
        // An annotation on one of my fields.
        ((org.microbean.lang.element.VariableElement)this.element(target.asField())).addAnnotationMirror(this.annotation(a));
        break;
      case METHOD:
        // An annotation on one of my methods.
        ((org.microbean.lang.element.ExecutableElement)this.element(target.asMethod())).addAnnotationMirror(this.annotation(a));
        break;
      case METHOD_PARAMETER:
        // An annotation on one of my methods' parameters.
        ((org.microbean.lang.element.VariableElement)this.element(target.asMethodParameter())).addAnnotationMirror(this.annotation(a));
        break;
      case RECORD_COMPONENT:
        // An annotation on one of my record components.
        ((org.microbean.lang.element.RecordComponentElement)this.element(target.asRecordComponent())).addAnnotationMirror(this.annotation(a));
        break;
      case TYPE:
        // This is where things get stupid.
        final TypeTarget tt = target.asType();
        switch (tt.usage()) {
        case CLASS_EXTENDS:
          // Genuine type usage. Maybe go ahead and apply it here.
          break;
        case EMPTY:
          // Genuine type usage. Maybe go ahead and apply it here.
          break;
        case METHOD_PARAMETER:
          // Genuine type usage WITHIN the type of a method parameter, e.g. blatz(Foo<@Bar String> baz). Maybe go ahead
          // and apply it here.
          break;
        case THROWS:
          // Genuine type usage. Maybe go ahead and apply it here.
          break;
        case TYPE_PARAMETER:
          // Stupid. Actually an *element* annotation. Definitely apply it here.
          ((org.microbean.lang.element.TypeParameterElement)this.element(tt.asTypeParameter())).addAnnotationMirror(this.annotation(a));
          break;
        case TYPE_PARAMETER_BOUND:
          // Genuine type usage WITHIN the type underlying a type parameter element. Maybe go ahead and apply it here.
          break;
        default:
          throw new AssertionError();
        }
        break;
      default:
        throw new AssertionError();
      }
    }
  }

  private final void build(final FieldInfo fi, final org.microbean.lang.element.VariableElement e) {
    e.setSimpleName(fi.name());
    e.setType(this.type(fi));
    e.setEnclosingElement(this.element(fi.declaringClass()));
  }

  private final void build(final MethodInfo mi, final org.microbean.lang.element.ExecutableElement e) {
    if (!mi.isConstructor()) {
      e.setSimpleName(mi.name());
    }
    e.setType(this.type(mi));

    // Modifiers.
    final short modifiers = mi.flags();
    if (isDefault(mi)) {
      e.setDefault(true);
      e.addModifier(javax.lang.model.element.Modifier.DEFAULT);
    } else {
      e.setDefault(false);
      if (java.lang.reflect.Modifier.isAbstract(modifiers)) {
        e.addModifier(javax.lang.model.element.Modifier.ABSTRACT);
      } else if (java.lang.reflect.Modifier.isFinal(modifiers)) {
        e.addModifier(javax.lang.model.element.Modifier.FINAL);
      } else if (java.lang.reflect.Modifier.isNative(modifiers)) {
        e.addModifier(javax.lang.model.element.Modifier.NATIVE);
      }
      if (java.lang.reflect.Modifier.isStatic(modifiers)) {
        e.addModifier(javax.lang.model.element.Modifier.STATIC);
      }
      if (java.lang.reflect.Modifier.isSynchronized(modifiers)) {
        e.addModifier(javax.lang.model.element.Modifier.SYNCHRONIZED);
      }
    }
    if (java.lang.reflect.Modifier.isPrivate(modifiers)) {
      e.addModifier(javax.lang.model.element.Modifier.PRIVATE);
    } else if (java.lang.reflect.Modifier.isProtected(modifiers)) {
      e.addModifier(javax.lang.model.element.Modifier.PROTECTED);
    } else if (java.lang.reflect.Modifier.isPublic(modifiers)) {
      e.addModifier(javax.lang.model.element.Modifier.PUBLIC);
    }

    for (final org.jboss.jandex.TypeVariable tp : mi.typeParameters()) {
      // e.addTypeParameter((org.microbean.lang.element.TypeParameterElement)this.element(tp));
      e.addTypeParameter((org.microbean.lang.element.TypeParameterElement)this.element(new TypeParameterInfo(mi, tp)));
    }
    
    for (final MethodParameterInfo p : mi.parameters()) {
      e.addParameter((org.microbean.lang.element.VariableElement)this.element(p));
    }

    // TODO: annotations.
    for (final AnnotationInstance a : mi.annotations()) {
      final AnnotationTarget target = a.target();
      switch (target.kind()) {
      case CLASS:
        // An annotation on...a local class declared inside me? I guess?
        ((org.microbean.lang.element.TypeElement)this.element(target.asClass())).addAnnotationMirror(this.annotation(a));
        break;
      case METHOD:
        // An annotation on me.
        e.addAnnotationMirror(this.annotation(a));
        break;
      case METHOD_PARAMETER:
        // An annotation on one of my parameters.
        ((org.microbean.lang.element.VariableElement)this.element(target.asMethodParameter())).addAnnotationMirror(this.annotation(a));
        break;
      case TYPE:
        // This is where things get stupid.
        final TypeTarget tt = target.asType();
        switch (tt.usage()) {
        case CLASS_EXTENDS:
          // Genuine type usage. Maybe go ahead and apply it here. The only way this could conceivably happen (TODO:) is
          // if local classes are inspected.
          break;
        case EMPTY:
          // Genuine type usage. Maybe go ahead and apply it here.
          break;
        case METHOD_PARAMETER:
          // Genuine type usage WITHIN the type of one of my parameters, e.g. blatz(Foo<@Bar String> baz). Maybe go ahead
          // and apply it here.
          break;
        case THROWS:
          // Genuine type usage. Maybe go ahead and apply it here.
          break;
        case TYPE_PARAMETER:
          // Stupid. Actually an *element* annotation. Definitely apply it here.
          ((org.microbean.lang.element.TypeParameterElement)this.element(tt.asTypeParameter())).addAnnotationMirror(this.annotation(a));
          break;
        case TYPE_PARAMETER_BOUND:
          // Genuine type usage WITHIN the type underlying a type parameter element. Maybe go ahead and apply it here.
          break;
        default:
          throw new AssertionError();
        }
        break;
      default:
        throw new AssertionError();
      }
    }
  }

  private final void build(final MethodParameterInfo mpi, final org.microbean.lang.element.VariableElement e) {
    String n = mpi.name();
    if (n == null) {
      n = "arg" + mpi.position();
    }
    e.setSimpleName(n);
    e.setType(this.type(mpi.type()));
    // e.setEnclosingElement(this.element(mpi.method())); // interestingly not supported by the javax.lang.model.* api
    for (final AnnotationInstance a : mpi.declaredAnnotations()) {
      e.addAnnotationMirror(this.annotation(a));
    }
  }

  private final void build(final RecordComponentInfo r, final org.microbean.lang.element.RecordComponentElement e) {
    e.setSimpleName(r.name());
    e.setType(this.type(r));
    e.setEnclosingElement(this.element(r));
    e.setAccessor((org.microbean.lang.element.ExecutableElement)this.element(r.declaringClass().method(r.name())));
    for (final AnnotationInstance a : r.declaredAnnotations()) {
      e.addAnnotationMirror(this.annotation(a));
    }
  }

  /*
  private final void build(final org.jboss.jandex.TypeVariable tp, final org.microbean.lang.element.TypeParameterElement e) {
    final org.microbean.lang.type.TypeVariable t = (org.microbean.lang.type.TypeVariable)this.type(tp);
    // Note: we must set type first, then defining element.
    e.setType(t);
    t.setDefiningElement(e);
    e.setSimpleName(tp.identifier());
    // TODO: can't really do annotations here without also knowing where the type parameter came from (did a method
    // declare it? a class?).
  }
  */

  private final void build(final TypeParameterInfo tpi, final org.microbean.lang.element.TypeParameterElement e) {
    final org.microbean.lang.type.TypeVariable t = (org.microbean.lang.type.TypeVariable)this.type(tpi);
    e.setType(t);
    t.setDefiningElement(e);
    e.setSimpleName(tpi.identifier());
  }


  /*
   * Type builders.
   */


  private final void build(final ArrayType a, final org.microbean.lang.type.ArrayType t) {
    t.setComponentType(this.type(a.component())); // TODO: UGH if a.component() is a TypeVariable, we need to know what declared it
  }

  private final void build(final ClassInfo ci, final org.microbean.lang.type.DeclaredType t) {
    final org.microbean.lang.element.TypeElement e = (org.microbean.lang.element.TypeElement)this.element(ci);
    // Note: we must set type first, then defining element.
    e.setType(t);
    t.setDefiningElement(e);

    // Need to do enclosing type, if there is one
    t.setEnclosingType(enclosingTypeFor(ci));

    // Now type arguments (which will be type variables), if there are any.
    for (final org.jboss.jandex.TypeVariable tp : ci.typeParameters()) {
      t.addTypeArgument(this.type(ci, tp));
    }

    // TODO: *possibly* annotations, since a ClassInfo sort of represents both an element and a type.
  }

  private final void build(final MethodInfo mi, final org.microbean.lang.type.ExecutableType t) {
    for (final Type pt : mi.parameterTypes()) {
      t.addParameterType(this.type(mi, pt));
    }
    t.setReceiverType(this.type(mi, mi.receiverType()));
    if (mi.isConstructor()) {
      
    } else {
      t.setReturnType(this.type(mi, mi.returnType()));
    }
    for (final Type et : mi.exceptions()) {
      t.addThrownType(this.type(mi, et));
    }
    for (final org.jboss.jandex.TypeVariable tv : mi.typeParameters()) {
      t.addTypeVariable((org.microbean.lang.type.TypeVariable)this.type(mi, tv));
    }
    // TODO: annotations
  }
  
  private final void build(final ParameterizedType p, final org.microbean.lang.type.DeclaredType t) {
    final ClassInfo ci = this.classInfoFor(p);
    if (ci != null) {
      t.setDefiningElement((org.microbean.lang.element.TypeElement)this.element(ci));
      final org.microbean.lang.type.DeclaredType enclosingType;
      Type owner = p.owner();
      if (owner == null) {
        enclosingType = this.enclosingTypeFor(this.classInfoFor(ci.enclosingClass()));
      } else {
        enclosingType = this.enclosingTypeFor(this.classInfoFor(owner));
      }
      t.setEnclosingType(enclosingType);
      for (final Type arg : p.arguments()) {
        t.addTypeArgument(this.type(ci, arg));
      }
    }
  }

  private final void build(final PrimitiveType p, final org.microbean.lang.type.PrimitiveType t) {
    // TODO: annotations
  }

  private final void build(final TypeParameterInfo tpi, final org.microbean.lang.type.TypeVariable t) {
    final org.microbean.lang.element.TypeParameterElement e = (org.microbean.lang.element.TypeParameterElement)this.element(tpi);
    e.setType(t);
    t.setDefiningElement(e);

    final List<? extends Type> bounds = tpi.typeVariable().bounds();
    switch (bounds.size()) {
    case 0:
      break;
    case 1:
      t.setUpperBound(this.type(bounds.get(0))); // TODO: do we need to account for type variables? If so we need the MethodInfo/ClassInfo here
      break;
    default:
      final org.microbean.lang.type.IntersectionType upperBound = new org.microbean.lang.type.IntersectionType();
      for (final Type bound : bounds) {
        upperBound.addBound(this.type(bound)); // TODO: do we need to account for type variables? If so we need the MethodInfo/ClassInfo here
      }
      t.setUpperBound(upperBound);
      break;
    }
    
  }

  /*
  private final void build(final org.jboss.jandex.TypeVariable tv, final org.microbean.lang.type.TypeVariable t) {
    final org.microbean.lang.element.TypeParameterElement e = (org.microbean.lang.element.TypeParameterElement)this.element(tv);
    // Note: we must set type first, then defining element.
    e.setType(t);
    t.setDefiningElement(e);

    final List<? extends Type> bounds = tv.bounds();
    switch (bounds.size()) {
    case 0:
      break;
    case 1:
      t.setUpperBound(this.type(bounds.get(0)));
      break;
    default:
      final org.microbean.lang.type.IntersectionType upperBound = new org.microbean.lang.type.IntersectionType();
      for (final Type bound : bounds) {
        upperBound.addBound(this.type(bound));
      }
      t.setUpperBound(upperBound);
      break;
    }
    // TODO: annotations, maybe? since org.jboss.jandex.TypeVariable is both an element and a type (!) we need to make
    // sure we're considering only type use annotations
  }
  */

  private final void build(final org.jboss.jandex.WildcardType w, final org.microbean.lang.type.WildcardType t) {
    t.setExtendsBound(this.type(w.extendsBound())); // TODO: blah; if it's a type variable we need its owner.
    t.setSuperBound(this.type(w.superBound())); // Same
  }

  /*
   * Housekeeping.
   */


  private final MethodInfo annotationElementFor(final ClassInfo ci, final String name) {
    // TODO: any validation needed?
    return ci.isAnnotation() ? ci.method(name) : null;
  }

    private final List<? extends javax.lang.model.element.AnnotationValue> annotationValues(final Collection<?> c) {
    if (c == null || c.isEmpty()) {
      return List.of();
    }
    final List<javax.lang.model.element.AnnotationValue> list = new ArrayList<>(c.size());
    for (final Object value : c) {
      list.add(this.annotationValue(value));
    }
    return Collections.unmodifiableList(list);
  }
    
  private final List<? extends javax.lang.model.element.AnnotationValue> annotationValues(final boolean[] o) {
    if (o == null || o.length <= 0) {
      return List.of();
    }
    final List<javax.lang.model.element.AnnotationValue> list = new ArrayList<>(o.length);
    for (final boolean value : o) {
      list.add(this.annotationValue(value));
    }
    return Collections.unmodifiableList(list);
  }

  private final List<? extends javax.lang.model.element.AnnotationValue> annotationValues(final byte[] o) {
    if (o == null || o.length <= 0) {
      return List.of();
    }
    final List<javax.lang.model.element.AnnotationValue> list = new ArrayList<>(o.length);
    for (final byte value : o) {
      list.add(this.annotationValue(value));
    }
    return Collections.unmodifiableList(list);
  }

  private final List<? extends javax.lang.model.element.AnnotationValue> annotationValues(final char[] o) {
    if (o == null || o.length <= 0) {
      return List.of();
    }
    final List<javax.lang.model.element.AnnotationValue> list = new ArrayList<>(o.length);
    for (final char value : o) {
      list.add(this.annotationValue(value));
    }
    return Collections.unmodifiableList(list);
  }

  private final List<? extends javax.lang.model.element.AnnotationValue> annotationValues(final double[] o) {
    if (o == null || o.length <= 0) {
      return List.of();
    }
    final List<javax.lang.model.element.AnnotationValue> list = new ArrayList<>(o.length);
    for (final double value : o) {
      list.add(this.annotationValue(value));
    }
    return Collections.unmodifiableList(list);
  }

  private final List<? extends javax.lang.model.element.AnnotationValue> annotationValues(final float[] o) {
    if (o == null || o.length <= 0) {
      return List.of();
    }
    final List<javax.lang.model.element.AnnotationValue> list = new ArrayList<>(o.length);
    for (final float value : o) {
      list.add(this.annotationValue(value));
    }
    return Collections.unmodifiableList(list);
  }

  private final List<? extends javax.lang.model.element.AnnotationValue> annotationValues(final int[] o) {
    if (o == null || o.length <= 0) {
      return List.of();
    }
    final List<javax.lang.model.element.AnnotationValue> list = new ArrayList<>(o.length);
    for (final int value : o) {
      list.add(this.annotationValue(value));
    }
    return Collections.unmodifiableList(list);
  }

  private final List<? extends javax.lang.model.element.AnnotationValue> annotationValues(final long[] o) {
    if (o == null || o.length <= 0) {
      return List.of();
    }
    final List<javax.lang.model.element.AnnotationValue> list = new ArrayList<>(o.length);
    for (final long value : o) {
      list.add(this.annotationValue(value));
    }
    return Collections.unmodifiableList(list);
  }

  private final List<? extends javax.lang.model.element.AnnotationValue> annotationValues(final short[] o) {
    if (o == null || o.length <= 0) {
      return List.of();
    }
    final List<javax.lang.model.element.AnnotationValue> list = new ArrayList<>(o.length);
    for (final short value : o) {
      list.add(this.annotationValue(value));
    }
    return Collections.unmodifiableList(list);
  }
  
  private final List<? extends javax.lang.model.element.AnnotationValue> annotationValues(final Object[] o) {
    if (o == null || o.length <= 0) {
      return List.of();
    }
    final List<javax.lang.model.element.AnnotationValue> list = new ArrayList<>(o.length);
    for (final Object value : o) {
      list.add(this.annotationValue(value));
    }
    return Collections.unmodifiableList(list);
  }
  
  private final ClassInfo classInfoFor(final Type t) {
    if (t == null) {
      return null;
    }
    switch (t.kind()) {
    case CLASS:
      return classInfoFor(t.asClassType());
    case PARAMETERIZED_TYPE:
      return classInfoFor(t.asParameterizedType());
    default:
      return null;
    }
  }

  private final ClassInfo classInfoFor(final ClassType t) {
    return t == null ? null : classInfoFor(t.name());
  }

  private final ClassInfo classInfoFor(final ParameterizedType t) {
    return t == null ? null : classInfoFor(t.name());
  }

  private final ClassInfo classInfoFor(final DotName n) {
    if (n == null) {
      return null;
    }
    final ClassInfo ci = this.i.getClassByName(n);
    if (ci == null) {
      return this.unindexedClassnameFunction.apply(n.toString(), this.i);
    }
    return ci;
  }

  private final ClassInfo classInfoFor(final String n) {
    if (n == null) {
      return null;
    }
    final ClassInfo ci = this.i.getClassByName(n);
    if (ci == null) {
      return this.unindexedClassnameFunction.apply(n, this.i);
    }
    return ci;
  }

  private final org.microbean.lang.type.DeclaredType enclosingTypeFor(final ClassInfo ci) {
    if (ci != null) {
      final DotName ecn = ci.enclosingClass(); // its id
      if (ecn != null) {
        final ClassInfo enclosingClassInfo = this.classInfoFor(ecn);
        if (enclosingClassInfo != null) {
          return (org.microbean.lang.type.DeclaredType)this.type(enclosingClassInfo);
        }
      }
    }
    return null;
  }


  /*
   * Static methods.
   */


  private static final boolean isDefault(final MethodInfo mi) {
    if (mi.declaringClass().isInterface()) {
      final short flags = mi.flags();
      return !java.lang.reflect.Modifier.isStatic(flags) && !java.lang.reflect.Modifier.isAbstract(flags);
    }
    return false;
  }

  private static final ElementKind kind(final ClassInfo ci) {
    if (ci.isAnnotation()) {
      return ElementKind.ANNOTATION_TYPE;
    } else if (ci.isEnum()) {
      return ElementKind.ENUM;
    } else if (ci.isInterface()) {
      return ElementKind.INTERFACE;
    } else if (ci.isModule()) {
      return ElementKind.MODULE;
    } else if (ci.isRecord()) {
      return ElementKind.RECORD;
    } else {
      return ElementKind.CLASS;
    }
  }

  private static final ElementKind kind(final FieldInfo f) {
    return f.isEnumConstant() ? ElementKind.ENUM_CONSTANT : ElementKind.FIELD;
  }

  private static final ElementKind kind(final MethodInfo m) {
    return m.isConstructor() ? ElementKind.CONSTRUCTOR : ElementKind.METHOD;
  }

  private static final TypeKind kind(final PrimitiveType p) {
    return kind(p.primitive());
  }

  private static final TypeKind kind(final PrimitiveType.Primitive p) {
    return switch (p) {
    case BOOLEAN -> TypeKind.BOOLEAN;
    case BYTE -> TypeKind.BYTE;
    case CHAR -> TypeKind.CHAR;
    case DOUBLE -> TypeKind.DOUBLE;
    case FLOAT -> TypeKind.FLOAT;
    case INT -> TypeKind.INT;
    case LONG -> TypeKind.LONG;
    case SHORT -> TypeKind.SHORT;
    };
  }

  private static final NestingKind nestingKind(final ClassInfo ci) {
    return nestingKind(ci.nestingType());
  }

  private static final NestingKind nestingKind(final NestingType n) {
    return switch (n) {
    case ANONYMOUS -> NestingKind.ANONYMOUS;
    case INNER -> NestingKind.MEMBER;
    case LOCAL -> NestingKind.LOCAL;
    case TOP_LEVEL -> NestingKind.TOP_LEVEL;
    };
  }

  final TypeParameterInfo declarationFor(final ClassInfo context, final org.jboss.jandex.TypeVariable tv) {
    for (final org.jboss.jandex.TypeVariable tp : context.typeParameters()) {
      if (tp.name().equals(tv.name())) {
        // The structural model of Jandex really ticks me off.
        assert tp == tv;
        return new TypeParameterInfo(context, tp);
      }
    }
    final EnclosingMethodInfo enclosingMethod = context.enclosingMethod();
    return enclosingMethod == null ? this.declarationFor(context.enclosingClass(), tv) : this.declarationFor(enclosingMethod, tv);
  }

  final TypeParameterInfo declarationFor(final EnclosingMethodInfo context, final org.jboss.jandex.TypeVariable tv) {
    return this.declarationFor(this.classInfoFor(context.enclosingClass()).method(context.name(), context.parameters().toArray(new Type[0])), tv);
  }

  final TypeParameterInfo declarationFor(final MethodInfo context, final org.jboss.jandex.TypeVariable tv) {
    for (final org.jboss.jandex.TypeVariable tp : context.typeParameters()) {
      if (tp.name().equals(tv.name())) {
        // The structural model of Jandex really ticks me off.
        assert tp == tv;
        return new TypeParameterInfo(context, tp);
      }
    }
    return this.declarationFor(context.declaringClass(), tv);
  }

  final TypeParameterInfo declarationFor(final FieldInfo context, final org.jboss.jandex.TypeVariable tv) {
    if (tv.kind() != Type.Kind.TYPE_VARIABLE) {
      throw new IllegalArgumentException("tv: " + tv);
    } else if (context.kind() != AnnotationTarget.Kind.FIELD) {
      throw new IllegalArgumentException("context: " + context);
    }
    return this.declarationFor(context.declaringClass(), tv);
  }

  final TypeParameterInfo declarationFor(final DotName context, final org.jboss.jandex.TypeVariable tv) {
    return this.declarationFor(this.classInfoFor(context), tv);
  }

  private static final record TypeContext(AnnotationTarget context, Type type) {}
  
  private static final record TypeParameterInfo(AnnotationTarget annotationTarget, org.jboss.jandex.TypeVariable typeVariable) {

    TypeParameterInfo {
      switch (annotationTarget.kind()) {
      case CLASS:
      case METHOD:
        break;
      default:
        throw new IllegalArgumentException("annotationTarget: " + annotationTarget);
      }
      Objects.requireNonNull(typeVariable, "typeVariable");
    }

    final AnnotationTarget.Kind kind() {
      return this.annotationTarget().kind();
    }

    final String identifier() {
      return this.typeVariable().identifier();
    }
    
  }
  
}
