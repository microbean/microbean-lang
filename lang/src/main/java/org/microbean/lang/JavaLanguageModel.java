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
package org.microbean.lang;

import java.io.Writer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import java.util.concurrent.CountDownLatch;

import java.util.function.Consumer;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import javax.lang.model.SourceVersion;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.microbean.lang.ElementSource;

public final class JavaLanguageModel implements AutoCloseable, ElementSource {

  private volatile ProcessingEnvironment pe;

  private final CountDownLatch initLatch;

  private final CountDownLatch runningLatch;

  public JavaLanguageModel() {
    this(null, null, null, false);
  }

  public JavaLanguageModel(final boolean verbose) {
    this(null, null, null, verbose);
  }

  public JavaLanguageModel(final Writer additionalOutputWriter,
                           final JavaFileManager fileManager,
                           final DiagnosticListener<? super JavaFileObject> diagnosticListener,
                           final boolean verbose) {
    super();
    final JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
    if (jc == null) {
      throw new IllegalStateException("ToolProvider.getSystemJavaCompiler() == null");
    }
    this.initLatch = new CountDownLatch(1);
    this.runningLatch = new CountDownLatch(1);
    final Thread t = new Thread(() -> {
        try {
          // (Any "loading" is actually performed by, e.g. com.sun.tools.javac.jvm.ClassReader.fillIn(), not reflective
          // machinery.)
          final CompilationTask task =
            jc.getTask(additionalOutputWriter,
                       fileManager,
                       diagnosticListener,
                       verbose ? List.of("-proc:only", "-sourcepath", "", "-verbose") : List.of("-proc:only", "-sourcepath", ""),
                       List.of("java.lang.annotation.RetentionPolicy"), // loads the least amount of stuff up front
                       null); // compilation units; null means we aren't actually compiling anything
          task.setProcessors(List.of(new P()));
          if (Boolean.FALSE.equals(task.call())) {
            this.close();
            this.initLatch.countDown();
          }
        } catch (final RuntimeException | Error e) {
          e.printStackTrace();
          this.close();
          this.initLatch.countDown();
          throw e;
        }
    }, "Elements");
    t.setDaemon(true);
    t.start();
  }

  private final ProcessingEnvironment pe() {
    ProcessingEnvironment pe = this.pe;
    if (pe == null) {
      try {
        initLatch.await();
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      if ((pe = this.pe) == null) {
        throw new IllegalStateException();
      }
    }
    return pe;
  }

  @Override // AutoCloseable
  public final void close() {
    this.runningLatch.countDown();
    this.pe = null;
  }

  @Override // ElementSource
  public final Element element(final String moduleName, final String n) {
    final Elements elements = this.elements();
    if (moduleName == null) {
      return elements.getTypeElement(n);
    }
    return elements.getTypeElement(elements.getModuleElement(moduleName), n);
  }

  @Override // ElementSource
  public final Element element(final String n) {
    return this.elements().getTypeElement(n);
  }

  public final Elements elements() {
    return this.pe().getElementUtils();
  }

  public final Locale locale() {
    return this.pe().getLocale();
  }

  public final Types types() {
    return this.pe().getTypeUtils();
  }

  public final TypeElement typeElement(final Class<?> c) {
    if (c == null || c.isPrimitive() || c.isArray() || c.isLocalClass() || c.isAnonymousClass()) {
      return null;
    }
    return this.elements().getTypeElement(c.getCanonicalName());
  }

  public final ExecutableElement executableElement(final Executable e) {
    return switch (e) {
    case null -> null;
    case Constructor<?> c -> executableElement(c);
    case Method m -> executableElement(m);
    default -> null;
    };
  }

  public final ExecutableElement executableElement(final Constructor<?> c) {
    if (c == null) {
      return null;
    }
    final TypeElement declaringTypeElement = (TypeElement)this.element(c.getDeclaringClass());
    final Class<?>[] reflectionParameterTypes = c.getParameterTypes();
    CONSTRUCTOR_LOOP:
    for (final Element e : ElementFilter.constructorsIn(declaringTypeElement.getEnclosedElements())) {
      final ExecutableElement ee = (ExecutableElement)e;
      final List<? extends VariableElement> parameterElements = ee.getParameters();
      if (reflectionParameterTypes.length == parameterElements.size()) {
        for (int i = 0; i < reflectionParameterTypes.length; i++) {
          final TypeMirror parameterType = this.type(reflectionParameterTypes[i]);
          final TypeMirror eeParameterType = parameterElements.get(i).asType();
          if (!this.types().isSameType(parameterType, eeParameterType)) {
            continue CONSTRUCTOR_LOOP;
          }
        }
        return ee;
      }
    }
    return null;
  }

  public final ExecutableElement executableElement(final Method m) {
    if (m == null) {
      return null;
    }
    final TypeElement declaringTypeElement = (TypeElement)this.element(m.getDeclaringClass());
    final Class<?>[] reflectionParameterTypes = m.getParameterTypes();
    METHOD_LOOP:
    for (final Element e : ElementFilter.methodsIn(declaringTypeElement.getEnclosedElements())) {
      final ExecutableElement ee = (ExecutableElement)e;
      if (ee.getSimpleName().contentEquals(m.getName()) && this.types().isSameType(ee.getReturnType(), this.type(m.getReturnType()))) {
        final List<? extends VariableElement> parameterElements = ee.getParameters();
        if (reflectionParameterTypes.length == parameterElements.size()) {
          for (int i = 0; i < reflectionParameterTypes.length; i++) {
            final TypeMirror parameterType = this.type(reflectionParameterTypes[i]);
            final TypeMirror eeParameterType = parameterElements.get(i).asType();
            if (!this.types().isSameType(parameterType, eeParameterType)) {
              continue METHOD_LOOP;
            }
          }
          return ee;
        }
      }
    }
    return null;
  }

  public final TypeMirror type(final Type t) {
    return switch (t) {
    case null -> null;
    case Class<?> c when c.isArray() -> this.arrayType(c);
    case Class<?> c when c.isPrimitive() -> this.primitiveType(c);
    case Class<?> c -> this.declaredType(c);
    case ParameterizedType pt -> this.declaredType(pt);
    case GenericArrayType g -> this.arrayType(g);
    case java.lang.reflect.TypeVariable<?> tv -> this.typeVariable(tv);
    case java.lang.reflect.WildcardType w -> this.wildcardType(w);
    default -> null;
    };
  }

  public final ArrayType arrayType(final Class<?> c) {
    if (c == null || !c.isArray()) {
      return null;
    }
    return this.types().getArrayType(this.type(c.getComponentType()));
  }

  public final ArrayType arrayType(final GenericArrayType g) {
    if (g == null) {
      return null;
    }
    return this.types().getArrayType(this.type(g.getGenericComponentType()));
  }

  private final DeclaredType declaredType(final Type t) {
    return switch (t) {
    case null -> null;
    case Class<?> c -> this.declaredType(c);
    case ParameterizedType p -> this.declaredType(p);
    default -> null;
    };
  }
  
  public final DeclaredType declaredType(final Class<?> c) {
    if (c == null || c.isPrimitive() || c.isArray() || c.isLocalClass() || c.isAnonymousClass()) {
      return null;
    }
    return
      this.types().getDeclaredType(this.declaredType(c.getEnclosingClass()), this.typeElement(c));
  }

  public final DeclaredType declaredType(final ParameterizedType pt) {
    if (pt == null) {
      return null;
    }
    return
      this.types().getDeclaredType(this.declaredType(pt.getOwnerType()),
                                   this.typeElement((Class<?>)pt.getRawType()),
                                   this.typeArguments(pt.getActualTypeArguments()));
  }

  public final ExecutableType executableType(final Executable e) {
    if (e == null) {
      return null;
    }
    return (ExecutableType)this.executableElement(e).asType();
  }

  public final PrimitiveType primitiveType(final Class<?> c) {
    if (c == null || !c.isPrimitive()) {
      return null;
    }
    return this.types().getPrimitiveType(TypeKind.valueOf(c.getName().toUpperCase()));
  }

  public final TypeParameterElement typeParameterElement(final java.lang.reflect.TypeVariable<?> t) {
    if (t == null) {
      return null;
    }
    GenericDeclaration gd = t.getGenericDeclaration();
    while (gd != null) {
      java.lang.reflect.TypeVariable<?>[] typeParameters = gd.getTypeParameters();
      for (int i = 0; i < typeParameters.length; i++) {
        if (typeParameters[i].getName().equals(t.getName())) {
          return this.parameterizable(gd).getTypeParameters().get(i);
        }
      }
      gd = gd instanceof Executable e ? e.getDeclaringClass() : ((Class<?>)gd).getEnclosingClass();
    }
    return null;
  }

  private final Parameterizable parameterizable(final GenericDeclaration gd) {
    return switch (gd) {
    case null -> null;
    case Executable e -> this.executableElement(e);
    case Class<?> c -> this.typeElement(c);
    default -> null;
    };
  }

  public final TypeVariable typeVariable(final java.lang.reflect.TypeVariable<?> t) {
    final TypeParameterElement e = this.typeParameterElement(t);
    return e == null ? null : (TypeVariable)e.asType();
  }

  public final WildcardType wildcardType(final java.lang.reflect.WildcardType t) {
    if (t == null) {
      return null;
    }
    final Type[] lowerBounds = t.getLowerBounds();
    return this.types().getWildcardType(this.type(t.getUpperBounds()[0]),
                                        this.type(lowerBounds.length <= 0 ? null : lowerBounds[0]));
  }

  private final TypeMirror[] typeArguments(final Type[] ts) {
    if (ts == null || ts.length <= 0) {
      return new TypeMirror[0];
    }
    final TypeMirror[] rv = new TypeMirror[ts.length];
    for (int i = 0; i < ts.length; i++) {
      rv[i] = this.type(ts[i]);
    }
    return rv;
  }


  /*
   * Inner and nested classes.
   */


  private final class P extends AbstractProcessor {

    private P() {
      super();
    }

    @Override
    public final void init(final ProcessingEnvironment pe) {
      JavaLanguageModel.this.pe = pe;
      JavaLanguageModel.this.initLatch.countDown(); // all set initializing
      try {
        JavaLanguageModel.this.runningLatch.await();
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    @Override // AbstractProcessor
    public final Set<String> getSupportedAnnotationTypes() {
      return Set.of();
    }

    @Override // AbstractProcessor
    public final Set<String> getSupportedOptions() {
      return Set.of();
    }

    @Override // AbstractProcessor
    public final SourceVersion getSupportedSourceVersion() {
      return SourceVersion.latestSupported();
    }

    @Override // AbstractProcessor
    public final boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment) {
      return false; // don't claim any annotations
    }

  }

}
