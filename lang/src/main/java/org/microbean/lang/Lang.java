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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import java.util.concurrent.CountDownLatch;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;

import javax.tools.ToolProvider;

import javax.lang.model.SourceVersion;

import org.microbean.lang.element.DelegatingElement;

import org.microbean.lang.type.DelegatingTypeMirror;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;
import static java.lang.constant.ConstantDescs.NULL;
import static java.lang.constant.DirectMethodHandleDesc.Kind.STATIC;
import static java.lang.constant.DirectMethodHandleDesc.Kind.STATIC_GETTER;

import static javax.lang.model.util.ElementFilter.constructorsIn;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;

public final class Lang {


  /*
   * Static fields.
   */


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

  private static final TypeMirror[] EMPTY_TYPEMIRROR_ARRAY = new TypeMirror[0];

  private static final CountDownLatch initLatch = new CountDownLatch(1);

  private static volatile ProcessingEnvironment pe;


  /*
   * Static initializer.
   */


  static {
    initialize();
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

  public static final Optional<? extends ConstantDesc> describeConstable(final Element e) {
    if (e == null) {
      return Optional.of(NULL);
    }
    final ElementKind k = e.getKind();
    return switch (k) {
    case MODULE -> describeConstable((ModuleElement)e);
    case PACKAGE -> describeConstable((PackageElement)e);
    // TODO: others probably need to be handled but not as urgently
    case ElementKind ek when ek.isDeclaredType() -> describeConstable((TypeElement)e);
    default -> Optional.empty();
    };
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
    if (t == null) {
      return Optional.of(NULL);
    }
    return switch (t.getKind()) {
    case ARRAY -> describeConstable((ArrayType)t);
    case BOOLEAN, BYTE, CHAR, DOUBLE, FLOAT, INT, LONG, SHORT -> describeConstable((PrimitiveType)t);
    case DECLARED, ERROR -> describeConstable((DeclaredType)t);
    case EXECUTABLE, INTERSECTION, OTHER, TYPEVAR, UNION -> Optional.empty();
    case MODULE, NONE, PACKAGE, VOID -> describeConstable((NoType)t);
    case NULL -> describeConstable((NullType)t);
    case WILDCARD -> describeConstable((WildcardType)t);
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
                                                                                 "arrayTypeOf",
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


  public static final Name binaryName(final TypeElement e) {
    // This does not cause completion.
    return pe().getElementUtils().getBinaryName(unwrap(e));
  }

  public static final boolean functionalInterface(TypeElement e) {
    e = unwrap(e);
    final Elements elements = pe().getElementUtils();
    // JavacElements#isFunctionalInterface(Element) calls Element#getKind().
    synchronized (CompletionLock.monitor()) {
      return elements.isFunctionalInterface(e);
    }
  }
  
  public static final TypeMirror capture(TypeMirror t) {
    t = unwrap(t);
    // JavacTypes#capture(TypeMirror) calls TypeMirror#getKind().
    final Types types = pe().getTypeUtils();
    final TypeMirror rv;
    synchronized (CompletionLock.monitor()) {
      rv = types.capture(t);
    }
    return wrap(rv);
  }

  public static final boolean contains(TypeMirror t, TypeMirror s) {
    t = unwrap(t);
    s = unwrap(s);
    // JavacTypes#contains(TypeMirror, TypeMirror) calls TypeMirror#getKind().
    final Types types = pe().getTypeUtils();
    synchronized (CompletionLock.monitor()) {
      return types.contains(t, s);
    }
  }

  public static final Element element(TypeMirror t) {
    t = unwrap(t);
    final Types types = pe().getTypeUtils();
    final Element rv;
    // JavacTypes#asElement(TypeMirror) calls TypeMirror#getKind().
    synchronized (CompletionLock.monitor()) {
      rv = types.asElement(t);
    }
    return wrap(rv);
  }

  public static final TypeMirror memberOf(DeclaredType t, Element e) {
    t = unwrap(t);
    e = unwrap(e);
    final Types types = pe().getTypeUtils();
    final TypeMirror rv;
    synchronized (CompletionLock.monitor()) {
      rv = types.asMemberOf(t, e);
    }
    return wrap(rv);
  }

  public static final TypeMirror box(TypeMirror t) {
    final TypeKind k;
    synchronized (CompletionLock.monitor()) {
      k = t.getKind();
    }
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

  public static final PrimitiveType unboxedType(TypeMirror t) {
    t = unwrap(t);
    final Types types = pe().getTypeUtils();
    // JavacTypes#unboxedType(TypeMirror) calls TypeMirror#getKind().
    synchronized (CompletionLock.monitor()) {
      return types.unboxedType(t);
    }
  }

  public static final List<? extends TypeMirror> directSupertypes(TypeMirror t) {
    t = unwrap(t);
    final Types types = pe().getTypeUtils();
    final List<? extends TypeMirror> rv;
    synchronized (CompletionLock.monitor()) {
      rv = types.directSupertypes(t);
    }
    return wrap(rv);
  }

  public static final boolean subsignature(ExecutableType e, ExecutableType f) {
    e = unwrap(e);
    f = unwrap(f);
    final Types types = pe().getTypeUtils();
    synchronized (CompletionLock.monitor()) {
      return types.isSubsignature(e, f);
    }
  }

  public static final TypeMirror erasure(TypeMirror t) {
    t = unwrap(t);
    final Types types = pe().getTypeUtils();
    // JavacTypes#erasure(TypeMirror) calls TypeMirror#getKind().
    synchronized (CompletionLock.monitor()) {
      t = types.erasure(t);
    }
    return wrap(t);
  }

  public static final ElementSource elementSource() {
    return ConstableElementSource.INSTANCE;
  }

  public static final ModuleElement moduleElement(final Class<?> c) {
    return c == null ? null : moduleElement(c.getModule());
  }

  public static final ModuleElement moduleElement(final Module module) {
    return module == null ? null : moduleElement(module.getName());
  }

  public static final ModuleElement moduleElement(final CharSequence moduleName) {
    final Elements elements = pe().getElementUtils();
    final ModuleElement rv;
    // Not absolutely clear this causes completion but...
    synchronized (CompletionLock.monitor()) {
      rv = elements.getModuleElement(moduleName);
    }
    return wrap(rv);
  }

  public static final ModuleElement moduleOf(final Element e) {
    // This does NOT appear to cause completion.
    return wrap(pe().getElementUtils().getModuleOf(unwrap(e)));
  }

  public static final Name name(final CharSequence name) {
    return pe().getElementUtils().getName(name);
  }

  public static final PackageElement packageElement(final Class<?> c) {
    return c == null ? null : packageElement(c.getModule(), c.getPackage());
  }

  public static final PackageElement packageElement(final Package pkg) {
    return pkg == null ? null : packageElement(pkg.getName());
  }

  public static final PackageElement packageElement(final CharSequence fullyQualifiedName) {
    final Elements elements = pe().getElementUtils();
    // JavacElements#getPackageElement() may end up calling JavacElements#nameToSymbol(ModuleSymbol, String, Class),
    // which calls complete() in certain code paths.
    final PackageElement rv;
    synchronized (CompletionLock.monitor()) {
      rv = elements.getPackageElement(fullyQualifiedName == null ? "" : fullyQualifiedName);
    }
    return wrap(rv);
  }

  public static final PackageElement packageElement(final Module module, final Package pkg) {
    return packageElement(moduleElement(module), pkg);
  }

  public static final PackageElement packageElement(final ModuleElement moduleElement, final Package pkg) {
    return packageElement(moduleElement, pkg == null ? null : pkg.getName());
  }

  public static final PackageElement packageElement(final ModuleElement moduleElement, final CharSequence fullyQualifiedName) {
    final Elements elements = pe().getElementUtils();
    final PackageElement rv;
    synchronized (CompletionLock.monitor()) {
      rv = elements.getPackageElement(moduleElement, fullyQualifiedName);
    }
    return wrap(rv);
  }

  public static final PackageElement packageOf(final Element e) {
    // This does NOT appear to cause completion.
    return wrap(pe().getElementUtils().getPackageOf(unwrap(e)));
  }

  public static final ArrayType arrayType(final Class<?> arrayClass) {
    if (arrayClass == null || !arrayClass.isArray()) {
      return null;
    }
    return arrayTypeOf(type(arrayClass.getComponentType()));
  }

  public static final ArrayType arrayType(final GenericArrayType g) {
    if (g == null) {
      return null;
    }
    return arrayTypeOf(type(g.getGenericComponentType()));
  }

  // Called by describeConstable().
  public static final ArrayType arrayTypeOf(TypeMirror componentType) {
    componentType = unwrap(componentType);
    final Types types = pe().getTypeUtils();
    final ArrayType rv;
    // JavacTypes#getArrayType(TypeMirror) calls getKind() on the component type.
    synchronized (CompletionLock.monitor()) {
      rv = types.getArrayType(componentType);
    }
    return wrap(rv);
  }

  public static final DeclaredType declaredType(final Type t) {
    return switch (t) {
    case null -> null;
    case Class<?> c -> declaredType(c);
    case ParameterizedType p -> declaredType(p);
    default -> null;
    };
  }

  public static final DeclaredType declaredType(final Class<?> c) {
    if (c == null || c.isPrimitive() || c.isArray() || c.isLocalClass() || c.isAnonymousClass()) {
      return null;
    }
    return declaredType(declaredType(c.getEnclosingClass()), typeElement(c));
  }

  public static final DeclaredType declaredType(final ParameterizedType pt) {
    if (pt == null) {
      return null;
    }
    return
      declaredType(declaredType(pt.getOwnerType()),
                   typeElement(pt.getRawType()),
                   typeArray(pt.getActualTypeArguments()));
  }

  public static final DeclaredType declaredType(final Type rawType, // usually (always) a Class<?>
                                                final Type... typeArguments) {
    return declaredType(typeElement(rawType), typeArray(typeArguments));
  }

  public static final DeclaredType declaredType(final Type rawType, // usually (always) a Class<?>
                                                final TypeMirror... typeArguments) {
    // Most commonly used when typeArguments is a single WildcardType
    return declaredType(typeElement(rawType), typeArguments);
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
    // JavacTypes#getDeclaredType() can call erasure() internally which can cause completion. It also might call
    // getTypeArguments() which can cause completion.
    synchronized (CompletionLock.monitor()) {
      // STILL occasionally getting:
      //
      // java.lang.NullPointerException: Cannot read field "type" because "sym" is null
      //     at jdk.compiler/com.sun.tools.javac.model.JavacTypes.getDeclaredType(JavacTypes.java:238)
      //     at org.microbean.lang@0.0.1-SNAPSHOT/org.microbean.lang.Lang.declaredType(Lang.java:702) // <-- that's here
      //     at org.microbean.bean@0.0.1-SNAPSHOT/org.microbean.bean.TestSelector.testSelectorListUnknownExtendsStringSelectsListString(TestSelector.java:83)
      rv = types.getDeclaredType(typeElement, typeArguments);
    }
    return wrap(rv);
  }

  public static final DeclaredType declaredType(final Type ownerType,
                                                final Type rawType,
                                                final Type... typeArguments) {
    return declaredType(declaredType(ownerType), typeElement(rawType), typeArray(typeArguments));
  }


  public static final DeclaredType declaredType(DeclaredType containingType,
                                                TypeElement typeElement,
                                                TypeMirror... typeArguments) {
    containingType = unwrap(containingType);
    typeElement = unwrap(typeElement);
    typeArguments = unwrap(typeArguments);
    final Types types = pe().getTypeUtils();
    final DeclaredType rv;
    synchronized (CompletionLock.monitor()) {
      rv = types.getDeclaredType(containingType, typeElement, typeArguments);
    }
    return wrap(rv);
  }

  public static final ExecutableElement executableElement(final Executable e) {
    return switch (e) {
    case null -> null;
    case Constructor<?> c -> executableElement(c);
    case Method m -> executableElement(m);
    default -> null;
    };
  }

  public static final ExecutableElement executableElement(final Constructor<?> c) {
    if (c == null) {
      return null;
    }
    return executableElement(typeElement(c.getDeclaringClass()), typeArray(c.getParameterTypes())); // deliberate erasure
  }

  public static final ExecutableElement executableElement(final Method m) {
    if (m == null) {
      return null;
    }
    return executableElement(typeElement(m.getDeclaringClass()),
                             m.getName(),
                             type(m.getReturnType()), // deliberate erasure
                             typeArray(m.getParameterTypes())); // deliberate erasure
  }

  public static final ExecutableElement executableElement(TypeElement declaringClass,
                                                          final List<? extends TypeMirror> parameterTypes) {
    if (declaringClass == null) {
      return null;
    }
    declaringClass = unwrap(declaringClass);
    final Types types = pe().getTypeUtils();
    ExecutableElement rv = null;
    final int parameterTypesSize = parameterTypes == null ? 0 : parameterTypes.size();
    synchronized (CompletionLock.monitor()) {
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
    }
    return wrap(rv);
  }

  public static final ExecutableElement executableElement(TypeElement declaringClass,
                                                          final TypeMirror... parameterTypes) {
    if (declaringClass == null) {
      return null;
    }
    declaringClass = unwrap(declaringClass);
    final Types types = pe().getTypeUtils();
    ExecutableElement rv = null;
    final int parameterTypesSize = parameterTypes == null ? 0 : parameterTypes.length;
    synchronized (CompletionLock.monitor()) {
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
    }
    return wrap(rv);
  }

  public static final ExecutableElement executableElement(TypeElement declaringClass,
                                                          final CharSequence name,
                                                          TypeMirror returnType,
                                                          final List<? extends TypeMirror> parameterTypes) {
    if (declaringClass == null || returnType == null || name == null) {
      return null;
    }
    declaringClass = unwrap(declaringClass);
    final Types types = pe().getTypeUtils();
    ExecutableElement rv = null;
    final int parameterTypesSize = parameterTypes == null ? 0 : parameterTypes.size();
    synchronized (CompletionLock.monitor()) {
      returnType = types.erasure(unwrap(returnType));
      METHOD_LOOP:
      for (final ExecutableElement m : (Iterable<? extends ExecutableElement>)methodsIn(declaringClass.getEnclosedElements())) {
        if (m.getSimpleName().contentEquals(name) && types.isSameType(types.erasure(m.getReturnType()), returnType)) {
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
    }
    return wrap(rv);
  }

  public static final ExecutableElement executableElement(TypeElement declaringClass,
                                                          final CharSequence name,
                                                          TypeMirror returnType,
                                                          final TypeMirror... parameterTypes) {
    if (declaringClass == null || returnType == null || name == null) {
      return null;
    }
    declaringClass = unwrap(declaringClass);
    final Types types = pe().getTypeUtils();
    ExecutableElement rv = null;
    final int parameterTypesSize = parameterTypes == null ? 0 : parameterTypes.length;
    synchronized (CompletionLock.monitor()) {
      returnType = types.erasure(unwrap(returnType));
      METHOD_LOOP:
      for (final ExecutableElement m : (Iterable<? extends ExecutableElement>)methodsIn(declaringClass.getEnclosedElements())) {
        if (m.getSimpleName().contentEquals(name) && types.isSameType(types.erasure(m.getReturnType()), returnType)) {
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
    }
    return wrap(rv);
  }

  public static final ExecutableType executableType(final Executable e) {
    return e == null ? null : (ExecutableType)executableElement(e).asType();
  }

  public static final NoType noType(final TypeKind k) {
    return pe().getTypeUtils().getNoType(k);
  }

  // Called by describeConstable
  public static final NullType nullType() {
    return pe().getTypeUtils().getNullType();
  }

  public static final PrimitiveType primitiveType(final Class<?> c) {
    if (c == null || !c.isPrimitive()) {
      return null;
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
    synchronized (CompletionLock.monitor()) {
      // Internally JavacTypes calls getKind() on each type, causing symbol completion
      return types.isSameType(t, s);
    }
  }

  public static final TypeElement typeElement(final Type t) {
    return switch (t) {
    case null -> null;
    case Class<?> c -> typeElement(c);
    default -> null;
    };
  }

  public static final TypeElement typeElement(final Class<?> c) {
    final Module m = c.getModule();
    return m == null ? typeElement(c.getCanonicalName()) : typeElement(moduleElement(m), c.getCanonicalName());
  }

  public static final TypeElement typeElement(final CharSequence canonicalName) {
    final Elements elements = pe().getElementUtils();
    final TypeElement rv;
    synchronized (CompletionLock.monitor()) {
      rv = elements.getTypeElement(canonicalName);
    }
    return wrap(rv);
  }

  public static final TypeElement typeElement(final Module module, final CharSequence canonicalName) {
    return module == null ? typeElement(canonicalName) : typeElement(moduleElement(module), canonicalName);
  }

  public static final TypeElement typeElement(ModuleElement moduleElement, final CharSequence canonicalName) {
    if (moduleElement == null) {
      return typeElement(canonicalName);
    }
    moduleElement = unwrap(moduleElement);
    final Elements elements = pe().getElementUtils();
    final TypeElement rv;
    synchronized (CompletionLock.monitor()) {
      rv = elements.getTypeElement(moduleElement, canonicalName);
    }
    return wrap(rv);
  }

  public static final Parameterizable parameterizable(final GenericDeclaration gd) {
    return switch (gd) {
    case null -> null;
    case Executable e -> executableElement(e);
    case Class<?> c -> typeElement(c);
    default -> null;
    };
  }

  public static final TypeParameterElement typeParameterElement(final java.lang.reflect.TypeVariable<?> t) {
    return typeParameterElement(t.getGenericDeclaration(), t.getName());
  }

  public static final TypeParameterElement typeParameterElement(GenericDeclaration gd, final String name) {
    while (gd != null) {
      java.lang.reflect.TypeVariable<?>[] typeParameters = gd.getTypeParameters();
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
    if (f == null) {
      return null;
    }
    synchronized (CompletionLock.monitor()) {
      // Conveniently, fieldsIn() doesn't use internal javac constructs so we can just skip the unwrapping.
      for (final VariableElement ve : (Iterable<? extends VariableElement>)fieldsIn(typeElement(f.getDeclaringClass()).getEnclosedElements())) {
        if (ve.getSimpleName().contentEquals(f.getName())) {
          return ve;
        }
      }
    }
    return null;
  }

  public static final WildcardType wildcardType() {
    return wildcardType(null, null);
  }

  public static final WildcardType wildcardType(final java.lang.reflect.WildcardType t) {
    if (t == null) {
      return null;
    }
    final Type[] lowerBounds = t.getLowerBounds();
    final Type lowerBound = lowerBounds.length <= 0 ? null : lowerBounds[0];
    final Type upperBound = t.getUpperBounds()[0];
    return wildcardType(type(upperBound), type(lowerBound));
  }

  public static final WildcardType wildcardType(TypeMirror extendsBound, TypeMirror superBound) {
    extendsBound = unwrap(extendsBound);
    superBound = unwrap(superBound);
    final Types types = pe().getTypeUtils();
    final WildcardType rv;
    synchronized (CompletionLock.monitor()) {
      // JavacTypes#getWildcardType() can call getKind() on bounds etc. which triggers symbol completion
      rv = types.getWildcardType(extendsBound, superBound);
    }
    return wrap(rv);
  }

  public static final boolean assignable(TypeMirror payload, TypeMirror receiver) {
    payload = unwrap(payload);
    receiver = unwrap(receiver);
    final Types types = pe().getTypeUtils();
    synchronized (CompletionLock.monitor()) {
      return types.isAssignable(payload, receiver);
    }
  }

  public static final boolean subtype(TypeMirror payload, TypeMirror receiver) {
    payload = unwrap(payload);
    receiver = unwrap(receiver);
    final Types types = pe().getTypeUtils();
    synchronized (CompletionLock.monitor()) {
      return types.isSubtype(payload, receiver);
    }
  }

  public static final TypeMirror type(final Type t) {
    return switch (t) {
    case null -> null;
    case Class<?> c when c == void.class -> noType(TypeKind.VOID);
    case Class<?> c when c.isArray() -> arrayType(c);
    case Class<?> c when c.isPrimitive() -> primitiveType(c);
    case Class<?> c -> declaredType(c);
    case ParameterizedType pt -> declaredType(pt);
    case GenericArrayType g -> arrayType(g);
    case java.lang.reflect.TypeVariable<?> tv -> typeVariable(tv);
    case java.lang.reflect.WildcardType w -> wildcardType(w);
    default -> null;
    };
  }

  public static final TypeMirror type(final Field f) {
    return f == null ? null : variableElement(f).asType();
  }


  /*
   * Private static methods.
   */


  private static final TypeMirror[] typeArray(final Type[] ts) {
    if (ts == null || ts.length <= 0) {
      return EMPTY_TYPEMIRROR_ARRAY;
    }
    final TypeMirror[] rv = new TypeMirror[ts.length];
    for (int i = 0; i < ts.length; i++) {
      rv[i] = type(ts[i]);
    }
    return rv;
  }

  private static final TypeMirror[] typeArray(final List<? extends Type> ts) {
    if (ts == null || ts.isEmpty()) {
      return EMPTY_TYPEMIRROR_ARRAY;
    }
    final TypeMirror[] rv = new TypeMirror[ts.size()];
    for (int i = 0; i < ts.size(); i++) {
      rv[i] = type(ts.get(i));
    }
    return rv;
  }

  private static final List<? extends TypeMirror> typeList(final Type[] ts) {
    if (ts == null || ts.length <= 0) {
      return List.of();
    }
    final List<TypeMirror> rv = new ArrayList<>(ts.length);
    for (final Type t : ts) {
      rv.add(type(t));
    }
    return Collections.unmodifiableList(rv);
  }

  static final List<? extends TypeMirror> typeList(final Collection<? extends Type> ts) {
    if (ts == null || ts.isEmpty()) {
      return List.of();
    }
    final List<TypeMirror> rv = new ArrayList<>(ts.size());
    for (final Type t : ts) {
      rv.add(type(t));
    }
    return Collections.unmodifiableList(rv);
  }

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

  // Called once, ever, by the static initializer. Idempotent.
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
        // Note to future maintainers: you're going to desperately want to move this to the process() method, and you
        // cannot.  If you decide to doubt this message, at least comment this out so you don't lose it here.  Don't say
        // I didn't warn you.
        try {
          runningLatch.await();
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }

      @Override // AbstractProcessor
      public final Set<String> getSupportedAnnotationTypes() {
        return Set.of(); // we claim nothing, although it's moot because we're the only processor in existence
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
        assert this.isInitialized();
        return false; // we don't claim anything, but we're the only processor in existence
      }

    };

    final Thread t = new Thread(() -> {
        try {
          final JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
          if (jc == null) {
            runningLatch.countDown();
            initLatch.countDown();
            return;
          }
          final List<String> options = new ArrayList<>();
          options.add("-proc:only");
          options.add("-sourcepath");
          options.add("");
          options.add("-cp");
          final String modulePath = System.getProperty("jdk.module.path"); // TODO: seems like we could do better, maybe using ModuleReference et al.
          if (modulePath == null) {
            options.add(System.getProperty("java.class.path"));
          } else {
            options.add(System.getProperty("java.class.path") + java.io.File.pathSeparator + modulePath); // TODO: yuck?!
            options.add("-p");
            options.add(modulePath);
          }
          final List<String> classes = new ArrayList<>();
          classes.add("java.lang.annotation.RetentionPolicy"); // loads the least amount of stuff up front
          if (Boolean.getBoolean("org.microbean.lang.Lang.verbose")) {
            options.add("-verbose");
          }
          // (Any "loading" is actually performed by, e.g. com.sun.tools.javac.jvm.ClassReader.fillIn(), not reflective
          // machinery.)
          final CompilationTask task =
            jc.getTask(null, // additionalOutputWriter
                       null, // fileManager,
                       null, // diagnosticListener,
                       options,
                       classes,
                       null); // compilation units; null means we aren't actually compiling anything
          task.setProcessors(List.of(new P()));
          if (Boolean.FALSE.equals(task.call())) { // NOTE: runs the task
            runningLatch.countDown();
            initLatch.countDown();
          }
        } catch (final RuntimeException | Error e) {
          e.printStackTrace();
          runningLatch.countDown();
          initLatch.countDown();
          throw e;
        } finally {
          pe = null; // volatile write
        }
    }, "Lang");
    t.setDaemon(true); // critical; runningLatch is never counted down except in error cases
    t.start();
  }

  @SuppressWarnings("unchecked")
  public static final <T extends TypeMirror> T unwrap(final T t) {
    return t == null ? null : (T)DelegatingTypeMirror.unwrap(t);
  }

  public static final TypeMirror[] unwrap(final TypeMirror[] ts) {
    return ts == null ? null : DelegatingTypeMirror.unwrap(ts);
  }

  @SuppressWarnings("unchecked")
  public static final <T extends TypeMirror> T wrap(final T t) {
    return t == null ? null : (T)DelegatingTypeMirror.of(t, elementSource(), null);
  }

  public static final List<? extends TypeMirror> wrap(final Collection<? extends TypeMirror> ts) {
    return DelegatingTypeMirror.of(ts, elementSource(), null);
  }

  @SuppressWarnings("unchecked")
  public static final <E extends Element> E unwrap(final E e) {
    return e == null ? null : (E)DelegatingElement.unwrap(e);
  }

  @SuppressWarnings("unchecked")
  public static final <E extends Element> E wrap(final E e) {
    return e == null ? null : (E)DelegatingElement.of(e, elementSource(), null);
  }


  /*
   * Inner and nested classes.
   */


  public static final class ConstableElementSource implements Constable, ElementSource {

    private static final ClassDesc CD_ConstableElementSource = ClassDesc.of(ConstableElementSource.class.getName());

    public static final ConstableElementSource INSTANCE = new ConstableElementSource();

    private ConstableElementSource() {
      super();
    }

    @Override
    public final Element element(final String moduleName, final String name) {
      return moduleName == null ? typeElement(name) : typeElement(moduleElement(moduleName), name);
    }

    @Override
    public final Optional<DynamicConstantDesc<ConstableElementSource>> describeConstable() {
      return
        Optional.of(DynamicConstantDesc.of(BSM_INVOKE,
                                           MethodHandleDesc.ofField(STATIC_GETTER,
                                                                    CD_ConstableElementSource,
                                                                    "INSTANCE",
                                                                    CD_ConstableElementSource)));
    }

  }


}
