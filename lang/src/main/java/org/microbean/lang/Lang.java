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
package org.microbean.lang;

import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import java.util.concurrent.CountDownLatch;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;

import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;

import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;

import javax.tools.ToolProvider;

import javax.lang.model.SourceVersion;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;
import static java.lang.constant.ConstantDescs.NULL;
import static java.lang.constant.DirectMethodHandleDesc.Kind.STATIC;

public final class Lang {

  private static final ClassDesc CD_ArrayType = ClassDesc.of("javax.lang.model.type.ArrayType");

  private static final ClassDesc CD_CharSequence = ClassDesc.of("java.lang.CharSequence");

  private static final ClassDesc CD_DeclaredType = ClassDesc.of("javax.lang.model.type.DeclaredType");

  private static final ClassDesc CD_Element = ClassDesc.of("javax.lang.model.element.Element");

  private static final ClassDesc CD_Lang = ClassDesc.of("org.microbean.lang.Lang");

  private static final ClassDesc CD_ModuleElement = ClassDesc.of("javax.lang.model.element.ModuleElement");

  private static final ClassDesc CD_Name = ClassDesc.of("javax.lang.model.element.Name");

  private static final ClassDesc CD_NoType = ClassDesc.of("javax.lang.model.type.NoType");

  private static final ClassDesc CD_NullType = ClassDesc.of("javax.lang.model.type.NullType");

  private static final ClassDesc CD_PackageElement = ClassDesc.of("javax.lang.model.element.PackageElement");

  private static final ClassDesc CD_PrimitiveType = ClassDesc.of("javax.lang.model.type.PrimitiveType");

  private static final ClassDesc CD_TypeElement = ClassDesc.of("javax.lang.model.element.TypeElement");

  private static final ClassDesc CD_TypeKind = ClassDesc.of("javax.lang.model.type.TypeKind");

  private static final ClassDesc CD_TypeMirror = ClassDesc.of("javax.lang.model.type.TypeMirror");

  private static final ClassDesc CD_WildcardType = ClassDesc.of("javax.lang.model.type.WildcardType");

  private static final CountDownLatch initLatch = new CountDownLatch(1);

  private static volatile ProcessingEnvironment pe;

  static {
    initialize();
  }

  private Lang() {
    super();
  }

  public static final Elements elements() {
    return pe().getElementUtils();
  }

  public static final Locale locale() {
    return pe().getLocale();
  }

  public static final Types types() {
    return pe().getTypeUtils();
  }

  public static final Optional<? extends ConstantDesc> describeConstable(final Name n) {
    if (n == null) {
      return Optional.of(NULL);
    } else if (n instanceof Constable c) {
      return c.describeConstable();
    } else if (n instanceof ConstantDesc cd) {
      // Future proofing?
      return Optional.of(cd);
    }
    return Optional.of(DynamicConstantDesc.of(BSM_INVOKE,
                                              MethodHandleDesc.ofMethod(STATIC,
                                                                        CD_Lang,
                                                                        "name",
                                                                        MethodTypeDesc.of(CD_Name,
                                                                                          CD_CharSequence)),
                                              n.toString()));
  }

  public static final Optional<? extends ConstantDesc> describeConstable(final ModuleElement e) {
    if (e == null) {
      return Optional.of(NULL);
    } else if (e instanceof Constable c) {
      return c.describeConstable();
    } else if (e instanceof ConstantDesc cd) {
      // Future proofing?
      return Optional.of(cd);
    }
    return describeConstable(e.getQualifiedName())
      .map(nameDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                              MethodHandleDesc.ofMethod(STATIC,
                                                                        CD_Lang,
                                                                        "moduleElement",
                                                                        MethodTypeDesc.of(CD_ModuleElement,
                                                                                          CD_CharSequence)),
                                              nameDesc));
  }

  public static final Optional<? extends ConstantDesc> describeConstable(final PackageElement e) {
    if (e == null) {
      return Optional.of(NULL);
    } else if (e instanceof Constable c) {
      return c.describeConstable();
    } else if (e instanceof ConstantDesc cd) {
      // Future proofing?
      return Optional.of(cd);
    }
    return describeConstable(moduleOf(e))
      .flatMap(moduleDesc -> describeConstable(e.getQualifiedName())
               .map(nameDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                       MethodHandleDesc.ofMethod(STATIC,
                                                                                 CD_Lang,
                                                                                 "packageElement",
                                                                                 MethodTypeDesc.of(CD_PackageElement,
                                                                                                   CD_ModuleElement,
                                                                                                   CD_CharSequence)),
                                                       moduleDesc,
                                                       nameDesc)));
  }

  public static final Optional<? extends ConstantDesc> describeConstable(final TypeElement e) {
    if (e == null) {
      return Optional.of(NULL);
    } else if (e instanceof Constable c) {
      return c.describeConstable();
    } else if (e instanceof ConstantDesc cd) {
      // Future proofing?
      return Optional.of(cd);
    }
    return describeConstable(moduleOf(e))
      .flatMap(moduleDesc -> describeConstable(e.getQualifiedName())
               .map(nameDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                       MethodHandleDesc.ofMethod(STATIC,
                                                                                 CD_Lang,
                                                                                 "typeElement",
                                                                                 MethodTypeDesc.of(CD_TypeElement,
                                                                                                   CD_ModuleElement,
                                                                                                   CD_CharSequence)),
                                                       moduleDesc,
                                                       nameDesc)));
  }

  public static final Optional<? extends ConstantDesc> describeConstable(final TypeMirror t) {
    return switch (t.getKind()) {
    case ARRAY -> describeConstable((ArrayType)t);
    case BOOLEAN, BYTE, CHAR, DOUBLE, FLOAT, INT, LONG, SHORT -> describeConstable((PrimitiveType)t);
    case DECLARED, ERROR -> describeConstable((DeclaredType)t);
    case MODULE, NONE, PACKAGE, VOID -> describeConstable((NoType)t);
    case NULL -> describeConstable((NullType)t);
    case WILDCARD -> describeConstable((WildcardType)t);
    default -> throw new IllegalArgumentException("Unhandled TypeMirror: " + t);
    };
  }

  public static final Optional<? extends ConstantDesc> describeConstable(final ArrayType t) {
    if (t == null) {
      return Optional.of(NULL);
    } else if (t instanceof Constable c) {
      return c.describeConstable();
    } else if (t instanceof ConstantDesc cd) {
      // Future proofing?
      return Optional.of(cd);
    }
    return describeConstable(t.getComponentType())
      .map(componentTypeDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                       MethodHandleDesc.ofMethod(STATIC,
                                                                                 CD_Lang,
                                                                                 "arrayType",
                                                                                 MethodTypeDesc.of(CD_ArrayType,
                                                                                                   CD_TypeMirror)),
                                                       componentTypeDesc));
  }

  public static final Optional<? extends ConstantDesc> describeConstable(final DeclaredType t) {
    if (t == null) {
      return Optional.of(NULL);
    } else if (t instanceof Constable c) {
      return c.describeConstable();
    } else if (t instanceof ConstantDesc cd) {
      // Future proofing?
      return Optional.of(cd);
    } else if (t.getKind() == TypeKind.ERROR) {
      return Optional.empty();
    }
    // Ugh; this is tricky thanks to varargs. We'll do it imperatively for clarity.
    final TypeMirror enclosingType = t.getEnclosingType();
    final ConstantDesc enclosingTypeDesc =
      enclosingType.getKind() == TypeKind.NONE ? NULL : describeConstable(enclosingType).orElse(null);
    if (enclosingTypeDesc == null) {
      return Optional.empty();
    }
    final ConstantDesc typeElementDesc = describeConstable((TypeElement)t.asElement()).orElse(null);
    if (typeElementDesc == null) {
      return Optional.empty();
    }
    final List<? extends TypeMirror> typeArguments = t.getTypeArguments();
    final ConstantDesc[] cds = new ConstantDesc[typeArguments.size() + 3];
    cds[0] = MethodHandleDesc.ofMethod(STATIC,
                                       CD_Lang,
                                       "declaredType",
                                       MethodTypeDesc.of(CD_DeclaredType,
                                                         CD_DeclaredType,
                                                         CD_TypeElement,
                                                         CD_TypeMirror.arrayType()));
    cds[1] = enclosingTypeDesc;
    cds[2] = typeElementDesc;
    for (int i = 3; i < cds.length; i++) {
      final ConstantDesc cd = describeConstable(typeArguments.get(i)).orElse(null);
      if (cd == null) {
        return Optional.empty();
      }
      cds[i] = cd;
    }
    return Optional.of(DynamicConstantDesc.of(BSM_INVOKE, cds));
  }

  public static final Optional<? extends ConstantDesc> describeConstable(final NoType t) {
    if (t == null) {
      return Optional.of(NULL);
    } else if (t instanceof Constable c) {
      return c.describeConstable();
    } else if (t instanceof ConstantDesc cd) {
      // Future proofing?
      return Optional.of(cd);
    }
    return Optional.of(DynamicConstantDesc.of(BSM_INVOKE,
                                              MethodHandleDesc.ofMethod(STATIC,
                                                                        CD_Lang,
                                                                        "noType",
                                                                        MethodTypeDesc.of(CD_NoType,
                                                                                          CD_TypeKind)),
                                              t.getKind().describeConstable().orElseThrow()));
  }

  public static final Optional<? extends ConstantDesc> describeConstable(final NullType t) {
    if (t == null) {
      return Optional.of(NULL);
    } else if (t instanceof Constable c) {
      return c.describeConstable();
    } else if (t instanceof ConstantDesc cd) {
      // Future proofing?
      return Optional.of(cd);
    }
    return Optional.of(DynamicConstantDesc.of(BSM_INVOKE,
                                              MethodHandleDesc.ofMethod(STATIC,
                                                                        CD_Lang,
                                                                        "nullType",
                                                                        MethodTypeDesc.of(CD_NullType))));
  }

  public static final Optional<? extends ConstantDesc> describeConstable(final PrimitiveType t) {
    if (t == null) {
      return Optional.of(NULL);
    } else if (t instanceof Constable c) {
      return c.describeConstable();
    } else if (t instanceof ConstantDesc cd) {
      // Future proofing?
      return Optional.of(cd);
    }
    return Optional.of(DynamicConstantDesc.of(BSM_INVOKE,
                                              MethodHandleDesc.ofMethod(STATIC,
                                                                        CD_Lang,
                                                                        "primitiveType",
                                                                        MethodTypeDesc.of(CD_PrimitiveType,
                                                                                          CD_TypeKind)),
                                              t.getKind().describeConstable().orElseThrow()));

  }

  public static final Optional<? extends ConstantDesc> describeConstable(final WildcardType t) {
    if (t == null) {
      return Optional.of(NULL);
    } else if (t instanceof Constable c) {
      return c.describeConstable();
    } else if (t instanceof ConstantDesc cd) {
      // Future proofing?
      return Optional.of(cd);
    }
    final TypeMirror extendsBound = t.getExtendsBound();
    final ConstantDesc extendsBoundDesc = extendsBound == null ? NULL : describeConstable(extendsBound).orElse(null);
    if (extendsBoundDesc == null) {
      return Optional.empty();
    }
    final TypeMirror superBound = t.getSuperBound();
    final ConstantDesc superBoundDesc = superBound == null ? NULL : describeConstable(superBound).orElse(null);
    if (superBoundDesc == null) {
      return Optional.empty();
    }
    return Optional.of(DynamicConstantDesc.of(BSM_INVOKE,
                                              MethodHandleDesc.ofMethod(STATIC,
                                                                        CD_Lang,
                                                                        "wildcardType",
                                                                        MethodTypeDesc.of(CD_WildcardType,
                                                                                          CD_TypeMirror,
                                                                                          CD_TypeMirror)),
                                              extendsBoundDesc,
                                              superBoundDesc));
  }

  public static final ModuleElement moduleElement(final CharSequence moduleName) {
    return elements().getModuleElement(moduleName);
  }

  public static final ModuleElement moduleOf(final Element e) {
    return elements().getModuleOf(e);
  }

  public static final Name name(final CharSequence name) {
    return elements().getName(name);
  }

  public static final PackageElement packageElement(final ModuleElement moduleElement, final CharSequence fullyQualifiedName) {
    return elements().getPackageElement(moduleElement, fullyQualifiedName);
  }

  public static final PackageElement packageOf(final Element e) {
    return elements().getPackageOf(e);
  }

  // Called by describeConstable().
  public static final ArrayType arrayType(final TypeMirror t) {
    return types().getArrayType(t);
  }

  public static final DeclaredType declaredType(final DeclaredType containingType,
                                                final TypeElement typeElement,
                                                final TypeMirror... typeArguments) {
    return types().getDeclaredType(containingType, typeElement, typeArguments);
  }

  public static final NoType noType(final TypeKind k) {
    return types().getNoType(k);
  }

  // Called by describeConstable
  public static final NullType nullType() {
    return types().getNullType();
  }

  // Called by describeConstable().
  public static final PrimitiveType primitiveType(final TypeKind k) {
    return types().getPrimitiveType(k);
  }

  public static final TypeElement typeElement(final ModuleElement moduleElement, final CharSequence fullyQualifiedName) {
    return elements().getTypeElement(moduleElement, fullyQualifiedName);
  }

  public static final WildcardType wildcardType(final TypeMirror extendsBound, final TypeMirror superBound) {
    return types().getWildcardType(extendsBound, superBound);
  }


  /*
   * Private static methods.
   */


  static final ProcessingEnvironment pe() {
    ProcessingEnvironment pe = Lang.pe; // volatile read
    if (pe == null) {
      try {
        initLatch.await();
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      if ((pe = Lang.pe) == null) { // volatile read
        throw new IllegalStateException();
      }
    }
    return pe;
  }

  // Called once, ever, by the static initializer.
  private static final void initialize() {
    if (pe != null) { // volatile read
      return;
    }
    final CountDownLatch runningLatch = new CountDownLatch(1);
    final class P extends AbstractProcessor {

      private P() {
        super();
      }

      @Override
      public final void init(final ProcessingEnvironment pe) {
        Lang.pe = pe; // volatile write
        initLatch.countDown();
        try {
          runningLatch.await();
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

    };

    final Thread t = new Thread(() -> {
        try {
          final JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
          if (jc == null) {
            return;
          }
          // (Any "loading" is actually performed by, e.g. com.sun.tools.javac.jvm.ClassReader.fillIn(), not reflective
          // machinery.)
          final CompilationTask task =
            jc.getTask(null, // additionalOutputWriter
                       null, // fileManager,
                       null, // diagnosticListener,
                       Boolean.getBoolean("org.microbean.lang.Lang.verbose") ? List.of("-proc:only", "-sourcepath", "", "-verbose") : List.of("-proc:only", "-sourcepath", ""),
                       List.of("java.lang.annotation.RetentionPolicy"), // loads the least amount of stuff up front
                       null); // compilation units; null means we aren't actually compiling anything
          task.setProcessors(List.of(new P()));
          if (Boolean.FALSE.equals(task.call())) {
            pe = null; // volatile write
            runningLatch.countDown();
            initLatch.countDown();
          }
        } catch (final RuntimeException | Error e) {
          e.printStackTrace();
          pe = null; // volatile write
          runningLatch.countDown();
          initLatch.countDown();
          throw e;
        }
    }, "Lang");
    t.setDaemon(true); // critical
    t.start();
  }


}
