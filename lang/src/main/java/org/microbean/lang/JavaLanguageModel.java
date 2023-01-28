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

import javax.tools.ToolProvider;

import javax.lang.model.SourceVersion;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public final class JavaLanguageModel implements AutoCloseable {
  
  private volatile ProcessingEnvironment pe;

  private final CountDownLatch initLatch;
  
  private final CountDownLatch runningLatch;

  public JavaLanguageModel() {
    this(null, false);
  }
  
  public JavaLanguageModel(final JavaFileManager fileManager, final boolean verbose) {
    super();
    final JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
    if (jc == null) {
      throw new IllegalStateException("ToolProvider.getSystemJavaCompiler() == null");
    }
    this.initLatch = new CountDownLatch(1);
    this.runningLatch = new CountDownLatch(1);
    final Thread t = new Thread(() -> {
        // (Any "loading" is actually performed by, e.g. com.sun.tools.javac.jvm.ClassReader.fillIn(), not reflective
        // machinery.)
        final CompilationTask task =
          jc.getTask(null, // System.err
                     fileManager,
                     null, // diagnosticListener
                     verbose ? List.of("-proc:only", "-sourcepath", "", "-verbose") : List.of("-proc:only", "-sourcepath", ""),
                     List.of("java.lang.annotation.RetentionPolicy"), // loads the least amount of stuff up front
                     null);
        task.setProcessors(List.of(new P()));
        task.call();
    }, "Elements");
    t.setDaemon(true);
    t.setUncaughtExceptionHandler((x, e) -> { e.printStackTrace(); this.close(); this.initLatch.countDown(); });
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
  
  public final Elements elements() {
    return this.pe().getElementUtils();
  }

  public final Locale locale() {
    return this.pe().getLocale();
  }

  public final Types types() {
    return this.pe().getTypeUtils();
  }

  private final class P extends AbstractProcessor {

    private P() {
      super();
    }

    @Override
    public final void init(final ProcessingEnvironment pe) {
      JavaLanguageModel.this.pe = pe;
      JavaLanguageModel.this.initLatch.countDown();
      try {
        JavaLanguageModel.this.runningLatch.await();
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }          
    }

    @Override // AbstractProcessor
    public final SourceVersion getSupportedSourceVersion() {
      return SourceVersion.latest();
    }

    @Override // AbstractProcessor
    public final boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment) {
      return false;
    }
    
  }
  
}
