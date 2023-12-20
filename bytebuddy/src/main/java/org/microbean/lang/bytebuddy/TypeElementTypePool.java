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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import net.bytebuddy.ClassFileVersion;

import net.bytebuddy.description.annotation.AnnotationValue;

import net.bytebuddy.dynamic.ClassFileLocator;

import net.bytebuddy.pool.TypePool;

import org.microbean.lang.Lang;
import org.microbean.lang.TypeAndElementSource;

import org.microbean.lang.element.DelegatingElement;

import org.microbean.lang.type.DelegatingTypeMirror;

public final class TypeElementTypePool extends TypePool.Default {

  private final ClassFileVersion classFileVersion;

  private final TypeAndElementSource tes;

  public TypeElementTypePool() {
    this(ClassFileVersion.ofThisVm(), new TypePool.CacheProvider.Simple(), Lang.typeAndElementSource());
  }

  public TypeElementTypePool(final TypePool.CacheProvider cacheProvider) {
    this(ClassFileVersion.ofThisVm(), cacheProvider, Lang.typeAndElementSource());
  }

  public TypeElementTypePool(final TypeAndElementSource tes) {
    this(ClassFileVersion.ofThisVm(), new TypePool.CacheProvider.Simple(), tes);
  }

  public TypeElementTypePool(final TypePool.CacheProvider cacheProvider,
                             final TypeAndElementSource tes) {
    this(ClassFileVersion.ofThisVm(), cacheProvider, tes);
  }

  public TypeElementTypePool(final ClassFileVersion classFileVersion,
                             final TypePool.CacheProvider cacheProvider,
                             final TypeAndElementSource tes) {
    super(cacheProvider == null ? new TypePool.CacheProvider.Simple() : cacheProvider,
          ClassFileLocator.NoOp.INSTANCE,
          TypePool.Default.ReaderMode.FAST /* actually irrelevant */);
    this.classFileVersion = classFileVersion == null ? ClassFileVersion.ofThisVm() : classFileVersion;
    this.tes = Objects.requireNonNull(tes, "tes");
  }

  @Override
  protected final Resolution doDescribe(final String name) {
    final TypeElement e = this.tes.typeElement(name);
    return e == null ? new Resolution.Illegal(name) : new Resolution.Simple(new TypeDescription(e));
  }

  private final class TypeDescription extends LazyTypeDescription {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    // Binary names in this mess are JVM binary names, not JLS binary names. Raph calls them "internal names" which
    // isn't a thing.
    private TypeDescription(final TypeElement e) {
      super(TypeElementTypePool.this,
            actualModifiers(e),
            modifiers(e),
            Lang.binaryName(e).toString(), // "internalName"
            binaryName(e.getSuperclass()), // "superClassName"
            interfaceBinaryNames(e), // "interfaceName" (yes, singular for some reason)
            genericSignature(e), // "genericSignature"; ASM just calls it a "signature" and seems to be expecting a *type* signature in the JVM parlance
            typeContainment(e),
            declaringTypeBinaryName(e),
            declaredTypeDescriptors(e),
            e.getNestingKind() == NestingKind.ANONYMOUS,
            nestHostBinaryName(e),
            nestMemberBinaryNames(e),
            superclassAnnotationTokens(e),
            interfaceAnnotationTokens(e),
            typeVariableAnnotationTokens(e),
            typeVariableBoundsAnnotationTokens(e),
            annotationTokens(e),
            fieldTokens(e),
            methodTokens(e),
            recordComponentTokens(e),
            permittedSubclassBinaryNames(e),
            classFileVersion);
    }

    private static final String binaryName(final TypeMirror t) {
      assert t instanceof DelegatingTypeMirror;
      return switch (t.getKind()) {
      case DECLARED -> Lang.binaryName((TypeElement)((DeclaredType)t).asElement()).toString();
      case NONE -> null; // or empty string?
      case TYPEVAR -> Lang.binaryName((TypeElement)((TypeVariable)t).asElement()).toString();
      default -> throw new IllegalArgumentException("t: " + t);
      };
    }

    private static final int actualModifiers(final Element e) {
      assert e instanceof DelegatingElement;
      return (int)Lang.modifiers(e);
    }

    private static final int modifiers(final Element e) {
      assert e instanceof DelegatingElement;
      return (int)Lang.modifiers(e);
    }

    private static final String[] interfaceBinaryNames(final TypeElement e) {
      assert e instanceof DelegatingElement;
      final List<? extends TypeMirror> ifaces = e.getInterfaces();
      if (ifaces.isEmpty()) {
        return EMPTY_STRING_ARRAY;
      }
      final String[] rv = new String[ifaces.size()];
      for (int i = 0; i < rv.length; i++) {
        rv[i] = binaryName(ifaces.get(i));
      }
      return rv;
    }

    private static final String genericSignature(final Element e) {
      return Lang.elementSignature(e);
    }

    private static final TypeContainment typeContainment(final Element e) {
      assert e instanceof DelegatingElement;
      final TypeElement ee = (TypeElement)e.getEnclosingElement();
      if (ee == null) {
        return TypeContainment.SelfContained.INSTANCE;
      }
      return switch (ee.getKind()) {
      case METHOD ->
        new TypeContainment.WithinMethod(Lang.binaryName((TypeElement)ee.getEnclosingElement()).toString(),
                                         ee.getSimpleName().toString(), // TODO: maybe? needs to be method's "internal name" which is just its "unqualified name" (4.2.2 JVM)
                                         Lang.descriptor(ee.asType())) {};
      case ANNOTATION_TYPE, CLASS, ENUM, INTERFACE, RECORD ->
        new TypeContainment.WithinType(Lang.binaryName(ee).toString(),
                                       ee.getNestingKind() == NestingKind.LOCAL) {}; // TODO: this is for the enclosing element, yes?
      case PACKAGE -> TypeContainment.SelfContained.INSTANCE;
      default -> throw new IllegalStateException(); // I guess?
      };
    }

    private static final String declaringTypeBinaryName(final TypeElement e) {
      assert e instanceof DelegatingElement;
      // TODO: triple check: getEnclosingType()? or getEnclosingElement.asType()?
      final TypeMirror t = ((DeclaredType)e.asType()).getEnclosingType();
      if (t == null || t.getKind() == TypeKind.NONE) {
        return null;
      }
      return Lang.binaryName((TypeElement)((DeclaredType)t).asElement()).toString();
    }

    private static final List<String> declaredTypeDescriptors(final Element e) {
      assert e instanceof DelegatingElement;
      final ArrayList<String> l = new ArrayList<>();
      for (final Element ee : e.getEnclosedElements()) {
        if (ee.getKind().isDeclaredType()) {
          l.add(Lang.descriptor(ee.asType()));
        }
      }
      l.trimToSize();
      return Collections.unmodifiableList(l);
    }

    private static final String nestHostBinaryName(final Element e) {
      return null;
    }

    private static final List<String> nestMemberBinaryNames(final Element e) {
      return List.of();
    }

    private static final Map<String, List<AnnotationToken>> superclassAnnotationTokens(final Element e) {
      return Map.of();
    }

    private static final Map<Integer, Map<String, List<AnnotationToken>>> interfaceAnnotationTokens(final Element e) {
      return Map.of();
    }

    private static final Map<Integer, Map<String, List<AnnotationToken>>> typeVariableAnnotationTokens(final Element e) {
      return Map.of();
    }

    private static final Map<Integer, Map<Integer, Map<String, List<AnnotationToken>>>> typeVariableBoundsAnnotationTokens(final Element e) {
      return Map.of();
    }

    private static final List<AnnotationToken> annotationTokens(final Element e) {
      return List.of();
    }

    private static final List<FieldToken> fieldTokens(final Element e) {
      assert e instanceof DelegatingElement;
      final ArrayList<FieldToken> l = new ArrayList<>();
      for (final Element ee : e.getEnclosedElements()) {
        if (ee.getKind().isField()) {
          l.add(fieldToken((VariableElement)ee));
        }
      }
      l.trimToSize();
      return Collections.unmodifiableList(l);
    }

    private static final List<MethodToken> methodTokens(final Element e) {
      assert e instanceof DelegatingElement;
      final ArrayList<MethodToken> l = new ArrayList<>();
      for (final Element ee : e.getEnclosedElements()) {
        if (ee.getKind().isExecutable()) {
          l.add(methodToken((ExecutableElement)ee));
        }
      }
      l.trimToSize();
      return Collections.unmodifiableList(l);
    }

    private static final List<RecordComponentToken> recordComponentTokens(final Element e) {
      assert e instanceof DelegatingElement;
      final ArrayList<RecordComponentToken> l = new ArrayList<>();
      for (final Element ee : e.getEnclosedElements()) {
        if (ee.getKind() == ElementKind.RECORD_COMPONENT) {
          l.add(recordComponentToken((RecordComponentElement)ee));
        }
      }
      l.trimToSize();
      return Collections.unmodifiableList(l);
    }

    private static final List<String> permittedSubclassBinaryNames(final TypeElement e) {
      assert e instanceof DelegatingElement;
      final List<? extends TypeMirror> ts = e.getPermittedSubclasses();
      if (ts.isEmpty()) {
        return List.of();
      }
      final List<String> l = new ArrayList<>(ts.size());
      for (final TypeMirror t : ts) {
        l.add(Lang.binaryName((TypeElement)((DeclaredType)t).asElement()).toString());
      }
      return Collections.unmodifiableList(l);
    }

    private static final FieldToken fieldToken(final VariableElement e) {
      assert e instanceof DelegatingElement;
      if (!e.getKind().isField()) {
        throw new IllegalArgumentException("e: " + e);
      }
      return
        new FieldToken(e.getSimpleName().toString(),
                       (int)Lang.modifiers(e),
                       Lang.descriptor(e.asType()),
                       genericSignature(e),
                       Map.of(), // TODO: typeAnnotationTokens
                       List.of()) {}; // TODO: annotationTokens
    }

    private static final MethodToken methodToken(final ExecutableElement e) {
      assert e instanceof DelegatingElement;
      final List<? extends TypeMirror> thrownTypes = e.getThrownTypes();
      final String[] exceptionBinaryNames;
      if (thrownTypes.isEmpty()) {
        exceptionBinaryNames = EMPTY_STRING_ARRAY;
      } else {
        exceptionBinaryNames = new String[thrownTypes.size()];
        for (int i = 0; i < exceptionBinaryNames.length; i++) {
          exceptionBinaryNames[i] = Lang.binaryName((TypeElement)((DeclaredType)thrownTypes.get(i)).asElement()).toString();
        }
      }
      final ArrayList<MethodTokenSubclass.ParameterTokenSubclass> parameterTokens = new ArrayList<>();
      for (final VariableElement p : e.getParameters()) {
        parameterTokens.add(parameterToken(p));
      }
      parameterTokens.trimToSize();
      return
        new MethodTokenSubclass(e.getSimpleName().toString(),
                                (int)Lang.modifiers(e),
                                Lang.descriptor(e.asType()),
                                genericSignature(e),
                                exceptionBinaryNames,
                                Map.of(), // typeVariableAnnotationTokens (Map<Integer, Map<String, List<AnnotationToken>>>)
                                Map.of(), // typeVariableBoundAnnotationTokens
                                Map.of(), // returnTypeAnnotationTokens
                                Map.of(), // parameterTypeAnnotationTokens
                                Map.of(), // exceptionTypeAnnotationTokens
                                Map.of(), // receiverTypeAnnotationTokens
                                List.of(), // annotationTokens
                                Map.of(), // parameterAnnotationTokens
                                Collections.unmodifiableList(parameterTokens),
                                null); // defaultValue
    }

    private static final MethodTokenSubclass.ParameterTokenSubclass parameterToken(final VariableElement e) {
      assert e instanceof DelegatingElement;
      final int modifiers = (int)Lang.modifiers(e);
      return new MethodTokenSubclass.ParameterTokenSubclass(e.getSimpleName().toString(), modifiers == 0 ? null : Integer.valueOf(modifiers));
    }

    private static final RecordComponentToken recordComponentToken(final RecordComponentElement e) {
      assert e instanceof DelegatingElement;
      return
        new RecordComponentToken(e.getSimpleName().toString(),
                                 Lang.descriptor(e.asType()),
                                 genericSignature(e),
                                 Map.of(),
                                 List.of()) {}; // annotationTokens
    }

    private static final class MethodTokenSubclass extends MethodToken {

      @SuppressWarnings("unchecked")
      private MethodTokenSubclass(final String name,
                                  final int modifiers,
                                  final String descriptor,
                                  final String genericSignature,
                                  final String[] exceptionName,
                                  final Map<Integer, Map<String, List<AnnotationToken>>> typeVariableAnnotationTokens,
                                  final Map<Integer, Map<Integer, Map<String, List<AnnotationToken>>>> typeVariableBoundAnnotationTokens,
                                  final Map<String, List<AnnotationToken>> returnTypeAnnotationTokens,
                                  final Map<Integer, Map<String, List<AnnotationToken>>> parameterTypeAnnotationTokens,
                                  final Map<Integer, Map<String, List<AnnotationToken>>> exceptionTypeAnnotationTokens,
                                  final Map<String, List<AnnotationToken>> receiverTypeAnnotationTokens,
                                  final List<AnnotationToken> annotationTokens,
                                  final Map<Integer, List<AnnotationToken>> parameterAnnotationTokens,
                                  final List<? extends ParameterToken> parameterTokens,
                                  final AnnotationValue<?,?> defaultValue) {
        super(name,
              modifiers,
              descriptor,
              genericSignature,
              exceptionName,
              typeVariableAnnotationTokens,
              typeVariableBoundAnnotationTokens,
              returnTypeAnnotationTokens,
              parameterTypeAnnotationTokens,
              exceptionTypeAnnotationTokens,
              receiverTypeAnnotationTokens,
              annotationTokens,
              parameterAnnotationTokens,
              (List<ParameterToken>)parameterTokens,
              defaultValue);
      }

      private static final class ParameterTokenSubclass extends ParameterToken {

        private ParameterTokenSubclass(final String name, final Integer modifiers) {
          super(name, modifiers);
        }

      }

    }

  }

}
