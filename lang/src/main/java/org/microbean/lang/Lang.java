/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2023–2024 microBean™.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.io.UncheckedIOException;

import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import java.lang.module.ModuleFinder;
import java.lang.module.ResolvedModule;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import java.net.URI;

import java.nio.charset.Charset;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.SourceVersion;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.microbean.constant.Constables;

import org.microbean.lang.element.DelegatingElement;

import org.microbean.lang.type.DelegatingTypeMirror;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.WARNING;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;
import static java.lang.constant.ConstantDescs.CD_List;
import static java.lang.constant.ConstantDescs.NULL;
import static java.lang.constant.DirectMethodHandleDesc.Kind.STATIC;
import static java.lang.constant.DirectMethodHandleDesc.Kind.STATIC_GETTER;

import static javax.lang.model.util.ElementFilter.constructorsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;

import static org.microbean.lang.ConstantDescs.CD_ArrayType;
import static org.microbean.lang.ConstantDescs.CD_CharSequence;
import static org.microbean.lang.ConstantDescs.CD_DeclaredType;
import static org.microbean.lang.ConstantDescs.CD_ExecutableElement;
import static org.microbean.lang.ConstantDescs.CD_Lang;
import static org.microbean.lang.ConstantDescs.CD_ModuleElement;
import static org.microbean.lang.ConstantDescs.CD_Name;
import static org.microbean.lang.ConstantDescs.CD_NoType;
import static org.microbean.lang.ConstantDescs.CD_NullType;
import static org.microbean.lang.ConstantDescs.CD_PackageElement;
import static org.microbean.lang.ConstantDescs.CD_PrimitiveType;
import static org.microbean.lang.ConstantDescs.CD_TypeElement;
import static org.microbean.lang.ConstantDescs.CD_TypeKind;
import static org.microbean.lang.ConstantDescs.CD_TypeMirror;
import static org.microbean.lang.ConstantDescs.CD_WildcardType;

/**
 * A utility class for working with the {@link AnnotatedConstruct javax.lang.model.*} packages at runtime.
 *
 * @author <a href="https://about.me/lairdnelson/" target="_top">Laird Nelson</a>
 */
public final class Lang {


  /*
   * Static fields.
   */


  private static final TypeMirror[] EMPTY_TYPEMIRROR_ARRAY = new TypeMirror[0];

  private static final Logger LOGGER = System.getLogger(Lang.class.getName());

  // Flags pulled from the Java Virtual Machine Specification, version 20, stored in 16 bits:

  private static final int ACC_PUBLIC =       1 << 0;   // 0x0001 // class, field, method
  private static final int ACC_PRIVATE =      1 << 1;   // 0x0002 // field, method
  private static final int ACC_PROTECTED =    1 << 2;   // 0x0004 // field, method
  private static final int ACC_STATIC =       1 << 3;   // 0x0008 // field, method
  private static final int ACC_FINAL =        1 << 4;   // 0x0010 // class, field, method

  private static final int ACC_OPEN =         1 << 5;   // 0x0020 // module               // same as ACC_SYNCHRONIZED, ACC_SUPER, ACC_TRANSITIVE
  private static final int ACC_SUPER =        1 << 5;   // 0x0020 // class                // same as ACC_SYNCHRONIZED, ACC_OPEN, ACC_TRANSITIVE
  private static final int ACC_SYNCHRONIZED = 1 << 5;   // 0x0020 // method               // same as ACC_SUPER, ACC_OPEN
  private static final int ACC_TRANSITIVE =   1 << 5;   // 0x0020 // module               // same as ACC_SYNCHRONIZED, ACC_SUPER, ACC_OPEN

  private static final int ACC_BRIDGE =       1 << 6;   // 0x0040 // method               // same as ACC_VOLATILE, ACC_STATIC_PHASE
  private static final int ACC_STATIC_PHASE = 1 << 6;   // 0x0040 // module               // same as ACC_BRIDGE, ACC_VOLATILE
  private static final int ACC_VOLATILE =     1 << 6;   // 0x0040 // field                // same as ACC_BRIDGE, ACC_STATIC_PHASE

  private static final int ACC_TRANSIENT =    1 << 7;   // 0x0080 // field                // same as ACC_VARARGS
  private static final int ACC_VARARGS =      1 << 7;   // 0x0080 // method               // same as ACC_TRANSIENT

  private static final int ACC_NATIVE =       1 << 8;   // 0x0100 // method
  private static final int ACC_INTERFACE =    1 << 9;   // 0x0200 // class
  private static final int ACC_ABSTRACT =     1 << 10;  // 0x0400 // class, method
  private static final int ACC_STRICTFP =     1 << 11;  // 0x0800 // method
  private static final int ACC_SYNTHETIC =    1 << 12;  // 0x1000 // class, field, method
  private static final int ACC_ANNOTATION =   1 << 13;  // 0x2000 // class
  private static final int ACC_ENUM =         1 << 14;  // 0x4000 // class, field

  private static final int ACC_MANDATED =     1 << 15;  // 0x8000 // module-info.class    // same as ACC_MODULE
  private static final int ACC_MODULE =       1 << 15;  // 0x8000 // module-info.class    // same as ACC_MANDATED

  // Flags pulled from the ASM library (https://gitlab.ow2.org/asm/asm/-/blob/master/asm/src/main/java/org/objectweb/asm/Opcodes.java):

  private static final int ASM_RECORD =       1 << 16;  // 0x10000 // class // (see for example https://github.com/raphw/byte-buddy/blob/byte-buddy-1.14.5/byte-buddy-dep/src/main/java/net/bytebuddy/pool/TypePool.java#L2949)

  // Not needed:
  // private static final int ASM_DEPRECATED =   1 << 17;  // 0x20000 // class

  // Flags pulled from the javac compiler and the innards of java.lang.reflect.Modifier. Not sure we need these.
  /*
  private static final long JAVAC_PARAMETER =                  1L << 33; // 0x0000000200000000
  private static final long JAVAC_DEFAULT =                    1L << 43; // 0x0000080000000000
  private static final long JAVAC_COMPACT_RECORD_CONSTRUCTOR = 1L << 51; // 0x0008000000000000 // same as JAVAC_MODULE
  private static final long JAVAC_RECORD =                     1L << 61; // 0x2000000000000000
  private static final long JAVAC_SEALED =                     1L << 62; // 0x4000000000000000
  private static final long JAVAC_NON_SEALED =                 1L << 63; // 0x8000000000000000
  */

  private static final Map<Modifier, Long> modifierMasks;

  private static final CountDownLatch initLatch = new CountDownLatch(1);

  private static final boolean lockNames = Boolean.parseBoolean(System.getProperty("org.microbean.lang.lockNames", "true"));

  // For debugging only
  private static final Field modulesField;

  // For debugging only
  private static final Method getDefaultModuleMethod;

  static {
    try {
      // For debugging only.
      getDefaultModuleMethod = com.sun.tools.javac.comp.Modules.class.getDeclaredMethod("getDefaultModule");
      getDefaultModuleMethod.trySetAccessible();

      // For debugging only.
      modulesField = com.sun.tools.javac.model.JavacElements.class.getDeclaredField("modules");
      modulesField.trySetAccessible();
    } catch (final ReflectiveOperationException x) {
      throw new ExceptionInInitializerError(x);
    }
  }

  private static final int INITIALIZATION_ERROR = -1;

  private static final int UNINITIALIZED = 0;

  private static final int INITIALIZING = 1;

  private static final int INITIALIZED = 2;

  private static final VarHandle STATE;

  private static volatile int state;

  private static volatile ProcessingEnvironment pe;

  static {
    final EnumMap<Modifier, Long> m = new EnumMap<>(Modifier.class);
    m.put(Modifier.ABSTRACT, Long.valueOf(ACC_ABSTRACT));
    // m.put(Modifier.DEFAULT, Long.valueOf(JAVAC_DEFAULT)); // not Java Virtual Machine Specification-defined
    m.put(Modifier.DEFAULT, 0L);
    m.put(Modifier.FINAL, Long.valueOf(ACC_FINAL));
    m.put(Modifier.NATIVE, Long.valueOf(ACC_NATIVE));
    // m.put(Modifier.NON_SEALED, Long.valueOf(JAVAC_NON_SEALED)); // not Java Virtual Machine Specification-defined
    m.put(Modifier.NON_SEALED, 0L);
    m.put(Modifier.PRIVATE, Long.valueOf(ACC_PRIVATE));
    m.put(Modifier.PROTECTED, Long.valueOf(ACC_PROTECTED));
    m.put(Modifier.PUBLIC, Long.valueOf(ACC_PUBLIC));
    // m.put(Modifier.SEALED, Long.valueOf(JAVAC_SEALED)); // not Java Virtual Machine Specification-defined
    m.put(Modifier.SEALED, 0L);
    m.put(Modifier.STATIC, Long.valueOf(ACC_STATIC));
    m.put(Modifier.STRICTFP, Long.valueOf(ACC_STRICTFP));
    m.put(Modifier.SYNCHRONIZED, Long.valueOf(ACC_SYNCHRONIZED));
    m.put(Modifier.TRANSIENT, Long.valueOf(ACC_TRANSIENT));
    m.put(Modifier.VOLATILE, Long.valueOf(ACC_VOLATILE));
    assert m.size() == Modifier.values().length;
    modifierMasks = Collections.unmodifiableMap(m);
    try {
      STATE = MethodHandles.lookup().findStaticVarHandle(Lang.class, "state", int.class);
    } catch (final IllegalAccessException | NoSuchFieldException e) {
      throw new ExceptionInInitializerError(e);
    }
  }


  /*
   * Constructors.
   */


  private Lang() {
    super();
  }


  /*
   * Constable support methods.
   */


  public static final Optional<? extends ConstantDesc> describeConstable(final Name n) {
    return switch (n) {
    case null            -> Optional.of(NULL);
    case Constable c     -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default              -> Optional.of(DynamicConstantDesc.of(BSM_INVOKE,
                                                               MethodHandleDesc.ofMethod(STATIC,
                                                                                         CD_Lang,
                                                                                         "name",
                                                                                         MethodTypeDesc.of(CD_Name,
                                                                                                           CD_CharSequence)),
                                                               lockNames ? CompletionLock.guard(n::toString) : n.toString()));
    };
  }

  public static final Optional<? extends ConstantDesc> describeConstable(final AnnotatedConstruct a) {
    return switch (a) {
    case null         -> Optional.of(NULL);
    case Element e    -> describeConstable(e);
    case TypeMirror t -> describeConstable(t);
    default           -> throw new IllegalArgumentException("a: " + a);
    };
  }

  public static final Optional<? extends ConstantDesc> describeConstable(final Element e) {
    return e == null ? Optional.of(NULL) : switch (CompletionLock.guard(e::getKind)) {
    case CONSTRUCTOR, METHOD                     -> describeConstable((ExecutableElement)e);
    case MODULE                                  -> describeConstable((ModuleElement)e);
    case PACKAGE                                 -> describeConstable((PackageElement)e);
    case PARAMETER                               -> describeConstable((VariableElement)e);
    // TODO: others probably need to be handled but not as urgently
    case ElementKind ek when ek.isDeclaredType() -> describeConstable((TypeElement)e);
    default                                      -> Optional.empty();
    };
  }

  public static final Optional<? extends ConstantDesc> describeConstable(final ExecutableElement e) {
    return switch (e) {
    case null            -> Optional.of(NULL);
    case Constable c     -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default              -> CompletionLock.guard(() -> switch (e.getKind()) {
      case CONSTRUCTOR ->
        Constables.describeConstable(e.getParameters(), Lang::describeConstable)
        .flatMap(parametersDesc -> describeConstable(e.getEnclosingElement())
                 .map(declaringElementDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                                     MethodHandleDesc.ofMethod(STATIC,
                                                                                               CD_Lang,
                                                                                               "executableElement",
                                                                                               MethodTypeDesc.of(CD_TypeElement,
                                                                                                                 CD_List)),
                                                                     declaringElementDesc,
                                                                     parametersDesc)));
      case METHOD ->
        Constables.describeConstable(e.getParameters())
        .flatMap(parametersDesc -> describeConstable(e.getEnclosingElement())
                 .flatMap(declaringElementDesc -> describeConstable(e.getSimpleName())
                          .map(nameDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                                  MethodHandleDesc.ofMethod(STATIC,
                                                                                            CD_Lang,
                                                                                            "executableElement",
                                                                                            MethodTypeDesc.of(CD_TypeElement,
                                                                                                              CD_CharSequence,
                                                                                                              CD_List)),
                                                                  declaringElementDesc,
                                                                  nameDesc,
                                                                  parametersDesc))));
      default -> Optional.empty();
      });
    };
  }

  public static final Optional<? extends ConstantDesc> describeConstable(final ModuleElement e) {
    return switch (e) {
    case null            -> Optional.of(NULL);
    case Constable c     -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default -> describeConstable(e.getQualifiedName()) // getQualifiedName() does not cause symbol completion
      .map(nameDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                              MethodHandleDesc.ofMethod(STATIC,
                                                                        CD_Lang,
                                                                        "moduleElement",
                                                                        MethodTypeDesc.of(CD_ModuleElement,
                                                                                          CD_CharSequence)),
                                              nameDesc));
    };
  }

  public static final Optional<? extends ConstantDesc> describeConstable(final PackageElement e) {
    return switch (e) {
    case null            -> Optional.of(NULL);
    case Constable c     -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default              -> describeConstable(moduleOf(e))
      .flatMap(moduleDesc -> describeConstable(e.getQualifiedName()) // getQualifiedName() does not cause symbol completion
               .map(nameDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                       MethodHandleDesc.ofMethod(STATIC,
                                                                                 CD_Lang,
                                                                                 "packageElement",
                                                                                 MethodTypeDesc.of(CD_PackageElement,
                                                                                                   CD_ModuleElement,
                                                                                                   CD_CharSequence)),
                                                       moduleDesc,
                                                       nameDesc)));
    };
  }

  public static final Optional<? extends ConstantDesc> describeConstable(final TypeElement e) {
    return switch (e) {
    case null            -> Optional.of(NULL);
    case Constable c     -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default              -> describeConstable(e.getQualifiedName()) // getQualifiedName() does not cause symbol completion
      .map(nameDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                              MethodHandleDesc.ofMethod(STATIC,
                                                                        CD_Lang,
                                                                        "typeElement",
                                                                        MethodTypeDesc.of(CD_TypeElement,
                                                                                          CD_CharSequence)),
                                              nameDesc));
    };
  }

  public static final Optional<? extends ConstantDesc> describeConstable(final VariableElement e) {
    return switch (e) {
    case null            -> Optional.of(NULL);
    case Constable c     -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default              -> CompletionLock.guard(() -> switch (e.getKind()) {
      case FIELD -> describeConstable(e.getSimpleName())
        .flatMap(nameDesc -> describeConstable(e.getEnclosingElement())
                 .map(declaringClassDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                                   MethodHandleDesc.ofMethod(STATIC,
                                                                                             CD_Lang,
                                                                                             "variableElement",
                                                                                             MethodTypeDesc.of(CD_TypeElement,
                                                                                                               CD_CharSequence)),
                                                                   declaringClassDesc,
                                                                   nameDesc)));
      case PARAMETER -> describeConstable(e.getSimpleName())
        .flatMap(nameDesc -> describeConstable(e.getEnclosingElement())
                 .map(declaringExecutableDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                                        MethodHandleDesc.ofMethod(STATIC,
                                                                                                  CD_Lang,
                                                                                                  "variableElement",
                                                                                                  MethodTypeDesc.of(CD_ExecutableElement,
                                                                                                                    CD_CharSequence)),
                                                                        declaringExecutableDesc,
                                                                        nameDesc)));
      default -> Optional.empty();
      });
    };
  }

  public static final Optional<? extends ConstantDesc> describeConstable(final TypeMirror t) {
    return t == null ? Optional.of(NULL) : switch (CompletionLock.guard(t::getKind)) {
      case ARRAY                                                -> describeConstable((ArrayType)t);
      case BOOLEAN, BYTE, CHAR, DOUBLE, FLOAT, INT, LONG, SHORT -> describeConstable((PrimitiveType)t);
      case DECLARED, ERROR                                      -> describeConstable((DeclaredType)t);
      case EXECUTABLE, INTERSECTION, OTHER, TYPEVAR, UNION      -> Optional.empty();
      case MODULE, NONE, PACKAGE, VOID                          -> describeConstable((NoType)t);
      case NULL                                                 -> describeConstable((NullType)t);
      case WILDCARD                                             -> describeConstable((WildcardType)t);
    };
  }

  public static final Optional<? extends ConstantDesc> describeConstable(final ArrayType t) {
    return switch (t) {
    case null            -> Optional.of(null);
    case Constable c     -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default              -> describeConstable(CompletionLock.guard(t::getComponentType))
      .map(componentTypeDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                       MethodHandleDesc.ofMethod(STATIC,
                                                                                 CD_Lang,
                                                                                 "arrayTypeOf",
                                                                                 MethodTypeDesc.of(CD_ArrayType,
                                                                                                   CD_TypeMirror)),
                                                       componentTypeDesc));
    };
  }

  public static final Optional<? extends ConstantDesc> describeConstable(final DeclaredType t) {
    return switch (t) {
    case null                                                                   -> Optional.of(NULL);
    case Constable c                                                            -> c.describeConstable();
    case ConstantDesc cd                                                        -> Optional.of(cd); // future proofing?
    case DeclaredType e when CompletionLock.guard(e::getKind) == TypeKind.ERROR -> Optional.empty();
    default                                                -> {
      // Ugh; this is tricky thanks to varargs and NONE/null silliness. We'll do it imperatively for clarity.
      final ConstantDesc[] cds;
      CompletionLock.acquire();
      try {
        final TypeMirror enclosingType = t.getEnclosingType();
        final ConstantDesc enclosingTypeDesc =
          enclosingType.getKind() == TypeKind.NONE ? NULL : describeConstable(enclosingType).orElseThrow();
        final ConstantDesc typeElementDesc = describeConstable((TypeElement)t.asElement()).orElseThrow();
        final List<? extends TypeMirror> typeArguments = t.getTypeArguments();
        cds = new ConstantDesc[typeArguments.size() + 3];
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
          cds[i] = describeConstable(typeArguments.get(i)).orElseThrow();
        }
      } finally {
        CompletionLock.release();
      }
      yield Optional.of(DynamicConstantDesc.of(BSM_INVOKE, cds));
    }
    };
  }

  public static final Optional<? extends ConstantDesc> describeConstable(final NoType t) {
    return switch (t) {
    case null            -> Optional.of(NULL);
    case Constable c     -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default              -> Optional.of(DynamicConstantDesc.of(BSM_INVOKE,
                                                               MethodHandleDesc.ofMethod(STATIC,
                                                                                         CD_Lang,
                                                                                         "noType",
                                                                                         MethodTypeDesc.of(CD_NoType,
                                                                                                           CD_TypeKind)),
                                                               CompletionLock.guard(t::getKind).describeConstable().orElseThrow()));
    };
  }

  public static final Optional<? extends ConstantDesc> describeConstable(final NullType t) {
    return switch (t) {
    case null            -> Optional.of(NULL);
    case Constable c     -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default              -> Optional.of(DynamicConstantDesc.of(BSM_INVOKE,
                                                               MethodHandleDesc.ofMethod(STATIC,
                                                                                         CD_Lang,
                                                                                         "nullType",
                                                                                         MethodTypeDesc.of(CD_NullType))));
    };
  }

  public static final Optional<? extends ConstantDesc> describeConstable(final PrimitiveType t) {
    return switch (t) {
    case null            -> Optional.of(NULL);
    case Constable c     -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default              -> Optional.of(DynamicConstantDesc.of(BSM_INVOKE,
                                                               MethodHandleDesc.ofMethod(STATIC,
                                                                                         CD_Lang,
                                                                                         "primitiveType",
                                                                                         MethodTypeDesc.of(CD_PrimitiveType,
                                                                                                           CD_TypeKind)),
                                                               CompletionLock.guard(t::getKind).describeConstable().orElseThrow()));
    };
  }

  public static final Optional<? extends ConstantDesc> describeConstable(final WildcardType t) {
    return switch (t) {
    case null            -> Optional.of(NULL);
    case Constable c     -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing?
    default              -> describeConstable(CompletionLock.guard(t::getExtendsBound))
      .flatMap(extendsBoundDesc -> describeConstable(CompletionLock.guard(t::getSuperBound))
               .map(superBoundDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                             MethodHandleDesc.ofMethod(STATIC,
                                                                                       CD_Lang,
                                                                                       "wildcardType",
                                                                                       MethodTypeDesc.of(CD_WildcardType,
                                                                                                         CD_TypeMirror,
                                                                                                         CD_TypeMirror)),
                                                             extendsBoundDesc,
                                                             superBoundDesc)));
    };
  }


  /*
   * Type and element support methods.
   */


  // Apparently only one Symbol completion can occur at any time. Good grief.
  //
  // Sample stack trace:
  /*
    java.lang.AssertionError: Filling jrt:/java.base/java/io/Serializable.class during DirectoryFileObject[/modules/java.base:java/lang/CharSequence.class]
        at jdk.compiler/com.sun.tools.javac.util.Assert.error(Assert.java:162)
        at jdk.compiler/com.sun.tools.javac.code.ClassFinder.fillIn(ClassFinder.java:365)
        at jdk.compiler/com.sun.tools.javac.code.ClassFinder.complete(ClassFinder.java:301)
        at jdk.compiler/com.sun.tools.javac.code.Symtab$1.complete(Symtab.java:326)
        at jdk.compiler/com.sun.tools.javac.code.Symbol.complete(Symbol.java:682)
        at jdk.compiler/com.sun.tools.javac.code.Symbol$ClassSymbol.complete(Symbol.java:1410)
        at jdk.compiler/com.sun.tools.javac.code.Symbol.apiComplete(Symbol.java:688)
        at jdk.compiler/com.sun.tools.javac.code.Type$ClassType.getKind(Type.java:1181)
  */

  public static final Set<? extends ModuleElement> allModuleElements() {
    final Elements elements = pe().getElementUtils();
    final Set<ModuleElement> rv = new HashSet<>();
    CompletionLock.guard(elements::getAllModuleElements).forEach(me -> rv.add(wrap(me)));
    return Collections.unmodifiableSet(rv);
  }

  public static final Name binaryName(final TypeElement e) {
    // This does not cause completion.
    return pe().getElementUtils().getBinaryName(unwrap(e));
  }

  public static final boolean functionalInterface(final TypeElement e) {
    final TypeElement e2 = unwrap(e);
    final Elements elements = pe().getElementUtils();
    // JavacElements#isFunctionalInterface(Element) calls Element#getKind().
    return CompletionLock.guard(() -> elements.isFunctionalInterface(e2));
  }

  public static final boolean generic(final Element e) {
    CompletionLock.acquire();
    try {
      return switch (e.getKind()) {
      case CLASS, CONSTRUCTOR, ENUM, INTERFACE, METHOD, RECORD -> !((Parameterizable)e).getTypeParameters().isEmpty();
      default                                                  -> false;
      };
    } finally {
      CompletionLock.release();
    }
  }

  public static final TypeMirror capture(TypeMirror t) {
    t = unwrap(t);
    // JavacTypes#capture(TypeMirror) calls TypeMirror#getKind().
    final Types types = pe().getTypeUtils();
    final TypeMirror rv;
    CompletionLock.acquire();
    try {
      rv = types.capture(t);
    } finally {
      CompletionLock.release();
    }
    return wrap(rv);
  }

  public static final boolean contains(TypeMirror t, TypeMirror s) {
    t = unwrap(t);
    s = unwrap(s);
    // JavacTypes#contains(TypeMirror, TypeMirror) calls TypeMirror#getKind().
    final Types types = pe().getTypeUtils();
    CompletionLock.acquire();
    try {
      return types.contains(t, s);
    } finally {
      CompletionLock.release();
    }
  }

  public static final Element element(TypeMirror t) {
    t = unwrap(t);
    final Types types = pe().getTypeUtils();
    final Element rv;
    // JavacTypes#asElement(TypeMirror) calls TypeMirror#getKind().
    CompletionLock.acquire();
    try {
      rv = types.asElement(t);
    } finally {
      CompletionLock.release();
    }
    return wrap(rv);
  }

  public static final TypeMirror memberOf(DeclaredType t, Element e) {
    t = unwrap(t);
    e = unwrap(e);
    final Types types = pe().getTypeUtils();
    final TypeMirror rv;
    CompletionLock.acquire();
    try {
      rv = types.asMemberOf(t, e);
    } finally {
      CompletionLock.release();
    }
    return wrap(rv);
  }

  public static final TypeMirror box(final TypeMirror t) {
    final TypeKind k = CompletionLock.guard(t::getKind);
    return k.isPrimitive() ? boxedClass((PrimitiveType)t).asType() : t;
  }

  public static final TypeMirror unbox(final TypeMirror t) {
    return unboxedType(t);
  }

  public static final TypeElement boxedClass(final PrimitiveType t) {
    // JavacTypes#boxedClass(TypeMirror) eventually calls com.sun.tools.javac.code.Symtab#defineClass(Name, Symbol) but
    // that doesn't seem to actually do completion.
    return wrap(pe().getTypeUtils().boxedClass(unwrap(t)));
  }

  public static final boolean bridge(final Element e) {
    final Elements elements = pe().getElementUtils();
    CompletionLock.acquire();
    try {
      return e.getKind() == ElementKind.METHOD && elements.isBridge(unwrap((ExecutableElement)e));
    } finally {
      CompletionLock.release();
    }
  }

  public static final boolean compactConstructor(final Element e) {
    final Elements elements = pe().getElementUtils();
    CompletionLock.acquire();
    try {
      return e.getKind() == ElementKind.CONSTRUCTOR && elements.isCompactConstructor(unwrap((ExecutableElement)e));
    } finally {
      CompletionLock.release();
    }
  }

  public static final boolean canonicalConstructor(final Element e) {
    Objects.requireNonNull(e, "e");
    final Elements elements = pe().getElementUtils();
    CompletionLock.acquire();
    try {
      return e.getKind() == ElementKind.CONSTRUCTOR && elements.isCanonicalConstructor(unwrap((ExecutableElement)e));
    } finally {
      CompletionLock.release();
    }
  }

  public static final PrimitiveType unboxedType(TypeMirror t) {
    t = unwrap(t);
    final Types types = pe().getTypeUtils();
    // JavacTypes#unboxedType(TypeMirror) calls TypeMirror#getKind().
    CompletionLock.acquire();
    try {
      return types.unboxedType(t);
    } finally {
      CompletionLock.release();
    }
  }


  /*
   * JVMS productions related to signatures and descriptors.
   */


  public static final String elementSignature(final Element e) {
    return switch (CompletionLock.guard(e::getKind)) {
    case CLASS, ENUM, INTERFACE, RECORD                                    -> classSignature((TypeElement)e);
    case CONSTRUCTOR, METHOD, INSTANCE_INIT, STATIC_INIT                   -> methodSignature((ExecutableElement)e);
    case ENUM_CONSTANT, FIELD, LOCAL_VARIABLE, PARAMETER, RECORD_COMPONENT -> fieldSignature(e);
    default                                                                -> throw new IllegalArgumentException("e: " + e);
    };
  }

  private static final String classSignature(final TypeElement e) {
    CompletionLock.acquire();
    try {
      return switch (e.getKind()) {
      case CLASS, ENUM, INTERFACE, RECORD -> {
        if (!generic(e) && ((DeclaredType)e.getSuperclass()).getTypeArguments().isEmpty()) {
          boolean signatureRequired = false;
          for (final TypeMirror iface : e.getInterfaces()) {
            if (!((DeclaredType)iface).getTypeArguments().isEmpty()) {
              signatureRequired = true;
              break;
            }
          }
          if (!signatureRequired) {
            yield null;
          }
        }
        final StringBuilder sb = new StringBuilder();
        classSignature(e, sb);
        yield sb.toString();
      }
      default -> throw new IllegalArgumentException("e: " + e + "; kind: " + e.getKind());
      };
    } finally {
      CompletionLock.release();
    }
  }

  private static final void classSignature(final TypeElement e, final StringBuilder sb) {
    CompletionLock.acquire();
    try {
      switch (e.getKind()) {
      case CLASS, ENUM, INTERFACE, RECORD -> { // note: no ANNOTATION_TYPE on purpose
        typeParameters(e.getTypeParameters(), sb);
        final List<? extends TypeMirror> directSupertypes = directSupertypes(e.asType());
        if (directSupertypes.isEmpty()) {
          assert e.getQualifiedName().contentEquals("java.lang.Object") : "DeclaredType with no supertypes: " + e.asType();
          // See
          // https://stackoverflow.com/questions/76453947/in-the-jvms-what-is-the-classsignature-for-java-lang-object-given-that-supercl
          //
          // Do nothing (and thereby violate the grammar? Derp?).
        } else {
          final DeclaredType firstSupertype = (DeclaredType)directSupertypes.get(0);
          assert firstSupertype.getKind() == TypeKind.DECLARED;
          // "For an interface type with no direct super-interfaces, a type mirror representing java.lang.Object is
          // returned." Therefore in all situations, given a non-empty list of direct supertypes, the first element will
          // always be a non-interface class.
          assert !((TypeElement)firstSupertype.asElement()).getKind().isInterface() : "Contract violation";
          superclassSignature(firstSupertype, sb);
          superinterfaceSignatures(directSupertypes.subList(1, directSupertypes.size()), sb);
        }
      }
      default -> throw new IllegalArgumentException("e: " + e + "; kind: " + e.getKind());
      }
    } finally {
      CompletionLock.release();
    }
  }

  private static final String methodSignature(final ExecutableElement e) {
    CompletionLock.acquire();
    try {
      if (e.getKind().isExecutable()) {
        boolean throwsClauseRequired = false;
        for (final TypeMirror exceptionType : e.getThrownTypes()) {
          if (exceptionType.getKind() == TypeKind.TYPEVAR) {
            throwsClauseRequired = true;
            break;
          }
        }
        if (!throwsClauseRequired && !generic(e)) {
          final TypeMirror returnType = e.getReturnType();
          if (returnType.getKind() != TypeKind.TYPEVAR && typeArguments(returnType).isEmpty()) {
            boolean signatureRequired = false;
            for (final VariableElement p : e.getParameters()) {
              final TypeMirror parameterType = p.asType();
              if (parameterType.getKind() == TypeKind.TYPEVAR || !typeArguments(parameterType).isEmpty()) {
                signatureRequired = true;
                break;
              }
            }
            if (!signatureRequired) {
              return null;
            }
          }
        }
        final StringBuilder sb = new StringBuilder();
        methodSignature(e, sb, throwsClauseRequired);
        return sb.toString();
      } else {
        throw new IllegalArgumentException("e: " + e + "; kind: " + e.getKind());
      }
    } finally {
      CompletionLock.release();
    }
  }

  private static final void methodSignature(final ExecutableElement e, final StringBuilder sb, final boolean throwsClauseRequired) {
    CompletionLock.acquire();
    try {
      if (e.getKind().isExecutable()) {
        typeParameters(e.getTypeParameters(), sb);
        sb.append('(');
        parameterSignatures(e.getParameters(), sb);
        sb.append(')');
        final TypeMirror returnType = e.getReturnType();
        if (returnType.getKind() == TypeKind.VOID) {
          sb.append('V');
        } else {
          typeSignature(returnType, sb);
        }
        if (throwsClauseRequired) {
          throwsSignatures(e.getThrownTypes(), sb);
        }
      } else {
        throw new IllegalArgumentException("e: " + e + "; kind: " + e.getKind());
      }
    } finally {
      CompletionLock.release();
    }
  }

  private static final String fieldSignature(final Element e) {
    CompletionLock.acquire();
    try {
      return switch (e.getKind()) {
      case ENUM_CONSTANT, FIELD, LOCAL_VARIABLE, PARAMETER, RECORD_COMPONENT -> {
        final TypeMirror t = e.asType();
        if (t.getKind() != TypeKind.TYPEVAR && typeArguments(t).isEmpty()) {
          // TODO: is this sufficient? Or do we, for example, have to examine the type's supertypes to see if *they*
          // "use" a parameterized type? Maybe we have to look at the enclosing type too? But if so, why only here, and
          // why not the same sort of thing for the return type of a method (see above)?
          yield null;
        }
        final StringBuilder sb = new StringBuilder();
        fieldSignature(e, sb);
        yield sb.toString();
      }
      default -> throw new IllegalArgumentException("e: " + e + "; kind: " + e.getKind());
      };
    } finally {
      CompletionLock.release();
    }
  }

  private static final void fieldSignature(final Element e, final StringBuilder sb) {
    CompletionLock.acquire();
    try {
      switch (e.getKind()) {
      case ENUM_CONSTANT, FIELD, LOCAL_VARIABLE, PARAMETER, RECORD_COMPONENT -> typeSignature(e.asType(), sb);
      default                                                                -> throw new IllegalArgumentException("e: " + e);
      }
    } finally {
      CompletionLock.release();
    }
  }

  private static final void parameterSignatures(final List<? extends VariableElement> ps, final StringBuilder sb) {
    if (ps.isEmpty()) {
      return;
    }
    CompletionLock.acquire();
    try {
      for (final VariableElement p : ps) {
        if (p.getKind() != ElementKind.PARAMETER) {
          throw new IllegalArgumentException("ps: " + ps);
        }
        typeSignature(p.asType(), sb);
      }
    } finally {
      CompletionLock.release();
    }
  }

  private static final void throwsSignatures(final List<? extends TypeMirror> ts, final StringBuilder sb) {
    if (ts.isEmpty()) {
      return;
    }
    CompletionLock.acquire();
    try {
      for (final TypeMirror t : ts) {
        sb.append(switch (t.getKind()) {
          case DECLARED, TYPEVAR -> "^";
          default                -> throw new IllegalArgumentException("ts: " + ts);
          });
        typeSignature(t, sb);
      }
    } finally {
      CompletionLock.release();
    }
  }

  private static final void typeParameters(final List<? extends TypeParameterElement> tps, final StringBuilder sb) {
    if (tps.isEmpty()) {
      return;
    }
    sb.append('<');
    CompletionLock.acquire();
    try {
      for (final TypeParameterElement tp : tps) {
        switch (tp.getKind()) {
        case TYPE_PARAMETER -> typeParameter(tp, sb);
        default             -> throw new IllegalArgumentException("tps: " + tps);
        }
      }
    } finally {
      CompletionLock.release();
    }
    sb.append('>');
  }

  private static final void typeParameter(final TypeParameterElement e, final StringBuilder sb) {
    CompletionLock.acquire();
    try {
      if (e.getKind() != ElementKind.TYPE_PARAMETER) {
        throw new IllegalArgumentException("e: " + e);
      }
      final List<? extends TypeMirror> bounds = e.getBounds();
      sb.append(e.getSimpleName());
      if (bounds.isEmpty()) {
        sb.append(":java.lang.Object");
      } else {
        sb.append(':');
        classBound(bounds.get(0), sb);
      }
      interfaceBounds(bounds.subList(1, bounds.size()), sb);
    } finally {
      CompletionLock.release();
    }
  }

  private static final void classBound(final TypeMirror t, final StringBuilder sb) {
    CompletionLock.acquire();
    try {
      if (t.getKind() != TypeKind.DECLARED) {
        throw new IllegalArgumentException("t: " + t);
      }
      typeSignature(t, sb);
    } finally {
      CompletionLock.release();
    }
  }

  private static final void interfaceBounds(final List<? extends TypeMirror> ts, final StringBuilder sb) {
    if (ts.isEmpty()) {
      return;
    }
    CompletionLock.acquire();
    try {
      for (final TypeMirror t : ts) {
        interfaceBound(t, sb);
      }
    } finally {
      CompletionLock.release();
    }
  }

  @SuppressWarnings("fallthrough")
  private static final void interfaceBound(final TypeMirror t, final StringBuilder sb) {
    CompletionLock.acquire();
    try {
      switch (t.getKind()) {
      case DECLARED:
        if (((DeclaredType)t).asElement().getKind().isInterface()) {
          sb.append(':');
          typeSignature(t, sb);
          return;
        }
        // fall through
      default:
        throw new IllegalArgumentException("t: " + t);
      }
    } finally {
      CompletionLock.release();
    }
  }

  private static final void superclassSignature(final TypeMirror t, final StringBuilder sb) {
    classTypeSignature(t, sb);
  }

  private static final void superinterfaceSignatures(final List<? extends TypeMirror> ts, final StringBuilder sb) {
    if (ts.isEmpty()) {
      return;
    }
    CompletionLock.acquire();
    try {
      for (final TypeMirror t : ts) {
        superinterfaceSignature(t, sb);
      }
    } finally {
      CompletionLock.release();
    }
  }

  @SuppressWarnings("fallthrough")
  private static final void superinterfaceSignature(final TypeMirror t, final StringBuilder sb) {
    CompletionLock.acquire();
    try {
      switch (t.getKind()) {
      case DECLARED:
        if (((DeclaredType)t).asElement().getKind().isInterface()) {
          classTypeSignature(t, sb);
          return;
        }
        // fall through
      default:
        throw new IllegalArgumentException("t: " + t);
      }
    } finally {
      CompletionLock.release();
    }
  }

  public static final String typeSignature(final TypeMirror t) {
    final StringBuilder sb = new StringBuilder();
    typeSignature(t, sb);
    return sb.toString();
  }

  private static final void typeSignature(final TypeMirror t, final StringBuilder sb) {
    CompletionLock.acquire();
    try {
      switch (t.getKind()) {
      case ARRAY    -> typeSignature(((ArrayType)t).getComponentType(), sb.append("[")); // recursive
      case BOOLEAN  -> sb.append("Z");
      case BYTE     -> sb.append("B");
      case CHAR     -> sb.append("C");
      case DECLARED -> classTypeSignature((DeclaredType)t, sb);
      case DOUBLE   -> sb.append("D");
      case FLOAT    -> sb.append("F");
      case INT      -> sb.append("I");
      case LONG     -> sb.append("J");
      case SHORT    -> sb.append("S");
      case TYPEVAR  -> sb.append("T").append(((TypeVariable)t).asElement().getSimpleName()).append(';');
      default       -> throw new IllegalArgumentException("t: " + t);
      }
    } finally {
      CompletionLock.release();
    }
  }

  private static final void classTypeSignature(final TypeMirror t, final StringBuilder sb) {
    CompletionLock.acquire();
    try {
      switch (t.getKind()) {
      case NONE:
        return;
      case DECLARED:
        break;
      default:
        throw new IllegalArgumentException("t: " + t);
      }
      final DeclaredType dt = (DeclaredType)t;

      // Build a deque of elements from the package to the (possibly inner or nested) class.
      final Deque<Element> dq = new ArrayDeque<>();
      Element e = dt.asElement();
      while (e != null && e.getKind() != ElementKind.MODULE) {
        dq.push(e);
        e = e.getEnclosingElement();
      }

      sb.append("L");

      final Iterator<Element> i = dq.iterator();
      while (i.hasNext()) {
        e = i.next();
        switch (e.getKind()) {
        case PACKAGE:
          // java.lang becomes java/lang
          sb.append(((PackageElement)e).getQualifiedName().toString().replace('.', '/'));
          assert i.hasNext();
          sb.append('/');
          break;
        case ANNOTATION_TYPE:
        case CLASS:
        case ENUM:
        case INTERFACE:
        case RECORD:
          // Outer.Inner remains Outer.Inner (i.e. not Outer$Inner or Outer/Inner)
          sb.append(e.getSimpleName());
          if (i.hasNext()) {
            sb.append('.');
          }
          break;
        default:
          // note that a method could fall in here; we just skip it
          break;
        }
        i.remove();
      }
      assert dq.isEmpty();

      // Now for the type arguments
      final List<? extends TypeMirror> typeArguments = dt.getTypeArguments();
      if (!typeArguments.isEmpty()) {
        sb.append('<');
        for (final TypeMirror ta : typeArguments) {
          sb.append(switch (ta.getKind()) {
            case WILDCARD -> {
              final WildcardType w = (WildcardType)ta;
              final TypeMirror superBound = w.getSuperBound();
              if (superBound == null) {
                final TypeMirror extendsBound = w.getExtendsBound();
                if (extendsBound == null) {
                  yield "*"; // I guess?
                } else {
                  yield "+" + typeSignature(extendsBound);
                }
              } else {
                yield "-" + typeSignature(superBound);
              }
            }
            default -> typeSignature(ta);
            });
        }
        sb.append('>');
      }

      sb.append(';');
    } finally {
      CompletionLock.release();
    }
  }

  public static final String descriptor(final TypeMirror t) {
    final StringBuilder sb = new StringBuilder();
    descriptor(t, sb);
    return sb.toString();
  }

  private static final void descriptor(final TypeMirror t, final StringBuilder sb) {
    CompletionLock.acquire();
    try {
      switch (t.getKind()) {
      case ARRAY      -> descriptor(((ArrayType)t).getComponentType(), sb.append("["));
      case BOOLEAN    -> sb.append("Z"); // yes, really
      case BYTE       -> sb.append("B");
      case CHAR       -> sb.append("C");
      case DECLARED   -> sb.append("L").append(jvmBinaryName((TypeElement)((DeclaredType)t).asElement())).append(";"); // basically an erasure
      case DOUBLE     -> sb.append("D");
      case EXECUTABLE -> descriptor((ExecutableType)t, sb);
      case FLOAT      -> sb.append("F");
      case INT        -> sb.append("I");
      case LONG       -> sb.append("J"); // yes, really
      case SHORT      -> sb.append("S");
      case TYPEVAR    -> descriptor(erasure(t), sb);
      case VOID       -> sb.append("V");
      case ERROR, INTERSECTION, MODULE, NONE, NULL, OTHER, PACKAGE, UNION, WILDCARD -> throw new IllegalArgumentException("t: " + t);
      }
    } finally {
      CompletionLock.release();
    }
  }

  private static final void descriptor(final ExecutableType t, final StringBuilder sb) {
    CompletionLock.acquire();
    try {
      if (t.getKind() != TypeKind.EXECUTABLE) {
        throw new IllegalArgumentException("t: " + t);
      }
      sb.append('(');
      for (final TypeMirror pt : t.getParameterTypes()) {
        descriptor(pt, sb);
      }
      sb.append(')');
      descriptor(t.getReturnType(), sb);
    } finally {
      CompletionLock.release();
    }
  }

  public static final String jvmBinaryName(final TypeElement te) {
    CompletionLock.acquire();
    try {
      if (!te.getKind().isDeclaredType()) {
        throw new IllegalArgumentException("te: " + te);
      }
      return binaryName(te).toString().replace('.', '/');
    } finally {
      CompletionLock.release();
    }
  }


  /*
   * End of JVMS productions.
   */


  public static final List<? extends TypeMirror> directSupertypes(TypeMirror t) {
    t = unwrap(t);
    final Types types = pe().getTypeUtils();
    final List<? extends TypeMirror> rv;
    CompletionLock.acquire();
    try {
      rv = types.directSupertypes(t);
    } finally {
      CompletionLock.release();
    }
    return wrapTypes(rv);
  }

  public static final boolean subsignature(ExecutableType e, ExecutableType f) {
    e = unwrap(e);
    f = unwrap(f);
    final Types types = pe().getTypeUtils();
    CompletionLock.acquire();
    try {
      return types.isSubsignature(e, f);
    } finally {
      CompletionLock.release();
    }
  }

  public static final TypeMirror erasure(TypeMirror t) {
    t = unwrap(t);
    final Types types = pe().getTypeUtils();
    // JavacTypes#erasure(TypeMirror) calls TypeMirror#getKind().
    CompletionLock.acquire();
    try {
      t = types.erasure(t);
    } finally {
      CompletionLock.release();
    }
    assert t != null;
    return wrap(t);
  }

  /**
   * Returns a {@link TypeAndElementSource} backed by relevant {@code static} methods in this class.
   *
   * @return a {@link TypeAndElementSource}; never {@code null}
   *
   * @see TypeAndElementSource
   */
  public static final TypeAndElementSource typeAndElementSource() {
    return ConstableTypeAndElementSource.INSTANCE;
  }

  public static final long modifiers(final Element e) {
    // modifiers is declared long because there are javac-specific modifiers that *might* be used later
    long modifiers;
    CompletionLock.acquire();
    try {
      modifiers = modifiers(e.getModifiers());
      switch (e.getKind()) {
      case ANNOTATION_TYPE:
        modifiers |= (long)ACC_ANNOTATION;
        // (abstract will have already been set)
        assert (modifiers & (long)ACC_ABSTRACT) != 0;
        modifiers |= (long)ACC_INTERFACE;
        break;
      case CONSTRUCTOR:
        // if (canonicalConstructor(e)) {
        //   modifiers |= JAVAC_RECORD; // See RECORD case
        //   if (compactConstructor(e)) {
        //     modifiers |= JAVAC_COMPACT_RECORD_CONSTRUCTOR;
        //   }
        // }
        break;
      case ENUM:
      case ENUM_CONSTANT:
        // Weirdly, if you're an enum *type* or an enum *constant* the same flag is set, I guess?
        modifiers |= (long)ACC_ENUM;
        break;
      case INTERFACE:
        // (abstract will have already been set)
        assert (modifiers & (long)ACC_ABSTRACT) != 0;
        modifiers |= (long)ACC_INTERFACE;
        break;
      case METHOD:
        if (bridge(e)) {
          modifiers |= (long)ACC_BRIDGE;
        }
        if (((ExecutableElement)e).isVarArgs()) {
          modifiers |= (long)ACC_VARARGS;
        }
        break;
      case MODULE:
        // TODO: there are various ACC_OPEN, ACC_STATIC_PHASE etc. modifiers that I'm not entirely sure where to put them
        break;
      case RECORD:
        // ASM uses Opcodes.ACC_RECORD; Byte buddy (among possibly other libraries) uses this to test recordness (maybe not the best idea)
        modifiers |= ASM_RECORD;
        break;
      case RECORD_COMPONENT:
        // "Flag to indicate that a class is a record. The flag is also used to mark fields that are part of the state
        // vector of a record [record components] and to mark the canonical constructor"
        // modifiers |= JAVAC_RECORD;
        break;
      case TYPE_PARAMETER:
        // "Flag that marks formal parameters."
        // modifiers |= JAVAC_PARAMETER;
        break;
      default:
        break;
      }
      switch (origin(e)) {
      case SYNTHETIC:
        modifiers |= (long)ACC_SYNTHETIC;
        break;
      case MANDATED:
        modifiers |= (long)ACC_MANDATED;
        break;
      case EXPLICIT:
      default:
        break;
      }

    } finally {
      CompletionLock.release();
    }
    return modifiers;
  }

  public static final long modifiers(final Set<? extends Modifier> ms) {
    long modifiers = 0L;
    for (final Modifier m : ms) {
      modifiers |= modifierMasks.get(m).longValue();
    }
    return modifiers;
  }

  public static final Set<? extends Modifier> modifiers(final long modifiers) {
    final EnumSet<Modifier> s = EnumSet.noneOf(Modifier.class);
    for (final Entry<Modifier, Long> e : modifierMasks.entrySet()) {
      if ((modifiers & e.getValue().longValue()) != 0) {
        s.add(e.getKey());
      }
    }
    return Collections.unmodifiableSet(s);
  }

  public static final ModuleElement moduleElement(final Class<?> c) {
    return moduleElement(c.getModule());
  }

  public static final ModuleElement moduleElement(final Module module) {
    if (module.isNamed()) {
      final ModuleElement rv = moduleElement(module.getName());
      assert rv != null : "null moduleElement for " + module.getName();
      return rv;
    }
    return moduleElement("");
  }

  public static final ModuleElement moduleElement(final CharSequence moduleName) {
    Objects.requireNonNull(moduleName, "moduleName");
    final Elements elements = pe().getElementUtils();
    final ModuleElement rv;
    // Not absolutely clear this causes completion but...
    CompletionLock.acquire();
    try {
      rv = elements.getModuleElement(moduleName);
      if (rv == null) {
        // Every so often this will return null. It shouldn't.
        if (LOGGER.isLoggable(DEBUG)) {
          LOGGER.log(DEBUG, "null ModuleElement for module name " + moduleName);
          try {
            LOGGER.log(DEBUG, "default module: " + getDefaultModuleMethod.invoke(modulesField.get(elements)));
          } catch (final ReflectiveOperationException x) {
            LOGGER.log(DEBUG, x.getMessage(), x);
          }
        }
      }
    } finally {
      CompletionLock.release();
    }
    return rv == null ? null : wrap(rv);
  }

  public static final ModuleElement moduleOf(Element e) {
    e = unwrap(e);
    final Elements elements = pe().getElementUtils();
    final ModuleElement rv;
    // This doesn't seem to cause completion, but better safe than sorry.
    CompletionLock.acquire();
    try {
      rv = elements.getModuleOf(e);
      if (rv == null) {
        if (LOGGER.isLoggable(DEBUG)) {
          LOGGER.log(DEBUG, "null ModuleElement for Element " + e);
          try {
            LOGGER.log(DEBUG, "default module: " + getDefaultModuleMethod.invoke(modulesField.get(e)));
          } catch (final ReflectiveOperationException x) {
            LOGGER.log(DEBUG, x.getMessage(), x);
          }
        }
      }
    } finally {
      CompletionLock.release();
    }
    return wrap(rv);
  }

  public static final Name name(final CharSequence name) {
    Objects.requireNonNull(name, "name");
    final Elements elements = pe().getElementUtils();
    if (lockNames) {
      CompletionLock.acquire();
      try {
        return elements.getName(name);
      } finally {
        CompletionLock.release();
      }
    } else {
      return elements.getName(name);
    }
  }

  public static final Elements.Origin origin(Element e) {
    e = unwrap(e);
    final Elements elements = pe().getElementUtils();
    final Elements.Origin rv;
    CompletionLock.acquire();
    try {
      rv = elements.getOrigin(e);
    } finally {
      CompletionLock.release();
    }
    return rv;
  }

  public static final PackageElement packageElement(final Class<?> c) {
    return packageElement(c.getModule(), c.getPackage());
  }

  public static final PackageElement packageElement(final Package pkg) {
    return packageElement(pkg.getName());
  }

  public static final PackageElement packageElement(final CharSequence fullyQualifiedName) {
    Objects.requireNonNull(fullyQualifiedName, "fullyQualifiedName");
    final Elements elements = pe().getElementUtils();
    // JavacElements#getPackageElement() may end up calling JavacElements#nameToSymbol(ModuleSymbol, String, Class),
    // which calls complete() in certain code paths.
    final PackageElement rv;
    CompletionLock.acquire();
    try {
      rv = elements.getPackageElement(fullyQualifiedName);
    } finally {
      CompletionLock.release();
    }
    return rv == null ? null : wrap(rv);
  }

  public static final PackageElement packageElement(final Module module, final Package pkg) {
    return packageElement(moduleElement(module), pkg);
  }

  public static final PackageElement packageElement(final ModuleElement moduleElement, final Package pkg) {
    return packageElement(moduleElement, pkg.getName());
  }

  public static final PackageElement packageElement(final ModuleElement moduleElement, final CharSequence fullyQualifiedName) {
    final Elements elements = pe().getElementUtils();
    final PackageElement rv;
    CompletionLock.acquire();
    try {
      rv = elements.getPackageElement(moduleElement, fullyQualifiedName);
    } finally {
      CompletionLock.release();
    }
    return rv == null ? null : wrap(rv);
  }

  public static final PackageElement packageOf(final Element e) {
    // This does NOT appear to cause completion.
    final PackageElement rv = pe().getElementUtils().getPackageOf(unwrap(e));
    return rv == null ? null : wrap(rv);
  }

  public static final ArrayType arrayType(final Class<?> arrayClass) {
    if (!arrayClass.isArray()) {
      throw new IllegalArgumentException("arrayClass: " + arrayClass);
    }
    return arrayTypeOf(type(arrayClass.getComponentType()));
  }

  public static final ArrayType arrayType(final GenericArrayType g) {
    return arrayTypeOf(type(g.getGenericComponentType()));
  }

  // Called by describeConstable().
  public static final ArrayType arrayTypeOf(TypeMirror componentType) {
    componentType = unwrap(componentType);
    final Types types = pe().getTypeUtils();
    final ArrayType rv;
    // JavacTypes#getArrayType(TypeMirror) calls getKind() on the component type.
    CompletionLock.acquire();
    try {
      rv = types.getArrayType(componentType);
    } finally {
      CompletionLock.release();
    }
    return wrap(rv);
  }

  public static final DeclaredType declaredType(final Class<?> c) {
    if (c.isArray() || c.isPrimitive() || c.isLocalClass() || c.isAnonymousClass()) {
      throw new IllegalArgumentException("c: " + c);
    }
    final Class<?> ec = c.getEnclosingClass();
    return declaredType(ec == null ? null : declaredType(ec), typeElement(c));
  }

  public static final DeclaredType declaredType(final ParameterizedType p) {
    return
      declaredType(switch (p.getOwnerType()) {
                   case null                 -> null;
                   case Class<?> c           -> declaredType(c);
                   case ParameterizedType pt -> declaredType(pt);
                   default                   -> throw new IllegalArgumentException("p: " + p);
                   },
                   typeElement((Class<?>)p.getRawType()),
                   typeArray(p.getActualTypeArguments()));
  }

  public static final DeclaredType declaredType(final CharSequence canonicalName) {
    return declaredType(typeElement(canonicalName));
  }

  public static final DeclaredType declaredType(TypeElement typeElement,
                                                TypeMirror... typeArguments) {
    typeElement = unwrap(typeElement);
    typeArguments = unwrap(typeArguments);
    final Types types = pe().getTypeUtils();
    final DeclaredType rv;
    CompletionLock.acquire();
    try {
      rv = types.getDeclaredType(typeElement, typeArguments);
    } finally {
      CompletionLock.release();
    }
    return wrap(rv);
  }

  public static final DeclaredType declaredType(DeclaredType containingType,
                                                TypeElement typeElement,
                                                TypeMirror... typeArguments) {
    if (containingType != null) {
      containingType = unwrap(containingType);
    }
    typeElement = unwrap(typeElement);
    typeArguments = unwrap(typeArguments);
    final Types types = pe().getTypeUtils();
    final DeclaredType rv;
    CompletionLock.acquire();
    try {
      // java.lang.NullPointerException: Cannot invoke "javax.lang.model.type.TypeMirror.toString()" because "t" is null
      // at jdk.compiler/com.sun.tools.javac.model.JavacTypes.getDeclaredType0(JavacTypes.java:272)
      // at jdk.compiler/com.sun.tools.javac.model.JavacTypes.getDeclaredType(JavacTypes.java:241)
      // at jdk.compiler/com.sun.tools.javac.model.JavacTypes.getDeclaredType(JavacTypes.java:249)
      // at org.microbean.lang@0.0.1-SNAPSHOT/org.microbean.lang.Lang.declaredType(Lang.java:1381)
      rv = types.getDeclaredType(containingType, typeElement, typeArguments);
    } finally {
      CompletionLock.release();
    }
    return wrap(rv);
  }

  public static final List<? extends TypeMirror> typeArguments(final TypeMirror t) {
    if (Objects.requireNonNull(t, "t") instanceof DeclaredType dt) {
      CompletionLock.acquire();
      try {
        switch (dt.getKind()) {
        case DECLARED:
          return dt.getTypeArguments();
        default:
          break;
        }
      } finally {
        CompletionLock.release();
      }
    }
    return List.of();
  }

  public static final ExecutableElement executableElement(final Executable e) {
    return switch (e) {
    case null             -> throw new NullPointerException("e");
    case Constructor<?> c -> executableElement(c);
    case Method m         -> executableElement(m);
    default               -> throw new IllegalArgumentException("e: " + e);
    };
  }

  public static final ExecutableElement executableElement(final Constructor<?> c) {
    return
      executableElement(typeElement(c.getDeclaringClass()), typeArray(c.getParameterTypes())); // deliberate erasure
  }

  public static final ExecutableElement executableElement(final Method m) {
    return
      executableElement(typeElement(m.getDeclaringClass()), m.getName(), typeArray(m.getParameterTypes())); // deliberate erasure
  }

  // (Constructor.)
  public static final ExecutableElement executableElement(TypeElement declaringClass,
                                                          final List<? extends TypeMirror> parameterTypes) {
    declaringClass = unwrap(declaringClass); // needed because types.erasure() is used on its children
    final Types types = pe().getTypeUtils();
    ExecutableElement rv = null;
    final int parameterTypesSize = parameterTypes == null ? 0 : parameterTypes.size();
    CompletionLock.acquire();
    try {
      CONSTRUCTOR_LOOP:
      for (final ExecutableElement c : (Iterable<? extends ExecutableElement>)constructorsIn(declaringClass.getEnclosedElements())) {
        final List<? extends VariableElement> parameterElements = c.getParameters();
        if (parameterTypesSize == parameterElements.size()) {
          for (int i = 0; i < parameterTypesSize; i++) {
            if (!types.isSameType(types.erasure(unwrap(parameterTypes.get(i))),
                                  types.erasure(parameterElements.get(i).asType()))) {
              continue CONSTRUCTOR_LOOP;
            }
          }
          rv = c;
          break;
        }
      }
    } finally {
      CompletionLock.release();
    }
    return rv == null ? null : wrap(rv);
  }

  // (Constructor.)
  public static final ExecutableElement executableElement(TypeElement declaringClass,
                                                          final TypeMirror... parameterTypes) {
    declaringClass = unwrap(declaringClass);
    final Types types = pe().getTypeUtils();
    ExecutableElement rv = null;
    final int parameterTypesSize = parameterTypes == null ? 0 : parameterTypes.length;
    CompletionLock.acquire();
    try {
      CONSTRUCTOR_LOOP:
      for (final ExecutableElement c : (Iterable<? extends ExecutableElement>)constructorsIn(declaringClass.getEnclosedElements())) {
        final List<? extends VariableElement> parameterElements = c.getParameters();
        if (parameterTypesSize == parameterElements.size()) {
          for (int i = 0; i < parameterTypesSize; i++) {
            if (!types.isSameType(types.erasure(unwrap(parameterTypes[i])),
                                  types.erasure(parameterElements.get(i).asType()))) {
              continue CONSTRUCTOR_LOOP;
            }
          }
          rv = c;
          break;
        }
      }
    } finally {
      CompletionLock.release();
    }
    return rv == null ? null : wrap(rv);
  }

  // (Method.)
  public static final ExecutableElement executableElement(TypeElement declaringClass,
                                                          final CharSequence name,
                                                          final List<? extends TypeMirror> parameterTypes) {
    if ("<init>".equals(name)) {
      return executableElement(declaringClass, parameterTypes);
    }
    declaringClass = unwrap(declaringClass);
    final Types types = pe().getTypeUtils();
    ExecutableElement rv = null;
    final int parameterTypesSize = parameterTypes == null ? 0 : parameterTypes.size();
    CompletionLock.acquire();
    try {
      METHOD_LOOP:
      for (final ExecutableElement m : (Iterable<? extends ExecutableElement>)methodsIn(declaringClass.getEnclosedElements())) {
        if (m.getSimpleName().contentEquals(name)) {
          final List<? extends VariableElement> parameterElements = m.getParameters();
          if (parameterTypesSize == parameterElements.size()) {
            for (int i = 0; i < parameterTypesSize; i++) {
              if (!types.isSameType(types.erasure(unwrap(parameterTypes.get(i))),
                                    types.erasure(parameterElements.get(i).asType()))) {
                continue METHOD_LOOP;
              }
            }
            rv = m;
            break;
          }
        }
      }
    } finally {
      CompletionLock.release();
    }
    return rv == null ? null : wrap(rv);
  }

  // (Method.)
  public static final ExecutableElement executableElement(TypeElement declaringClass,
                                                          final CharSequence name,
                                                          final TypeMirror... parameterTypes) {
    if ("<init>".equals(name)) {
      return executableElement(declaringClass, parameterTypes);
    }
    declaringClass = unwrap(declaringClass);
    final Types types = pe().getTypeUtils();
    ExecutableElement rv = null;
    final int parameterTypesSize = parameterTypes == null ? 0 : parameterTypes.length;
    CompletionLock.acquire();
    try {
      METHOD_LOOP:
      for (final ExecutableElement m : (Iterable<? extends ExecutableElement>)methodsIn(declaringClass.getEnclosedElements())) {
        if (m.getSimpleName().contentEquals(name)) {
          final List<? extends VariableElement> parameterElements = m.getParameters();
          if (parameterTypesSize == parameterElements.size()) {
            for (int i = 0; i < parameterTypesSize; i++) {
              if (!types.isSameType(types.erasure(unwrap(parameterTypes[i])),
                                    types.erasure(parameterElements.get(i).asType()))) {
                continue METHOD_LOOP;
              }
            }
            rv = m;
            break;
          }
        }
      }
    } finally {
      CompletionLock.release();
    }
    return rv == null ? null : wrap(rv);
  }

  public static final ExecutableType executableType(final Executable e) {
    return (ExecutableType)executableElement(e).asType();
  }

  public static final NoType noType(final TypeKind k) {
    return pe().getTypeUtils().getNoType(k);
  }

  // Called by describeConstable
  public static final NullType nullType() {
    return pe().getTypeUtils().getNullType();
  }

  public static final PrimitiveType primitiveType(final Class<?> c) {
    if (!c.isPrimitive()) {
      throw new IllegalArgumentException("c: " + c);
    }
    return primitiveType(TypeKind.valueOf(c.getName().toUpperCase()));
  }

  // Called by describeConstable().
  public static final PrimitiveType primitiveType(final TypeKind k) {
    return pe().getTypeUtils().getPrimitiveType(k);
  }

  public static final boolean sameType(TypeMirror t, TypeMirror s) {
    if (t == s) {
      // Optimization
      return true;
    }
    t = unwrap(t);
    s = unwrap(s);
    final Types types = pe().getTypeUtils();
    CompletionLock.acquire();
    try {
      // Internally JavacTypes calls getKind() on each type, causing symbol completion
      return types.isSameType(t, s);
    } finally {
      CompletionLock.release();
    }
  }

  public static final TypeElement typeElement(final Class<?> c) {
    if (c.isArray() || c.isPrimitive() || c.isLocalClass() || c.isAnonymousClass()) {
      throw new IllegalArgumentException("c: " + c);
    }
    return typeElement(c.getCanonicalName());
    // final ModuleElement me = moduleElement(c.getModule());
    // return me == null ? typeElement(c.getCanonicalName()) : typeElement(me, c.getCanonicalName());
  }

  public static final TypeElement typeElement(final CharSequence canonicalName) {
    Objects.requireNonNull(canonicalName, "canonicalName");
    final Elements elements = pe().getElementUtils();
    final TypeElement rv;
    CompletionLock.acquire();
    try {
      rv = elements.getTypeElement(canonicalName);
      if (rv == null) {
        if (LOGGER.isLoggable(DEBUG)) {
          LOGGER.log(DEBUG, "null TypeElement for canonicalName " + canonicalName);
        }
        return null;
      } else if (!rv.getKind().isDeclaredType() && LOGGER.isLoggable(WARNING)) {
        LOGGER.log(WARNING, "rv.getKind(): " + rv.getKind() + "; rv: " + rv);
      }
    } finally {
      CompletionLock.release();
    }
    return wrap(rv);
  }

  public static final TypeElement typeElement(ModuleElement moduleElement, final CharSequence canonicalName) {
    if (moduleElement == null) {
      // TODO: in certain cases while running parallel unit tests even with all the synchronization in this class, a
      // ModuleElement can be null, and can get described as a DynamicConstant that is
      // java.lang.constant.ConstantDescs.NULL, which will then be hydrated back to null, and passed to this
      // method. This should, of course, never happen. Finding out what is responsible is a long-running pain in my ass.
      //
      // One possible culprit may be the Elements#getModuleOf(Element) method, which does this (see
      // com.sun.tools.javac.model.JavacElements):
      //
      //  Symbol sym = cast(Symbol.class, e);
      //  if (modules.getDefaultModule() == syms.noModule)
      //    return null; // <--- NOTE
      //  return (sym.kind == MDL) ? ((ModuleElement) e)
      //          : (sym.owner.kind == MDL) ? (ModuleElement) sym.owner
      //          : sym.packge().modle;
      //
      // modules is an instance of com.sun.tools.javac.comp.Modules. It only sets defaultModule to syms.noModule if
      // modules are not allowed by the current source level, which would seem to be impossible in this case.
      final Elements elements = pe().getElementUtils();
      String message = "moduleElement";
      CompletionLock.acquire();
      try {
        message += "; canonicalName: " + canonicalName + "; defaultModule: " + getDefaultModuleMethod.invoke(modulesField.get(elements));
      } catch (final ReflectiveOperationException x) {
        x.printStackTrace();
      } finally {
        CompletionLock.release();
      }
      // See also:
      // https://github.com/openjdk/jdk/blob/jdk-24%2B6/src/jdk.compiler/share/classes/com/sun/tools/javac/model/JavacElements.java#L171
      throw new NullPointerException(message);
    }
    Objects.requireNonNull(canonicalName, "canonicalName");
    moduleElement = unwrap(moduleElement);
    final Elements elements = pe().getElementUtils();
    final TypeElement rv;
    CompletionLock.acquire();
    try {
      rv = elements.getTypeElement(moduleElement, canonicalName);
      if (rv == null) {
        if (LOGGER.isLoggable(DEBUG)) {
          LOGGER.log(DEBUG, "null TypeElement for ModuleElement " + moduleElement + " and canonicalName " + canonicalName);
        }
        return null;
      }
    } finally {
      CompletionLock.release();
    }
    return wrap(rv);
  }

  public static final Parameterizable parameterizable(final GenericDeclaration gd) {
    return switch (gd) {
    case null         -> throw new NullPointerException("gd");
    case Executable e -> executableElement(e);
    case Class<?> c   -> typeElement(c);
    default           -> throw new IllegalArgumentException("gd: " + gd);
    };
  }

  public static final TypeParameterElement typeParameterElement(final java.lang.reflect.TypeVariable<?> t) {
    return typeParameterElement(t.getGenericDeclaration(), t.getName());
  }

  public static final TypeParameterElement typeParameterElement(GenericDeclaration gd, final String name) {
    Objects.requireNonNull(gd, "gd");
    Objects.requireNonNull(name, "name");
    while (gd != null) {
      final java.lang.reflect.TypeVariable<?>[] typeParameters = gd.getTypeParameters();
      for (int i = 0; i < typeParameters.length; i++) {
        if (typeParameters[i].getName().equals(name)) {
          return parameterizable(gd).getTypeParameters().get(i);
        }
      }
      gd = gd instanceof Executable e ? e.getDeclaringClass() : ((Class<?>)gd).getEnclosingClass();
    }
    return null;
  }

  public static final TypeVariable typeVariable(final java.lang.reflect.TypeVariable<?> t) {
    final TypeParameterElement e = typeParameterElement(t);
    return e == null ? null : (TypeVariable)e.asType();
  }

  public static final TypeVariable typeVariable(final GenericDeclaration gd, final String name) {
    final TypeParameterElement e = typeParameterElement(gd, name);
    return e == null ? null : (TypeVariable)e.asType();
  }

  public static final VariableElement variableElement(final Field f) {
    return variableElement(typeElement(f.getDeclaringClass()), f.getName());
  }

  // (Field.)
  public static final VariableElement variableElement(final TypeElement declaringClass, final CharSequence fieldName) {
    Objects.requireNonNull(fieldName, "fieldName");
    CompletionLock.acquire();
    try {
      for (final Element e : declaringClass.getEnclosedElements()) {
        if (e.getKind() == ElementKind.FIELD && e.getSimpleName().contentEquals(fieldName)) {
          return wrap((VariableElement)e);
        }
      }
    } finally {
      CompletionLock.release();
    }
    return null;
  }

  // (Parameter.)
  public static final VariableElement variableElement(final ExecutableElement declaringExecutable, final CharSequence parameterName) {
    Objects.requireNonNull(parameterName, "parameterName");
    CompletionLock.acquire();
    try {
      for (final Element e : declaringExecutable.getEnclosedElements()) {
        if (e.getKind() == ElementKind.PARAMETER && e.getSimpleName().contentEquals(parameterName)) {
          // In the javax.lang.model.* classes, a VariableElement serving as a parameter always has a name, and that
          // name is unique within the executable.
          return wrap((VariableElement)e);
        }
      }
    } finally {
      CompletionLock.release();
    }
    return null;
  }

  // (Parameter.)
  public static final VariableElement variableElement(final ExecutableElement declaringExecutable, final int position) {
    CompletionLock.acquire();
    try {
      switch (declaringExecutable.getKind()) {
      case CONSTRUCTOR:
      case METHOD:
        return wrap((VariableElement)declaringExecutable.getEnclosedElements().get(position));
      default:
        break;
      }
    } finally {
      CompletionLock.release();
    }
    return null;
  }

  public static final WildcardType wildcardType() {
    return wildcardType(null, null);
  }

  public static final WildcardType wildcardType(final java.lang.reflect.WildcardType t) {
    final Type[] lowerBounds = t.getLowerBounds();
    return wildcardType(type(t.getUpperBounds()[0]), type(lowerBounds.length <= 0 ? null : lowerBounds[0]));
  }

  public static final WildcardType wildcardType(TypeMirror extendsBound, TypeMirror superBound) {
    extendsBound = extendsBound == null ? null : unwrap(extendsBound);
    superBound = superBound == null ? null : unwrap(superBound);
    final Types types = pe().getTypeUtils();
    final WildcardType rv;
    CompletionLock.acquire();
    try {
      // JavacTypes#getWildcardType() can call getKind() on bounds etc. which triggers symbol completion
      rv = types.getWildcardType(extendsBound, superBound);
    } finally {
      CompletionLock.release();
    }
    return wrap(rv);
  }

  /**
   * Returns {@code true} if and only if a bearer of the supplied {@code payload} is assignable to a bearer of the
   * supplied {@code receiver}.
   *
   * @param payload a type borne by the "right hand side" of a potential assignment; must not be {@code null}
   *
   * @param receiver a type borne by the "left hand side" of a potential assignment; must not be {@code null}
   *
   * @return {@code true} if and only if a bearer of the supplied {@code payload} is assignable to a bearer of the
   * supplied {@code receiver}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @see Types#isAssignable(TypeMirror, TypeMirror)
   */
  public static final boolean assignable(TypeMirror payload, TypeMirror receiver) {
    payload = unwrap(payload);
    receiver = unwrap(receiver);
    final Types types = pe().getTypeUtils();
    CompletionLock.acquire();
    try {
      return types.isAssignable(payload, receiver);
    } finally {
      CompletionLock.release();
    }
  }

  public static final boolean subtype(TypeMirror payload, TypeMirror receiver) {
    payload = unwrap(payload);
    receiver = unwrap(receiver);
    final Types types = pe().getTypeUtils();
    CompletionLock.acquire();
    try {
      return types.isSubtype(payload, receiver);
    } finally {
      CompletionLock.release();
    }
  }

  public static final TypeMirror type(final Type t) {
    return switch (t) {
    case null                                 -> throw new NullPointerException();
    case Class<?> c when c == void.class      -> noType(TypeKind.VOID);
    case Class<?> c when c.isArray()          -> arrayType(c);
    case Class<?> c when c.isPrimitive()      -> primitiveType(c);
    case Class<?> c                           -> declaredType(c);
    case ParameterizedType p                  -> declaredType(p);
    case GenericArrayType g                   -> arrayType(g);
    case java.lang.reflect.TypeVariable<?> tv -> typeVariable(tv);
    case java.lang.reflect.WildcardType w     -> wildcardType(w);
    default                                   -> throw new IllegalArgumentException("t: " + t);
    };
  }

  public static final TypeMirror type(final Field f) {
    return variableElement(f).asType();
  }

  static final ProcessingEnvironment pe() {
    ProcessingEnvironment pe = Lang.pe; // volatile read
    if (pe == null) {
      initialize();
      try {
        initLatch.await();
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      pe = Lang.pe; // volatile read
      if (pe == null || initLatch.getCount() > 0L) {
        state = INITIALIZATION_ERROR; // volatile write
        throw new IllegalStateException();
      }
      assert state == INITIALIZED; // volatile read
    }
    return pe;
  }

  private static final TypeMirror[] typeArray(final Type[] ts) {
    if (ts.length <= 0) {
      return EMPTY_TYPEMIRROR_ARRAY;
    }
    final TypeMirror[] rv = new TypeMirror[ts.length];
    for (int i = 0; i < ts.length; i++) {
      rv[i] = type(ts[i]);
    }
    return rv;
  }

  /**
   * Asynchronously and idempotently initializes the {@link Lang} class for use.
   *
   * <p>This method is automatically called by the internals of this class when appropriate, but is {@code public} to
   * support eager initialization use cases.</p>
   */
  // Idempotent.
  public static final void initialize() {
    if (!STATE.compareAndSet(UNINITIALIZED, INITIALIZING)) {
      return;
    }
    if (LOGGER.isLoggable(DEBUG)) {
      LOGGER.log(DEBUG, "Initializing");
    }
    // Virtual thread, not platform thread, because it will spend the vast majority of its life blocked on a
    // CountDownLatch
    Thread.ofVirtual()
      .name(Lang.class.getName())
      .uncaughtExceptionHandler((t, e) -> {
          if (LOGGER.isLoggable(ERROR)) {
            LOGGER.log(ERROR, e.getMessage(), e);
          }
        })
      .start(new BlockingCompilationTask(initLatch));
  }

  @SuppressWarnings("unchecked")
  public static final <T extends TypeMirror> T unwrap(final T t) {
    return (T)DelegatingTypeMirror.unwrap(Objects.requireNonNull(t, "t"));
  }

  public static final TypeMirror[] unwrap(final TypeMirror[] ts) {
    return DelegatingTypeMirror.unwrap(Objects.requireNonNull(ts, "ts"));
  }

  public static final <E extends Element> E unwrap(final E e) {
    return DelegatingElement.unwrap(Objects.requireNonNull(e, "e"));
  }

  @SuppressWarnings("unchecked")
  public static final <T extends TypeMirror> T wrap(final T t) {
    return (T)DelegatingTypeMirror.of(Objects.requireNonNull(t, "t"), typeAndElementSource(), null);
  }

  @SuppressWarnings("unchecked")
  public static final <E extends Element> E wrap(final E e) {
    return (E)DelegatingElement.of(Objects.requireNonNull(e, "e"), typeAndElementSource(), null);
  }

  public static final List<? extends TypeMirror> wrapTypes(final Collection<? extends TypeMirror> ts) {
    return DelegatingTypeMirror.of(Objects.requireNonNull(ts, "ts"), typeAndElementSource(), null);
  }

  public static final List<? extends Element> wrapElements(final Collection<? extends Element> es) {
    return DelegatingElement.of(Objects.requireNonNull(es, "es"), typeAndElementSource(), null);
  }


  /*
   * Inner and nested classes.
   */


  /**
   * A {@link TypeAndElementSource} implementation that is also {@link Constable}.
   *
   * @author <a href="https://about.me/lairdnelson/" target="_top">Laird Nelson</a>
   */
  public static final class ConstableTypeAndElementSource implements Constable, TypeAndElementSource {


    /*
     * Static fields.
     */


    /**
     * The sole instance of this class.
     */
    public static final ConstableTypeAndElementSource INSTANCE = new ConstableTypeAndElementSource();

    private static final ClassDesc CD_ConstableTypeAndElementSource = ClassDesc.of(ConstableTypeAndElementSource.class.getName());


    /*
     * Constructors.
     */


    private ConstableTypeAndElementSource() {
      super();
    }


    /*
     * Instance methods.
     */


    @Override
    public final ArrayType arrayTypeOf(final TypeMirror componentType) {
      return Lang.arrayTypeOf(componentType);
    }

    // Note the counterintuitive parameter order.
    @Override
    public final boolean assignable(final TypeMirror payload, final TypeMirror receiver) {
      return Lang.assignable(payload, receiver);
    }

    @Override
    public final TypeElement boxedClass(final PrimitiveType t) {
      return Lang.boxedClass(t);
    }

    @Override
    public final DeclaredType declaredType(final TypeElement typeElement, final TypeMirror... typeArguments) {
      return Lang.declaredType(typeElement, typeArguments);
    }

    @Override
    public final DeclaredType declaredType(final DeclaredType containingType,
                                           final TypeElement typeElement,
                                           final TypeMirror... typeArguments) {
      return Lang.declaredType(containingType, typeElement, typeArguments);
    }

    @Override
    public final Optional<? extends ConstantDesc> describeConstable(final AnnotatedConstruct a) {
      return Lang.describeConstable(a);
    }

    @Override
    public final List<? extends TypeMirror> directSupertypes(final TypeMirror t) {
      return Lang.directSupertypes(t);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <T extends TypeMirror> T erasure(final T t) {
      return (T)Lang.erasure(t);
    }

    @Override
    public final ModuleElement moduleElement(final CharSequence canonicalName) {
      return Lang.moduleElement(canonicalName);
    }

    @Override
    public final NoType noType(final TypeKind k) {
      return Lang.noType(k);
    }

    @Override
    public final NullType nullType() {
      return Lang.nullType();
    }

    @Override
    public final PrimitiveType primitiveType(final TypeKind k) {
      return Lang.primitiveType(k);
    }

    @Override
    public final boolean sameType(final TypeMirror t, final TypeMirror s) {
      return t == s || Lang.sameType(t, s);
    }

    @Override
    public final boolean subtype(final TypeMirror t, final TypeMirror s) {
      return t == s || Lang.subtype(t, s);
    }

    @Override
    public final TypeElement typeElement(final CharSequence canonicalName) {
      return Lang.typeElement(canonicalName);
    }

    @Override
    public final TypeElement typeElement(final ModuleElement m, final CharSequence canonicalName) {
      return Lang.typeElement(m, canonicalName);
    }

    @Override
    public final TypeVariable typeVariable(final java.lang.reflect.TypeVariable<?> t) {
      return Lang.typeVariable(t);
    }

    @Override
    public final WildcardType wildcardType(final TypeMirror extendsBound, final TypeMirror superBound) {
      return Lang.wildcardType(extendsBound, superBound);
    }

    @Override
    public final Optional<DynamicConstantDesc<ConstableTypeAndElementSource>> describeConstable() {
      return
        Optional.of(DynamicConstantDesc.of(BSM_INVOKE,
                                           MethodHandleDesc.ofField(STATIC_GETTER,
                                                                    CD_ConstableTypeAndElementSource,
                                                                    "INSTANCE",
                                                                    CD_ConstableTypeAndElementSource)));
    }

  }

  private static final class BlockingCompilationTask implements Runnable {


    /*
     * Static fields.
     */


    private static final Logger LOGGER = System.getLogger(BlockingCompilationTask.class.getName());


    /*
     * Instance fields.
     */


    private final CountDownLatch initLatch;

    // (Never counted down except in error cases.)
    private final CountDownLatch runningLatch;


    /*
     * Constructors.
     */


    private BlockingCompilationTask(final CountDownLatch initLatch) {
      super();
      this.initLatch = Objects.requireNonNull(initLatch, "initLatch");
      this.runningLatch = new CountDownLatch(1);
    }


    /*
     * Instance methods.
     */


    @Override
    public final void run() {
      assert state == INITIALIZING; // volatile read
      if (LOGGER.isLoggable(DEBUG)) {
        LOGGER.log(DEBUG, "CompilationTask invocation daemon thread running");
      }
      try {
        final JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        if (jc == null) {
          state = INITIALIZATION_ERROR; // volatile write
          if (LOGGER.isLoggable(ERROR)) {
            LOGGER.log(ERROR, "No system Java compiler available");
          }
          runningLatch.countDown();
          initLatch.countDown();
          return;
        }

        final List<String> options = new ArrayList<>();
        options.add("-proc:only"); // critical
        options.add("-cp");
        options.add(System.getProperty("java.class.path"));

        // See
        // https://github.com/openjdk/jdk/blob/jdk-21%2B35/src/jdk.compiler/share/classes/com/sun/tools/javac/util/Names.java#L430-L436
        // in JDK 21; JDK 22+ changes things dramatically.
        //
        // This turns out to be rather important. The default name table in javac (21 and earlier) is shared and
        // unsynchronized such that, given a Name, calling its toString() method may involve reading a shared byte array
        // that is being updated in another thread. By contrast, the unshared name table creates Names whose contents
        // are not shared, so toString() invocations on them are not problematic.
        //
        // In JDK 22+, there is a String-based name table that is used by default; see
        // https://github.com/openjdk/jdk/pull/15470. However note that this is also not thread-safe:
        // https://github.com/openjdk/jdk/blob/jdk-22%2B36/src/jdk.compiler/share/classes/com/sun/tools/javac/util/StringNameTable.java#L65
        // The non-thread-safety may be a non-issue, however, as the map's computations are used only for canonical
        // mappings, so unless the internals of HashMap are broken repeated computation is just inconvenient, not a deal-breaker.
        //
        // TODO: It *seems* that the thread safety issues we sometimes see are due to the shared name table's Name
        // implementation's toString() method, which can read a portion of the shared byte[] array in which all name
        // content is stored at the same time that the same byte array is being updated. It does not appear to me that
        // any of the other Name.Table implementations suffer from this, so the string table may be good enough. That
        // is, except for the shared name table situation, any time you have a Name in your hand you should be able to
        // call toString() on it without any problems.
        if (Runtime.version().feature() >= 22) {
          if (Boolean.getBoolean("useUnsharedTable")) {
            if (LOGGER.isLoggable(DEBUG)) {
              LOGGER.log(DEBUG, "Using unshared name table");
            }
            options.add("-XDuseUnsharedTable");
          } else if (Boolean.getBoolean("useSharedTable")) {
            // Yikes
            if (LOGGER.isLoggable(WARNING)) {
              LOGGER.log(WARNING, "Using shared name table");
            }
            options.add("-XDuseSharedTable");
          } else {
            if (LOGGER.isLoggable(DEBUG)) {
              LOGGER.log(DEBUG, "Using string name table (default)");
            }
            if (Boolean.parseBoolean(System.getProperty("internStringTable", "true"))) {
              if (LOGGER.isLoggable(DEBUG)) {
                LOGGER.log(DEBUG, "Interning string name table strings");
              }
              options.add("-XDinternStringTable");
            }
          }
        } else if (Boolean.getBoolean("useSharedTable")) {
          // Strictly speaking not an option in JDK 21-, but we want to default to the unshared table because otherwise it's dangerous
          if (LOGGER.isLoggable(WARNING)) {
            LOGGER.log(WARNING, "Using shared name table");
          }
        } else {
          if (LOGGER.isLoggable(DEBUG)) {
            LOGGER.log(DEBUG, "Using unshared name table");
          }
          options.add("-XDuseUnsharedTable");
        }

        final Set<String> additionalRootModuleNames = new HashSet<>();
        final Collection<ModuleLocation> moduleLocations = new ArrayList<>();
        final ModuleFinder smf = ModuleFinder.ofSystem();
        final Module unnamedModule = this.getClass().getClassLoader().getUnnamedModule();
        try (final Stream<Module> s = this.getClass().getModule().getLayer().modules().stream().sequential()) {
          s
            // Figure out which runtime modules are named and not system modules. That set will be added, eventually, to
            // the task via its addModules(Set) method (see below).
            .filter(m -> m.isNamed() && smf.find(m.getName()).isEmpty())
            .forEach(m -> {
                additionalRootModuleNames.add(m.getName());
                if (m.canRead(unnamedModule)) {
                  // This is a (required) stupendous hack.
                  options.add("--add-reads");
                  options.add(m.getName() + "=ALL-UNNAMED");
                }
                moduleLocations.add(new ModuleLocation(m));
              });
        }

        if (Boolean.getBoolean(Lang.class.getName() + ".verbose")) {
          options.add("-verbose");
        }

        final Locale defaultLocale = Locale.getDefault();

        final DiagnosticLogger diagnosticLogger = new DiagnosticLogger(defaultLocale);
        final StandardJavaFileManager sjfm = jc.getStandardFileManager(diagnosticLogger, defaultLocale, Charset.defaultCharset());

        // (Any "loading" is actually performed by, e.g. com.sun.tools.javac.jvm.ClassReader.fillIn(), not reflective
        // machinery. Once a class has been so loaded, com.sun.tools.javac.code.Symtab#getClass(ModuleSymbol, Name) will
        // retrieve it from a HashMap.)
        final CompilationTask task =
          jc.getTask(new LogWriter(),
                     new ReadOnlyModularJavaFileManager(sjfm, moduleLocations),
                     diagnosticLogger,
                     options,
                     List.of("java.lang.annotation.RetentionPolicy"), // arbitrary, but loads the least amount of stuff up front
                     null); // compilation units; null means we aren't actually compiling anything
        task.setProcessors(List.of(new P()));
        task.setLocale(defaultLocale);
        task.addModules(additionalRootModuleNames);

        if (LOGGER.isLoggable(DEBUG)) {
          LOGGER.log(DEBUG, "CompilationTask options: " + options);
          LOGGER.log(DEBUG, "CompilationTask additional root module names: " + additionalRootModuleNames);
          LOGGER.log(DEBUG, "Calling CompilationTask");
        }

        if (Boolean.FALSE.equals(task.call())) { // NOTE: runs the task; task blocks forever by design; this thread therefore blocks forever here
          state = INITIALIZATION_ERROR; // volatile write
          if (LOGGER.isLoggable(ERROR)) {
            LOGGER.log(ERROR, "Calling CompilationTask failed");
          }
          runningLatch.countDown();
          initLatch.countDown();
        } else {
          state = INITIALIZED; // volatile write
        }

      } catch (final RuntimeException | Error e) {
        state = INITIALIZATION_ERROR; // volatile write
        runningLatch.countDown();
        initLatch.countDown();
        throw e;
      } finally {
        Lang.pe = null; // volatile write
      }
      if (LOGGER.isLoggable(DEBUG)) {
        LOGGER.log(DEBUG, "CompilationTask invocation daemon thread exiting");
      }
    }


    /*
     * Inner and nested classes.
     */


    private final class P extends AbstractProcessor {


      /*
       * Static fields.
       */


      private static final Logger LOGGER = System.getLogger(P.class.getName());


      /*
       * Constructors.
       */


      private P() {
        super();
        if (LOGGER.isLoggable(DEBUG)) {
          LOGGER.log(DEBUG, "Created");
        }
      }


      /*
       * Instance methods.
       */


      @Override // AbstractProcessor (Processor)
      public final void init(final ProcessingEnvironment pe) {
        if (LOGGER.isLoggable(DEBUG)) {
          LOGGER.log(DEBUG, "AbstractProcessor inititializing with " + pe);
        }
        Lang.pe = pe; // volatile write
        state = INITIALIZED; // volatile write
        initLatch.countDown(); // all done initializing
        if (LOGGER.isLoggable(DEBUG)) {
          LOGGER.log(DEBUG, "The " + Lang.class.getName() + " class is ready for use");
        }
        // Note to future maintainers: you're going to desperately want to move this to the process() method, and you
        // cannot. If you decide to doubt this message, at least comment this out so you don't lose it here. Don't say I
        // didn't warn you.
        try {
          runningLatch.await(); // NOTE: Blocks forever except in error cases
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }

      @Override // AbstractProcessor (Processor)
      public final Set<String> getSupportedAnnotationTypes() {
        return Set.of(); // we claim nothing, although it's moot because we're the only processor in existence
      }

      @Override // AbstractProcessor (Processor)
      public final Set<String> getSupportedOptions() {
        return Set.of();
      }

      @Override // AbstractProcessor (Processor)
      public final SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
      }

      @Override // AbstractProcessor (Processor)
      public final boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment) {
        assert this.isInitialized();
        return false; // we don't claim anything, but we're the only processor in existence
      }

    }

    private static final class DiagnosticLogger implements DiagnosticListener<JavaFileObject> {

      private final Locale locale;

      private DiagnosticLogger(final Locale locale) {
        super();
        this.locale = locale;
      }

      @Override // DiagnosticListener<JavaFileObject>
      public final void report(final Diagnostic<? extends JavaFileObject> d) {
        final Level level = switch (d.getKind()) {
        case ERROR                      -> Level.ERROR;
        case MANDATORY_WARNING, WARNING -> Level.WARNING;
        case NOTE, OTHER                -> Level.INFO;
        };
        if (LOGGER.isLoggable(level)) {
          LOGGER.log(level, d.getMessage(this.locale));
        }
      }

    }

    private static final class LogWriter extends StringWriter {

      private LogWriter() {
        super();
      }

      @Override
      public final void flush() {
        super.flush(); // does nothing
        if (LOGGER.isLoggable(INFO)) {
          LOGGER.log(INFO, this.toString());
        }
        this.getBuffer().setLength(0);
      }

    }

  }

  private static final class ModuleLocation implements Location {

    private final ModuleReference moduleReference;

    private ModuleLocation(final Module module) {
      this(module.getLayer().configuration().findModule(module.getName()).map(ResolvedModule::reference).orElse(null));
    }

    private ModuleLocation(final ModuleReference moduleReference) {
      super();
      this.moduleReference = Objects.requireNonNull(moduleReference, "moduleReference");
    }

    @Override // Location
    public final String getName() {
      return this.moduleReference.descriptor().name();
    }

    @Override // Location
    public final boolean isModuleOrientedLocation() {
      return false;
    }

    @Override // Location
    public final boolean isOutputLocation() {
      return false;
    }

    private final ModuleReference moduleReference() {
      return this.moduleReference;
    }

    @Override // Object
    public final int hashCode() {
      return this.moduleReference.descriptor().name().hashCode();
    }

    @Override // Object
    public final boolean equals(final Object other) {
      if (other == this) {
        return true;
      } else if (other != null && other.getClass() == this.getClass()) {
        return Objects.equals(this.getName(), ((ModuleLocation)other).getName());
      } else {
        return false;
      }
    }

    @Override // Object
    public final String toString() {
      return this.moduleReference.toString();
    }

  }

  private static final class ReadOnlyModularJavaFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {


    /*
     * Static fields.
     */


    private static final Logger LOGGER = System.getLogger(ReadOnlyModularJavaFileManager.class.getName());

    private static final Set<JavaFileObject.Kind> ALL_KINDS = EnumSet.allOf(JavaFileObject.Kind.class);


    /*
     * Instance fields.
     */


    private final Set<Location> locations;

    private final Map<ModuleReference, Map<String, List<JavaFileRecord>>> maps;


    /*
     * Constructors.
     */


    private ReadOnlyModularJavaFileManager(final StandardJavaFileManager fm, final Collection<? extends ModuleLocation> moduleLocations) {
      super(fm);
      this.maps = new ConcurrentHashMap<>();
      this.locations = moduleLocations == null ? Set.of() : Set.copyOf(moduleLocations);
      if (LOGGER.isLoggable(DEBUG)) {
        LOGGER.log(DEBUG, "Module locations: " + this.locations);
      }
    }


    /*
     * Instance methods.
     */


    @Override
    public final void close() throws IOException {
      super.close();
      this.maps.clear();
    }

    @Override
    public final ClassLoader getClassLoader(final Location packageOrientedLocation) {
      assert !packageOrientedLocation.isModuleOrientedLocation();
      if (packageOrientedLocation instanceof ModuleLocation m) {
        return this.getClass().getModule().getLayer().findLoader(m.getName());
      }
      return super.getClassLoader(packageOrientedLocation);
    }

    @Override
    public final JavaFileObject getJavaFileForInput(final Location packageOrientedLocation,
                                                    final String className,
                                                    final JavaFileObject.Kind kind) throws IOException {
      if (packageOrientedLocation instanceof ModuleLocation m) {
        try (final ModuleReader mr = m.moduleReference().open()) {
          return mr.find(className.replace('.', '/') + kind.extension)
            .map(u -> new JavaFileRecord(kind, className, u))
            .orElse(null);
        } catch (final IOException e) {
          throw new UncheckedIOException(e.getMessage(), e);
        }
      }
      return super.getJavaFileForInput(packageOrientedLocation, className, kind);
    }

    @Override
    public final boolean hasLocation(final Location location) {
      return switch (location) {
      case null -> false;
      case StandardLocation s -> switch (s) {
      case CLASS_PATH, MODULE_PATH, PATCH_MODULE_PATH, PLATFORM_CLASS_PATH, SYSTEM_MODULES, UPGRADE_MODULE_PATH -> {
        assert !s.isOutputLocation();
        yield super.hasLocation(s);
      }
      case ANNOTATION_PROCESSOR_MODULE_PATH, ANNOTATION_PROCESSOR_PATH, CLASS_OUTPUT, MODULE_SOURCE_PATH, NATIVE_HEADER_OUTPUT, SOURCE_OUTPUT, SOURCE_PATH -> false;
      };
      case ModuleLocation m -> true;
      default -> !location.isOutputLocation() && super.hasLocation(location);
      };
    }

    @Override
    public final Iterable<JavaFileObject> list(final Location packageOrientedLocation,
                                               final String packageName,
                                               final Set<JavaFileObject.Kind> kinds,
                                               final boolean recurse)
      throws IOException {
      if (packageOrientedLocation instanceof ModuleLocation m) {
        final ModuleReference mref = m.moduleReference();
        if (recurse) {
          // Don't cache anything; not really worth it
          try (final ModuleReader reader = mref.open()) {
            return list(reader, packageName, kinds, true);
          }
        }
        final Map<String, List<JavaFileRecord>> m0 = this.maps.computeIfAbsent(mref, mr -> {
            try (final ModuleReader reader = mr.open();
                 final Stream<String> ss = reader.list()) {
              return
                Collections.unmodifiableMap(ss.filter(s -> !s.endsWith("/"))
                                            .collect(HashMap::new,
                                                     (map, s) -> {
                                                       // s is, e.g., "foo/Bar.class"
                                                       final int lastSlashIndex = s.lastIndexOf('/');
                                                       assert lastSlashIndex != 0;
                                                       final String p0 = lastSlashIndex > 0 ? s.substring(0, lastSlashIndex).replace('/', '.') : "";
                                                       // p0 is now "foo"; list will be class files under package foo
                                                       final List<JavaFileRecord> list = map.computeIfAbsent(p0, p1 -> new ArrayList<>());
                                                       final JavaFileObject.Kind kind = kind(s);
                                                       try {
                                                         list.add(new JavaFileRecord(kind,
                                                                                     kind == JavaFileObject.Kind.CLASS || kind == JavaFileObject.Kind.SOURCE ?
                                                                                     s.substring(0, s.length() - kind.extension.length()).replace('/', '.') :
                                                                                     null,
                                                                                     reader.find(s).orElseThrow()));
                                                       } catch (final IOException ioException) {
                                                         throw new UncheckedIOException(ioException.getMessage(), ioException);
                                                       }
                                                     },
                                                     Map::putAll));
            } catch (final IOException ioException) {
              throw new UncheckedIOException(ioException.getMessage(), ioException);
            }
          });
        List<JavaFileRecord> unfilteredPackageContents = m0.get(packageName);
        if (unfilteredPackageContents == null) {
          return List.of();
        }
        assert !unfilteredPackageContents.isEmpty();
        if (kinds.size() < ALL_KINDS.size()) {
          unfilteredPackageContents = new ArrayList<>(unfilteredPackageContents);
          unfilteredPackageContents.removeIf(f -> !kinds.contains(f.kind()));
        }
        return Collections.unmodifiableList(unfilteredPackageContents);
      }
      return super.list(packageOrientedLocation, packageName, kinds, recurse);
    }

    // Returns package-oriented locations (or output locations if the given moduleOrientedOrOutputLocation is an output
    // location).
    @Override
    public final Iterable<Set<Location>> listLocationsForModules(final Location moduleOrientedOrOutputLocation) throws IOException {
      return
        moduleOrientedOrOutputLocation == StandardLocation.MODULE_PATH ?
        List.of(this.locations) :
        super.listLocationsForModules(moduleOrientedOrOutputLocation);
    }

    @Override
    public final String inferBinaryName(final Location packageOrientedLocation, final JavaFileObject file) {
      return file instanceof JavaFileRecord f ? f.binaryName() : super.inferBinaryName(packageOrientedLocation, file);
    }

    @Override
    public final String inferModuleName(final Location packageOrientedLocation) throws IOException {
      if (packageOrientedLocation instanceof ModuleLocation m) {
        assert m.getName() != null : "m.getName() == null: " + m;
        return m.getName();
      }
      return super.inferModuleName(packageOrientedLocation);
    }

    @Override
    public final boolean contains(final Location packageOrModuleOrientedLocation, final FileObject fo) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public final FileObject getFileForInput(final Location packageOrientedLocation,
                                            final String packageName,
                                            final String relativeName)
      throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public final FileObject getFileForOutput(final Location outputLocation,
                                             final String packageName,
                                             final String relativeName,
                                             final FileObject sibling)
      throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public final FileObject getFileForOutputForOriginatingFiles(final Location outputLocation,
                                                                final String packageName,
                                                                final String relativeName,
                                                                final FileObject... originatingFiles)
      throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public final JavaFileObject getJavaFileForOutput(final Location packageOrientedLocation,
                                                     final String className,
                                                     final JavaFileObject.Kind kind,
                                                     final FileObject sibling)
      throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public final JavaFileObject getJavaFileForOutputForOriginatingFiles(final Location packageOrientedLocation,
                                                                        final String className,
                                                                        final JavaFileObject.Kind kind,
                                                                        final FileObject... originatingFiles)
      throws IOException {
      throw new UnsupportedOperationException();
    }

    // Returns a module-oriented location or an output location.
    @Override
    public final Location getLocationForModule(final Location moduleOrientedOrOutputLocation,
                                               final String moduleName) throws IOException {
      throw new UnsupportedOperationException();
    }

    // Returns a module-oriented location or an output location.
    @Override
    public final Location getLocationForModule(final Location moduleOrientedOrOutputLocation,
                                               final JavaFileObject fileObject)
      throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public final boolean isSameFile(final FileObject a, final FileObject b) {
      throw new UnsupportedOperationException();
    }


    /*
     * Static methods.
     */


    private static final JavaFileObject.Kind kind(final String s) {
      return kind(ALL_KINDS, s);
    }

    private static final JavaFileObject.Kind kind(final Iterable<? extends JavaFileObject.Kind> kinds, final String s) {
      for (final JavaFileObject.Kind kind : kinds) {
        if (kind != JavaFileObject.Kind.OTHER && s.endsWith(kind.extension)) {
          return kind;
        }
      }
      return JavaFileObject.Kind.OTHER;
    }

    private static final Iterable<JavaFileObject> list(final ModuleReader mr,
                                                       final String packageName,
                                                       final Set<JavaFileObject.Kind> kinds,
                                                       final boolean recurse)
      throws IOException {
      final String p = packageName.replace('.', '/');
      final int packagePrefixLength = p.length() + 1;
      try (final Stream<String> ss = mr.list()) {
        return ss
          .filter(s ->
                  !s.endsWith("/") &&
                  s.startsWith(p) &&
                  isAKind(kinds, s) &&
                  (recurse || s.indexOf('/', packagePrefixLength) < 0))
          .map(s -> {
              final JavaFileObject.Kind kind = kind(kinds, s);
              try {
                return
                  new JavaFileRecord(kind,
                                     kind == JavaFileObject.Kind.CLASS || kind == JavaFileObject.Kind.SOURCE ?
                                     s.substring(0, s.length() - kind.extension.length()).replace('/', '.') :
                                     null,
                                     mr.find(s).orElseThrow());
              } catch (final IOException ioException) {
                throw new UncheckedIOException(ioException.getMessage(), ioException);
              }
            })
          .collect(Collectors.toUnmodifiableList());
      }
    }

    private static final boolean isAKind(final Set<JavaFileObject.Kind> kinds, final String moduleResource) {
      for (final JavaFileObject.Kind k : kinds) {
        if (moduleResource.endsWith(k.extension)) {
          return true;
        }
      }
      return false;
    }

    private static final record JavaFileRecord(JavaFileObject.Kind kind, String binaryName, URI uri) implements JavaFileObject {

      @Override
      public final URI toUri() {
        return this.uri();
      }

      @Override
      public final NestingKind getNestingKind() {
        return null;
      }

      @Override
      public final Modifier getAccessLevel() {
        return null;
      }

      @Override
      public final long getLastModified() {
        return 0L;
      }

      @Override
      public final Reader openReader(final boolean ignoreEncodingErrors) {
        throw new UnsupportedOperationException();
      }

      @Override
      public final OutputStream openOutputStream() {
        throw new UnsupportedOperationException();
      }

      @Override
      public final CharSequence getCharContent(final boolean ignoreEncodingErrors) {
        throw new UnsupportedOperationException();
      }

      @Override
      public final Writer openWriter() {
        throw new UnsupportedOperationException();
      }

      @Override
      public final JavaFileObject.Kind getKind() {
        return this.kind();
      }

      private final String path() {
        final String path = this.uri().getPath();
        if (path == null) {
          // Probably a jar: URI
          final String ssp = this.uri().getSchemeSpecificPart();
          return ssp.substring(ssp.lastIndexOf('!') + 1);
        }
        return path;
      }

      @Override
      public final String getName() {
        return this.path();
      }

      @Override
      public final boolean isNameCompatible(final String simpleName, final JavaFileObject.Kind kind) {
        if (kind != this.kind()) {
          return false;
        }
        final String basename = simpleName + kind.extension;
        final String path = this.path();
        return path.equals(basename) || path.endsWith("/" + basename);
      }

      @Override
      public final boolean delete() {
        return false;
      }

      @Override
      public final InputStream openInputStream() throws IOException {
        return this.uri().toURL().openConnection().getInputStream();
      }

    }

  }


}
