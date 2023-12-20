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
package org.microbean.lang;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import java.util.function.Supplier;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;

import javax.lang.model.SourceVersion;

import javax.lang.model.element.TypeElement;

import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import org.opentest4j.AssertionFailedError;
import org.opentest4j.IncompleteExecutionException;
import org.opentest4j.MultipleFailuresError;

public final class AnnotationProcessingInterceptor implements InvocationInterceptor, ParameterResolver, TestExecutionExceptionHandler {

  @Deprecated // for use by JUnit Jupiter internals only
  private AnnotationProcessingInterceptor() {
    super();
  }

  /*
   * Invocation order:
   *
   * 1. supportsParameter(ParameterContext, ExtensionContext)
   * 2. resolveParameter(ParameterContext, ExtensionContext)
   * 3. interceptTestMethod(Invocation<Void>,
   *                        ReflectiveInvocationContext<Method>,
   *                        ExtensionContext)
   * 4. (handleTestExecutionException(ExtensionContext, Throwable))
   */

  @Deprecated // for use by JUnit Jupiter internals only
  @Override // ParameterResolver
  public final boolean supportsParameter(final ParameterContext parameterContext,
                                         final ExtensionContext extensionContext) {
    return ProcessingEnvironment.class.isAssignableFrom(parameterContext.getParameter().getType());
  }

  @Deprecated // for use by JUnit Jupiter internals only
  @Override // ParameterResolver
  public final ForwardingProcessingEnvironment resolveParameter(final ParameterContext parameterContext,
                                                                final ExtensionContext extensionContext) {
    return new ForwardingProcessingEnvironment();
  }

  @Deprecated // for use by JUnit Jupiter internals only
  @Override // InvocationInterceptor
  public final void interceptTestMethod(final Invocation<Void> invocation,
                                        final ReflectiveInvocationContext<Method> invocationContext,
                                        final ExtensionContext extensionContext)
    throws Throwable {
    final List<?> arguments = invocationContext.getArguments();
    if (arguments.size() != 1) {
      invocation.proceed();
      return;
    }
    final Object soleArgument = arguments.get(0);
    if (!(soleArgument instanceof ForwardingProcessingEnvironment)) {
      invocation.proceed();
      return;
    }

    final CompilationTask task = ToolProvider.getSystemJavaCompiler()
      .getTask(null,
               null,
               null,
               List.of("-proc:only"),
               List.of("java.lang.Object"),
               null);
    // SupportedSourceVersion targets types only.
    final SupportedSourceVersion ssv = invocationContext.getTargetClass().getAnnotation(SupportedSourceVersion.class);
    final SourceVersion sourceVersion = ssv == null ? SourceVersion.latest() : ssv.value();
    final ForwardingProcessingEnvironment fpe = (ForwardingProcessingEnvironment)soleArgument;
    task.setProcessors(List.of(new AbstractProcessor() {

        @Override // AbstractProcessor
        public final void init(final ProcessingEnvironment processingEnvironment) {
          try {
            fpe.delegate = processingEnvironment;
            invocation.proceed();
          } catch (final RuntimeException | Error e) {
            throw e;
          } catch (final Exception e) {
            throw new RuntimeException(e.getMessage(), e);
          } catch (final Throwable t) {
            throw new AssertionError(t.getMessage(), t);
          } finally {
            fpe.delegate = null;
          }
        }

        @Override // AbstractProcessor
        public final SourceVersion getSupportedSourceVersion() {
          return sourceVersion;
        }

        @Override // AbstractProcessor
        public final boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment) {
          return false;
        }

      }));
    task.call();
  }

  @Override // TestExecutionExceptionHandler
  public final void handleTestExecutionException(final ExtensionContext extensionContext,
                                                 final Throwable throwable)
    throws Throwable {
    if ((throwable instanceof Error) || (throwable instanceof IncompleteExecutionException)) {
      throw throwable;
    } else if (throwable != null) {
      handleTestExecutionException(extensionContext, throwable.getCause());
      throw throwable;
    }
  }


  /*
   * Inner and nested classes.
   */


  private static final class ForwardingProcessingEnvironment implements ProcessingEnvironment {

    private volatile ProcessingEnvironment delegate;

    private ForwardingProcessingEnvironment() {
      super();
    }

    @Override
    public final Elements getElementUtils() {
      return this.delegate.getElementUtils();
    }

    @Override
    public final Filer getFiler() {
      return this.delegate.getFiler();
    }

    @Override
    public final Locale getLocale() {
      return this.delegate.getLocale();
    }

    @Override
    public final Messager getMessager() {
      return this.delegate.getMessager();
    }

    @Override
    public final Map<String, String> getOptions() {
      return this.delegate.getOptions();
    }

    @Override
    public final SourceVersion getSourceVersion() {
      return this.delegate.getSourceVersion();
    }

    @Override
    public final Types getTypeUtils() {
      return this.delegate.getTypeUtils();
    }

    @Override
    public final boolean isPreviewEnabled() {
      return this.delegate.isPreviewEnabled();
    }

    @Override
    public final int hashCode() {
      return this.delegate == null ? 0 : this.delegate.hashCode();
    }

    @Override
    public final boolean equals(final Object other) {
      if (other == this) {
        return true;
      } else if (other != null && this.getClass() == other.getClass()) {
        return Objects.equals(this.delegate, ((ForwardingProcessingEnvironment)other).delegate);
      } else {
        return false;
      }
    }

    @Override
    public final String toString() {
      return this.delegate == null ? super.toString() : this.delegate.toString();
    }

  }

}
