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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import java.util.function.Function;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeKind;

import javax.lang.model.util.Elements;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationValue;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;

import net.bytebuddy.description.modifier.EnumerationState;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.modifier.SyntheticState;
import net.bytebuddy.description.modifier.TypeManifestation;
import net.bytebuddy.description.modifier.Visibility;

import net.bytebuddy.description.type.RecordComponentDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.description.type.TypeVariableToken;

import net.bytebuddy.dynamic.ClassFileLocator;

import net.bytebuddy.dynamic.scaffold.InstrumentedType;

import net.bytebuddy.pool.TypePool;
import net.bytebuddy.pool.TypePool.CacheProvider;
import net.bytebuddy.pool.TypePool.Resolution;

import static org.microbean.lang.Lang.binaryName;
import static org.microbean.lang.Lang.origin;
import static org.microbean.lang.Lang.typeElement;

// TODO: the circularity in here is absolutely frustrating. Look at TypeExtractor instead maybe?  See
// https://github.com/raphw/byte-buddy/blob/a326aae41c2928d20d299e457f3d162c94d5557c/byte-buddy-dep/src/main/java/net/bytebuddy/pool/TypePool.java#L8273-L8298
//
// See GenericTypeExtractor
// See TypeContainment, the LazyTypeDescription class, not the enum

// final class TypeElementTypePool extends TypePool.AbstractBase {
final class TypeElementTypePool extends TypePool.Default.WithLazyResolution {

  public TypeElementTypePool(final CacheProvider cp) {
    super(cp, ClassFileLocator.NoOp.INSTANCE, TypePool.Default.ReaderMode.FAST);
  }

  @Override // TypePool.Default.WithLazyResolution
  // protected final Resolution doDescribe(final String name) {
  protected final Resolution doResolve(final String name) {
    System.out.println("*** calling doResolve with " + name);
    if (name.equals("java.lang.Object")) {
      return new Resolution.Simple(TypeDescription.ForLoadedType.of(Object.class));
    }
    final TypeElement e = typeElement(name);
    if (e == null) {
      return new Resolution.Illegal(name);
    }
    InstrumentedType t = InstrumentedType.Default.of(name, this.typeDescriptionGeneric(e.getSuperclass()), modifiers(e))
      .withInterfaces(this.typeListGeneric(e.getInterfaces())); // ...and so on

    switch (e.getNestingKind()) {
    case ANONYMOUS:
      t = t.withAnonymousClass(true);
      break;
    case LOCAL:
      t = t.withLocalClass(true);
      break;
    }

    if (e.getKind() == ElementKind.RECORD) {
      t = t.withRecord(true);
      for (final RecordComponentElement rce : e.getRecordComponents()) {
        t = t.withRecordComponent(this.recordComponentDescriptionToken(rce));
      }
    }

    // TODO: enclosing? declaring? element, which will be a package, class or executable
    //
    // https://stackoverflow.com/a/9360115/208288:
    // "The subtilty [sic] with getDeclaringClass is that anonymous inner classes are not counted as member of a class in the
    // Java Language Specification whereas named inner classes are. Therefore this method returns null for an anonymous
    // class. The alternative method getEnclosingClass works for both anonymous and named classes."
    final Element enclosingElement = e.getEnclosingElement();
    assert enclosingElement != null;
    final ElementKind eek = enclosingElement.getKind();
    if (eek.isDeclaredType()) {
      t = t.withEnclosingType(this.typeDescription(enclosingElement));
    } else if (eek.isExecutable()) {
      // TODO
    } else {
      assert eek == ElementKind.PACKAGE;
      assert e.getNestingKind() == NestingKind.TOP_LEVEL;
    }

    for (final TypeParameterElement tpe : e.getTypeParameters()) {
      t = t.withTypeVariable(this.typeVariableToken(tpe));
    }

    t = t.withPermittedSubclasses(typeList(e.getPermittedSubclasses()));

    return new Resolution.Simple(t);
  }

  private final MethodDescription.InDefinedShape methodDescriptionInDefinedShape(final ExecutableElement e) {
    if (!e.getKind().isExecutable()) {
      throw new IllegalArgumentException("e: " + e);
    }
    final TypeDescription declaringType = this.typeDescription(e.getEnclosingElement());
    // "Internal name" is really a JVM method descriptor (4.3.3).
    final String internalName = this.descriptor((ExecutableType)e.asType());
    final int modifiers = modifiers(e);
    final List<? extends TypeVariableToken> typeVariables = typeVariableTokens(e);
    final TypeDescription.Generic returnType = typeDescriptionGeneric(e.getReturnType());
    final List<? extends ParameterDescription.Token> parameterTokens = parameterDescriptionTokens(e.getParameters());
    final TypeList.Generic exceptionTypes = this.typeListGeneric(e.getThrownTypes());
    final List<? extends AnnotationDescription> declaredAnnotations = List.of(); // TODO maybe
    final AnnotationValue<?, ?> defaultValue = null; // TODO maybe
    final TypeDescription.Generic receiverType = typeDescriptionGeneric(e.getReceiverType());
    return
      new MethodDescription.Latent(declaringType,
                                   internalName,
                                   modifiers,
                                   typeVariables,
                                   returnType,
                                   parameterTokens,
                                   exceptionTypes,
                                   declaredAnnotations,
                                   defaultValue,
                                   receiverType);
  }

  private final String descriptor(final TypeMirror t) {
    return switch (t.getKind()) {
    case ARRAY -> "[" + this.descriptor(((ArrayType)t).getComponentType()); // recursive
    case BOOLEAN -> "Z"; // yes, really
    case BYTE -> "B";
    case CHAR -> "C";
    case DECLARED -> "L" + this.jvmBinaryName((TypeElement)((DeclaredType)t).asElement()) + ";";
    case DOUBLE -> "D";
    case ERROR -> throw new IllegalArgumentException("t: " + t);
    case EXECUTABLE -> this.descriptor((ExecutableType)t);
    case FLOAT -> "F";
    case INT -> "I";
    case INTERSECTION -> throw new IllegalArgumentException("t: " + t);
    case LONG -> "J"; // yes, really
    case MODULE -> throw new IllegalArgumentException("t: " + t);
    case NONE -> throw new IllegalArgumentException("t: " + t);
    case NULL -> throw new IllegalArgumentException("t: " + t);
    case OTHER -> throw new IllegalArgumentException("t: " + t);
    case PACKAGE -> throw new IllegalArgumentException("t: " + t);
    case SHORT -> "S";
    case TYPEVAR -> throw new IllegalArgumentException("t: " + t);
    case UNION -> throw new IllegalArgumentException("t: " + t);
    case VOID -> "V";
    case WILDCARD -> throw new IllegalArgumentException("t: " + t);
    };
  }

  private final String descriptor(final ExecutableType t) {
    if (t.getKind() != TypeKind.EXECUTABLE) {
      throw new IllegalArgumentException("t: " + t);
    }
    final StringBuilder sb = new StringBuilder("(");
    for (final TypeMirror pt : t.getParameterTypes()) {
      sb.append(this.descriptor(pt));
    }
    return sb.append(")")
      .append(this.descriptor(t.getReturnType()))
      .toString();
  }

  private final String jvmBinaryName(final TypeElement te) {
    if (!te.getKind().isDeclaredType()) {
      throw new IllegalArgumentException("te: " + te);
    }
    /*
    return switch (te.getNestingKind()) {
    case ANONYMOUS, LOCAL -> ""; // "A local class does not have a canonical name [and does not have a fully qualified name either]."
    case MEMBER -> this.jvmBinaryName(te.getEnclosingElement()) + "$" + te.getSimpleName().toString();
    case TOP_LEVEL -> te.getQualifiedName().toString().replace('.', '/'); // QualifiedNameable#getQualifiedName() returns the canonical name.
    };
    */
    return binaryName(te).toString().replace('.', '/');
  }

  private final List<? extends ParameterDescription.Token> parameterDescriptionTokens(final Collection<? extends Element> ps) {
    if (ps.isEmpty()) {
      return List.of();
    }
    final List<ParameterDescription.Token> list = new ArrayList<>(ps.size());
    for (final Element p : ps) {
      list.add(parameterDescriptionToken(p));
    }
    return Collections.unmodifiableList(list);
  }

  private final ParameterDescription.Token parameterDescriptionToken(final Element p) {
    if (p.getKind() != ElementKind.PARAMETER) {
      throw new IllegalArgumentException("p: " + p);
    }
    return new ParameterDescription.Token(this.typeDescriptionGeneric(p.asType()), p.getSimpleName().toString(), modifiers((VariableElement)p));
  }

  private final TypeDescription typeDescription(final Element e) {
    if (!e.getKind().isDeclaredType()) {
      throw new IllegalArgumentException("e: " + e);
    }
    return this.describe(((QualifiedNameable)e).getQualifiedName().toString()).resolve();
  }

  private final TypeDescription typeDescription(final TypeMirror t) {
    if (t.getKind() != TypeKind.DECLARED) {
      throw new IllegalArgumentException("t: " + t);
    }
    return this.typeDescription(((DeclaredType)t).asElement());
  }

  private final TypeDescription.Generic typeDescriptionGeneric(final TypeMirror t) {
    switch (t.getKind()) {
    case ARRAY:
      return TypeDescription.Generic.Builder.of(this.typeDescriptionGeneric(((ArrayType)t).getComponentType())).asArray().build();
    case BOOLEAN:
      return TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(boolean.class);
    case BYTE:
      return TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(byte.class);
    case CHAR:
      return TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(char.class);
    case DECLARED:
      final DeclaredType dt = (DeclaredType)t;
      final TypeDescription rawType = this.typeDescription(dt);
      if (!generic(dt)) {
        return rawType.asGenericType();
      }
      final TypeDescription.Generic ownerType = this.typeDescriptionGeneric(dt.getEnclosingType());
      final TypeList.Generic arguments = this.typeListGeneric(dt.getTypeArguments());
      if (ownerType == null) {
        return TypeDescription.Generic.Builder.parameterizedType(rawType, arguments).build();
      }
      return TypeDescription.Generic.Builder.parameterizedType(rawType, ownerType, arguments).build();
    case DOUBLE:
      return TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(double.class);
    case FLOAT:
      return TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(float.class);
    case INT:
      return TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(int.class);
    case LONG:
      return TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(long.class);
    case NONE:
      return null; // I think?
    case SHORT:
      return TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(short.class);
    case VOID:
      return TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(void.class);
    default:
      throw new IllegalArgumentException("Illegal type or unhandled type: " + t);
    }
  }

  private final TypeList typeList(final Collection<? extends TypeMirror> ts) {
    final List<TypeDescription> list = new ArrayList<>(ts.size());
    for (final TypeMirror t : ts) {
      list.add(this.typeDescriptionGeneric(t).asErasure());
    }
    return new TypeList.Explicit(list);
  }

  private final TypeList.Generic typeListGeneric(final Collection<? extends TypeMirror> ts) {
    final List<TypeDefinition> list = new ArrayList<>(ts.size());
    for (final TypeMirror t : ts) {
      list.add(this.typeDescriptionGeneric(t));
    }
    return new TypeList.Generic.Explicit(list);
  }

  private final List<? extends TypeVariableToken> typeVariableTokens(final Parameterizable p) {
    final List<? extends TypeParameterElement> tps = p.getTypeParameters();
    if (tps.isEmpty()) {
      return List.of();
    }
    final List<TypeVariableToken> list = new ArrayList<>(tps.size());
    for (final TypeParameterElement tp : tps) {
      list.add(typeVariableToken(tp));
    }
    return Collections.unmodifiableList(list);
  }

  private final TypeVariableToken typeVariableToken(final TypeParameterElement tpe) {
    if (tpe.getKind() != ElementKind.TYPE_PARAMETER) {
      throw new IllegalArgumentException("tpe: " + tpe);
    }
    return new TypeVariableToken(tpe.getSimpleName().toString(), this.typeListGeneric(tpe.getBounds()));
  }

  private final RecordComponentDescription.Token recordComponentDescriptionToken(final RecordComponentElement rce) {
    if (rce.getKind() != ElementKind.RECORD_COMPONENT) {
      throw new IllegalArgumentException("rce: " + rce);
    }
    return new RecordComponentDescription.Token(rce.getSimpleName().toString(), this.typeDescriptionGeneric(rce.asType()));
  }

  private static final int modifiers(final VariableElement ve) {
    return 0; // TODO FIXME
  }

  private static final int modifiers(final ExecutableElement e) {
    return 0; // TODO FIXME
  }

  private static final boolean generic(final Parameterizable p) {
    return !p.getTypeParameters().isEmpty();
  }
  
  private static final boolean generic(final TypeMirror t) {
    return t.getKind() == TypeKind.DECLARED && generic((Parameterizable)((DeclaredType)t).asElement());
  }

  private static final ModifierContributor.ForType[] modifiers(final TypeElement e) {
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
    return builderModifiers.toArray(new ModifierContributor.ForType[0]);
  }

}
