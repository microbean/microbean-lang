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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
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

import net.bytebuddy.pool.TypePool;
// import net.bytebuddy.pool.TypePool.Default.LazyTypeDescription;
// import net.bytebuddy.pool.TypePool.Default.LazyTypeDescription.TypeContainment;

import net.bytebuddy.description.type.TypeDescription;

import net.bytebuddy.dynamic.ClassFileLocator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.microbean.lang.Lang;

import org.microbean.lang.element.DelegatingElement;

import org.microbean.lang.type.DelegatingTypeMirror;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.microbean.lang.Lang.modifiers;
import static org.microbean.lang.Lang.wrap;

final class TestTypeElementTypePool {

  private TypePool tp;
  
  private TestTypeElementTypePool() {
    super();
  }

  @BeforeEach
  final void setup() {
    this.tp = new TypeElementTypePool(new TypePool.CacheProvider.Simple());
  }

  @Disabled // infinite loop
  @Test
  final void testFirstSpike() {
    final TypeDescription td = tp.describe("java.lang.Integer").resolve();
    System.out.println("*** td: " + td);
  }

  @Test
  final void testModifiers() {
    final long modifiers = modifiers(EnumSet.of(Modifier.PUBLIC, Modifier.PRIVATE));
    assertEquals(java.lang.reflect.Modifier.PUBLIC | java.lang.reflect.Modifier.PRIVATE, modifiers);
  }

  @Test
  final void testFrob() {
    assertEquals(1<<12, 0x1000);
  }

  @Test
  final void testJavaLangReflectModifierAndJavaxLangModelElementModifierEquality() {
    assertTrue(java.lang.reflect.Modifier.isPublic(1));
    assertTrue(java.lang.reflect.Modifier.isPrivate(2));
    assertTrue(java.lang.reflect.Modifier.isPrivate(3));

    // How about AccessFlag?
    assertEquals(java.lang.reflect.Modifier.PUBLIC, java.lang.reflect.AccessFlag.PUBLIC.mask());
    assertEquals(java.lang.reflect.Modifier.SYNCHRONIZED, java.lang.reflect.AccessFlag.SYNCHRONIZED.mask());
  }

  @Test
  final void testMyTypePool() {
    final MyTypePool tp = new MyTypePool(ClassFileVersion.JAVA_V20);
    final TypeDescription javaLangInteger = new MyTypePool.MyTypeDescription(ClassFileVersion.JAVA_V20, tp, Lang.typeElement("java.lang.Integer"));
    System.out.println(javaLangInteger);
    for (final TypeDescription.Generic i : javaLangInteger.getInterfaces()) {
      System.out.println(i);
    }
  }

  @Test
  final void testSignatureStuff() {
    final TypeElement e = Lang.typeElement("java.util.List");
    System.out.println(Lang.elementSignature(e));    
  }

  private static final class MyTypePool extends TypePool.Default {

    private static final long RECORD = 1L << 61;

    private final ClassFileVersion classFileVersion;
    
    private MyTypePool(final ClassFileVersion classFileVersion) {
      super(new TypePool.CacheProvider.Simple(), ClassFileLocator.NoOp.INSTANCE, TypePool.Default.ReaderMode.FAST);
      this.classFileVersion = classFileVersion;
    }

    @Override
    protected final Resolution doDescribe(final String name) {
      final TypeElement e = Lang.typeElement(name);
      if (e == null) {
        return new Resolution.Illegal(name);
      }
      return new Resolution.Simple(new MyTypeDescription(this.classFileVersion, this, Lang.typeElement(name)));
    }

    static final class MyTypeDescription extends LazyTypeDescription {

      private static final String[] EMPTY_STRING_ARRAY = new String[0];

      // TODO: for binary names in this mess, are they JVM binary names, or JLS binary names? Raph calls them "internal names" which isn't a thing.
      MyTypeDescription(final ClassFileVersion classFileVersion, final TypePool typePool, TypeElement e) {
        super(typePool,
              actualModifiers(e),
              modifiers(e),
              Lang.binaryName(e).toString(),
              binaryName(e.getSuperclass()),
              interfaceBinaryNames(e),
              genericSignature(e),
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

      private static final String binaryName(TypeMirror t) {
        t = wrap(t);
        return switch (t.getKind()) {
        case DECLARED -> Lang.binaryName((TypeElement)((DeclaredType)t).asElement()).toString();
        case NONE -> null; // or empty string?
        case TYPEVAR -> Lang.binaryName((TypeElement)((TypeVariable)t).asElement()).toString();
        default -> throw new IllegalArgumentException("t: " + t);
        };
      }
      
      private static final int actualModifiers(Element e) {
        e = wrap(e);
        return (int)Lang.modifiers(e);
      }

      private static final int modifiers(Element e) {
        e = wrap(e);
        return (int)Lang.modifiers(e);
      }

      private static final String[] interfaceBinaryNames(TypeElement e) {
        e = wrap(e);
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

      private static final String genericSignature(Element e) {
        return Lang.elementSignature(e);
      }

      private static final TypeContainment typeContainment(Element e) {
        e = wrap(e);
        final Element ee = e.getEnclosingElement();
        if (ee == null) {
          return TypeContainment.SelfContained.INSTANCE;
        }
        // ee should be wrapped already
        return switch (ee.getKind()) {
        case METHOD ->
          new TypeContainment.WithinMethod(Lang.binaryName(wrap((TypeElement)ee.getEnclosingElement())).toString(),
                                           ee.getSimpleName().toString(), // TODO: maybe? needs to be method's "internal name" which I think is just its "unqualified name" (4.2.2 JVM)
                                           Lang.descriptor(ee.asType())) {};
        case ANNOTATION_TYPE, CLASS, ENUM, INTERFACE, RECORD ->
          new TypeContainment.WithinType(Lang.binaryName((TypeElement)ee).toString(),
                                         ((TypeElement)ee).getNestingKind() == NestingKind.LOCAL) {}; // TODO: this is for the enclosing element, yes?
        case PACKAGE -> TypeContainment.SelfContained.INSTANCE;
        default -> throw new IllegalStateException(); // I guess?
        };
      }

      private static final String declaringTypeBinaryName(TypeElement e) {
        e = wrap(e);
        // TODO: triple check
        final TypeMirror t = ((DeclaredType)e.asType()).getEnclosingType();
        // t should already be wrapped
        if (t == null || t.getKind() == TypeKind.NONE) {
          return null;
        }
        return Lang.binaryName((TypeElement)((DeclaredType)t).asElement()).toString();
      }

      private static final List<String> declaredTypeDescriptors(Element e) {
        e = wrap(e);
        final ArrayList<String> l = new ArrayList<>();
        for (final Element ee : e.getEnclosedElements()) {          
          // ee should already be wrapped
          assert ee instanceof DelegatingElement;
          if (ee.getKind().isDeclaredType()) {
            l.add(Lang.descriptor(ee.asType()));
          }
        }
        l.trimToSize();
        return Collections.unmodifiableList(l);
      }

      private static final String nestHostBinaryName(Element e) {
        e = wrap(e);
        return null;
      }

      private static final List<String> nestMemberBinaryNames(Element e) {
        e = wrap(e);
        return List.of();
      }

      private static final Map<String, List<AnnotationToken>> superclassAnnotationTokens(Element e) {
        e = wrap(e);
        return Map.of();
      }

      private static final Map<Integer, Map<String, List<AnnotationToken>>> interfaceAnnotationTokens(Element e) {
        e = wrap(e);
        return Map.of();
      }

      private static final Map<Integer, Map<String, List<AnnotationToken>>> typeVariableAnnotationTokens(Element e) {
        e = wrap(e);
        return Map.of();
      }

      private static final Map<Integer, Map<Integer, Map<String, List<AnnotationToken>>>> typeVariableBoundsAnnotationTokens(Element e) {
        e = wrap(e);
        return Map.of();
      }

      private static final List<AnnotationToken> annotationTokens(Element e) {
        e = wrap(e);
        return List.of();
      }

      private static final List<FieldToken> fieldTokens(Element e) {
        e = wrap(e);
        final ArrayList<FieldToken> l = new ArrayList<>();
        for (final Element ee : e.getEnclosedElements()) {
          assert ee instanceof DelegatingElement;
          if (ee.getKind() == ElementKind.FIELD) { // TODO: and enum constants?
            l.add(fieldToken((VariableElement)ee));
          }
        }
        l.trimToSize();
        return Collections.unmodifiableList(l);
      }

      private static final List<MethodToken> methodTokens(Element e) {
        e = wrap(e);
        final ArrayList<MethodToken> l = new ArrayList<>();
        for (final Element ee : e.getEnclosedElements()) {
          assert ee instanceof DelegatingElement;
          if (ee.getKind().isExecutable()) {
            l.add(methodToken((ExecutableElement)ee));
          }
        }
        l.trimToSize();
        return Collections.unmodifiableList(l);
      }

      private static final List<RecordComponentToken> recordComponentTokens(Element e) {
        e = wrap(e);
        final ArrayList<RecordComponentToken> l = new ArrayList<>();
        for (final Element ee : e.getEnclosedElements()) {
          assert ee instanceof DelegatingElement;
          if (ee.getKind() == ElementKind.RECORD_COMPONENT) {
            l.add(recordComponentToken((RecordComponentElement)ee));
          }
        }
        l.trimToSize();
        return Collections.unmodifiableList(l);
      }

      private static final List<String> permittedSubclassBinaryNames(TypeElement e) {
        e = wrap(e);
        final List<? extends TypeMirror> ts = e.getPermittedSubclasses();
        if (ts.isEmpty()) {
          return List.of();
        }
        final List<String> l = new ArrayList<>(ts.size());
        // every t should already be wrapped
        for (final TypeMirror t : ts) {
          assert t instanceof DelegatingTypeMirror;
          l.add(Lang.binaryName((TypeElement)((DeclaredType)t).asElement()).toString());
        }
        return Collections.unmodifiableList(l);
      }

      private static final FieldToken fieldToken(Element e) {
        e = wrap(e);
        if (e.getKind() != ElementKind.FIELD) {
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

      private static final MethodToken methodToken(ExecutableElement e) {
        e = wrap(e);
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
        final ArrayList<MyMethodToken.MyParameterToken> parameterTokens = new ArrayList<>();
        for (final VariableElement p : e.getParameters()) {
          assert p instanceof DelegatingElement;
          parameterTokens.add(parameterToken(p));
        }
        parameterTokens.trimToSize();
        return
          new MyMethodToken(e.getSimpleName().toString(),
                            (int)Lang.modifiers(e),
                            Lang.descriptor(e.asType()),
                            genericSignature(e),
                            exceptionBinaryNames,
                            Map.of(),
                            Map.of(),
                            Map.of(),
                            Map.of(),
                            Map.of(),
                            Map.of(),
                            List.of(), // annotationTokens
                            Map.of(),
                            Collections.unmodifiableList(parameterTokens),
                            null); // defaultValue
      }

      private static final MyMethodToken.MyParameterToken parameterToken(VariableElement e) {
        e = wrap(e);
        final int modifiers = (int)Lang.modifiers(e);
        return new MyMethodToken.MyParameterToken(e.getSimpleName().toString(), modifiers == 0 ? null : Integer.valueOf(modifiers));
      }

      private static final RecordComponentToken recordComponentToken(Element e) {
        e = wrap(e);
        return
          new RecordComponentToken(e.getSimpleName().toString(),
                                   Lang.descriptor(e.asType()),
                                   genericSignature(e),
                                   Map.of(),
                                   List.of()) {}; // annotationTokens
      }

      private static final class MyMethodToken extends MethodToken {

        @SuppressWarnings("unchecked")
        private MyMethodToken(final String name,
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

        static final class MyParameterToken extends ParameterToken {

          MyParameterToken(final String name, final Integer modifiers) {
            super(name, modifiers);
          }
          
        }
        
      }
      
    }
    
  }
  
}
