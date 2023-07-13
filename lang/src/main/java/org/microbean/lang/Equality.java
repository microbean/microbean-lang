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

import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.lang.model.AnnotatedConstruct;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement.Directive;
import javax.lang.model.element.ModuleElement.DirectiveKind;
import javax.lang.model.element.ModuleElement.ExportsDirective;
import javax.lang.model.element.ModuleElement.OpensDirective;
import javax.lang.model.element.ModuleElement.ProvidesDirective;
import javax.lang.model.element.ModuleElement.RequiresDirective;
import javax.lang.model.element.ModuleElement.UsesDirective;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;
import static java.lang.constant.ConstantDescs.CD_boolean;
import static java.lang.constant.ConstantDescs.FALSE;
import static java.lang.constant.ConstantDescs.TRUE;

/**
 * Provides determinate hashcode and equality calculations chiefly for {@code javax.lang.model} implementations.
 *
 * @author <a href="https://about.me/lairdnelson" target="_parent">Laird Nelson</a>
 *
 * @see #hashCode(Object)
 *
 * @see #equals(Object, Object)
 */
public class Equality implements Constable {

  private static final ClassDesc CD_Equality = ClassDesc.of("org.microbean.lang.Equality");
  
  private final boolean ia;

  /**
   * Creates a new {@link Equality}.
   *
   * @param includeAnnotations whether annotations on objects should be taken into consideration while performing
   * hashcode and equality operations
   *
   * @see #includeAnnotations()
   */
  public Equality(final boolean includeAnnotations) {
    super();
    this.ia = includeAnnotations;
  }

  public final boolean includeAnnotations() {
    return this.ia;
  }

  @Override // Constable
  public Optional<DynamicConstantDesc<Equality>> describeConstable() {
    return Optional.of(DynamicConstantDesc.of(BSM_INVOKE,
                                              MethodHandleDesc.ofConstructor(CD_Equality, CD_boolean),
                                              this.ia ? TRUE : FALSE));
  }
  
  public int hashCode(final Object o1) {
    return hashCode(o1, this.ia);
  }

  public boolean equals(final Object o1, final Object o2) {
    return equals(o1, o2, this.ia);
  }


  /*
   * Static methods.
   */


  public static final int hashCodeIncludingAnnotations(final Object o) {
    return hashCode(o, true);
  }

  public static final int hashCodeNotIncludingAnnotations(final Object o) {
    return hashCode(o, false);
  }

  public static final int hashCode(final Object o, final boolean ia) {
    return switch (o) {
    case null -> 0;
    case AnnotationMirror am -> hashCode(am, ia);
    case AnnotationValue av -> hashCode(av, ia);
    case AnnotatedConstruct ac -> hashCode(ac, ia);
    case CharSequence c -> hashCode(c);
    case List<?> list -> hashCode(list, ia);
    case int[] hashCodes -> hashCode(hashCodes);
    case Object[] array -> hashCode(array, ia);
    case Directive d -> hashCode(d, ia);
    default -> System.identityHashCode(o);
    };
  }

  private static final int hashCode(final int... hashCodes) {
    if (hashCodes == null) {
      return 0;
    } else if (hashCodes.length <= 0) {
      return 1;
    }
    int result = 1;
    for (final int hashCode : hashCodes) {
      result = 31 * result + hashCode;
    }
    return result;
  }

  private static final int hashCode(final Object[] os, final boolean ia) {
    if (os == null) {
      return 0;
    } else if (os.length <= 0) {
      return 1;
    }
    int result = 1;
    for (final Object o : os) {
      result = 31 * result + (o == null ? 0 : hashCode(o, ia));
    }
    return result;
  }

  private static final int hashCode(final List<?> list, final boolean ia) {
    if (list == null) {
      return 0;
    } else if (list.isEmpty()) {
      return 1;
    }
    // This calculation is mandated by java.util.List#hashCode().
    int hashCode = 1;
    for (final Object o : list) {
      hashCode = 31 * hashCode + (o == null ? 0 : hashCode(o, ia));
    }
    return hashCode;
  }

  private static final int hashCode(final CharSequence c) {
    return switch (c) {
    case null -> 0;
    case Name n -> n.toString().hashCode();
    default -> c.hashCode();
    };
  }

  private static final int hashCode(final AnnotationMirror am, final boolean ia) {
    return am == null ? 0 : hashCode(values(am), ia);
  }

  private static final int hashCode(final AnnotationValue av, final boolean ia) {
    return switch (av) {
    case null -> 0;
    case AnnotationMirror am -> hashCode(am, ia);
    case List<?> list -> hashCode(list, ia);
    case TypeMirror t -> hashCode(t, ia);
    case VariableElement e -> hashCode(e, ia);
    default -> System.identityHashCode(av); // illegal argument
    };
  }

  private static final int hashCode(final AnnotatedConstruct ac, final boolean ia) {
    return switch (ac) {
    case null -> 0;
    case Element e -> hashCode(e, ia);
    case TypeMirror t -> hashCode(t, ia);
    default -> System.identityHashCode(ac); // basically illegal argument
    };
  }

  private static final int hashCode(final Element e, final boolean ia) {
    // This is the entry point for all Element hashCode calculations. We have to synchronize on the completion lock
    // because various elements' getKind() methods may trigger symbol completion.
    synchronized (CompletionLock.monitor()) {
      return e == null ? 0 : switch (e.getKind()) {
      case ANNOTATION_TYPE, CLASS, ENUM, INTERFACE, RECORD -> hashCode((TypeElement)e, ia);
      case TYPE_PARAMETER -> hashCode((TypeParameterElement)e, ia);
      case BINDING_VARIABLE, ENUM_CONSTANT, EXCEPTION_PARAMETER, FIELD, LOCAL_VARIABLE, PARAMETER, RESOURCE_VARIABLE -> hashCode((VariableElement)e, ia);
      case RECORD_COMPONENT -> hashCode((RecordComponentElement)e, ia);
      case CONSTRUCTOR, INSTANCE_INIT, METHOD, STATIC_INIT -> hashCode((ExecutableElement)e, ia);
      case PACKAGE -> hashCode((PackageElement)e, ia);
      case MODULE -> hashCode((ModuleElement)e, ia);
      default -> System.identityHashCode(e); // basically illegal argument
      };
    }
  }

  private static final int hashCode(final ExecutableElement e, final boolean ia) {
    if (e == null) {
      return 0;
    } else if (!e.getKind().isExecutable()) {
      return System.identityHashCode(e); // illegal argument
    }
    // This gets tricky. If we're truly trying to do value-based hashcodes, we have a catch-22: a method's hashcode must
    // include the hashcodes of its parameters, and parameters, taken in isolation, must include the hashcodes of their
    // enclosing element (method).  That's an infinite loop.
    //
    // Next, VariableElement, which is the class assigned to executable/method/constructor parameters, is also used for
    // things like fields.  So we can't make assumptions about its hashcode calculations, save for one:
    //
    // Given that VariableElements are always enclosed
    // (https://docs.oracle.com/en/java/javase/19/docs/api/java.compiler/javax/lang/model/element/VariableElement.html#getEnclosingElement()),
    // it is reasonable to assume that any given VariableElement implementation will likely include the return value of
    // getEnclosingElement() in its hashcode calculations.  But the only enclosing "thing" that would "normally" include
    // the hashcode calculations of its *enclosed* elements is an ExecutableElement (because its parameters make up its
    // identity).
    //
    // So probably the best place to break that infinite loop is here, not there.
    //
    // Now, these are hashcodes, not equality comparisons, so the worst that happens if we eliminate parameters from the
    // mix is a collision for overridden methods. That's what the java.lang.reflect.* Executable/Parameter split does.
    return
      hashCode(e.getKind().hashCode(),
               hashCode(e.getEnclosingElement(), ia), // this is OK
               hashCode(e.getSimpleName()),
               hashCode(e.getParameters().size(), ia),
               // hashCode(e.getParameters(), ia), // this is problematic because each parameter includes this in its hashcode calculations
               hashCode(e.getReturnType(), ia),
               ia ? hashCode(e.getAnnotationMirrors(), ia) : 0);
  }

  private static final int hashCode(final ModuleElement e, final boolean ia) {
    if (e == null) {
      return 0;
    } else if (e.getKind() != ElementKind.MODULE) {
      return System.identityHashCode(e); // illegal argument
    }
    return
      hashCode(e.getKind().hashCode(),
               hashCode(e.getQualifiedName()),
               ia ? hashCode(e.getAnnotationMirrors(), ia) : 0);
  }

  private static final int hashCode(final PackageElement e, final boolean ia) {
    if (e == null) {
      return 0;
    } else if (e.getKind() != ElementKind.PACKAGE) {
      return System.identityHashCode(e); // illegal argument
    }
    return
      hashCode(e.getKind().hashCode(),
               hashCode(e.getQualifiedName()),
               ia ? hashCode(e.getAnnotationMirrors(), ia) : 0);
  }

  private static final int hashCode(final RecordComponentElement e, final boolean ia) {
    if (e == null) {
      return 0;
    } else if (e.getKind() != ElementKind.RECORD_COMPONENT) {
      return System.identityHashCode(e); // illegal argument
    }
    return
      hashCode(e.getKind().hashCode(),
               hashCode(e.getSimpleName(), ia),
               hashCode(e.getEnclosingElement(), ia),
               ia ? hashCode(e.getAnnotationMirrors(), ia) : 0);
  }

  private static final int hashCode(final TypeElement e, final boolean ia) {
    if (e == null) {
      return 0;
    }
    final ElementKind k = e.getKind();
    if (!k.isClass() && !k.isInterface()) {
      return System.identityHashCode(e); // illegal argument
    }
    return
      hashCode(k.hashCode(),
               hashCode(e.getQualifiedName(), ia),
               ia ? hashCode(e.getAnnotationMirrors(), ia) : 0);
  }

  private static final int hashCode(final TypeParameterElement e, final boolean ia) {
    if (e == null) {
      return 0;
    } else if (e.getKind() != ElementKind.TYPE_PARAMETER) {
      return System.identityHashCode(e); // illegal argument
    }
    return
      hashCode(e.getKind().hashCode(),
               hashCode(e.getGenericElement(), ia),
               hashCode(e.getSimpleName(), ia),
               ia ? hashCode(e.getAnnotationMirrors(), ia) : 0);
  }

  private static final int hashCode(final VariableElement e, final boolean ia) {
    if (e == null) {
      return 0;
    }
    final ElementKind k = e.getKind();
    if (!k.isVariable()) {
      return System.identityHashCode(e); // illegal argument
    }
    return
      hashCode(k.hashCode(),
               hashCode(e.getSimpleName(), ia),
               hashCode(e.getEnclosingElement(), ia),
               ia ? hashCode(e.getAnnotationMirrors(), ia) : 0);
  }

  private static final int hashCode(final TypeMirror t, final boolean ia) {
    // This is the entry point for all TypeMirror hashCode calculations. We have to synchronize on the completion lock
    // because a declared type's getKind() method may trigger symbol completion.
    synchronized (CompletionLock.monitor()) {
      return t == null ? 0 : switch (t.getKind()) {
      case ARRAY -> hashCode((ArrayType)t, ia);
      case DECLARED -> hashCode((DeclaredType)t, ia);
      case EXECUTABLE -> hashCode((ExecutableType)t, ia);
      case INTERSECTION -> hashCode((IntersectionType)t, ia);
      case MODULE, NONE, PACKAGE, VOID -> hashCode((NoType)t, ia);
      case NULL -> hashCode((NullType)t, ia);
      case BOOLEAN, BYTE, CHAR, DOUBLE, FLOAT, INT, LONG, SHORT -> hashCode((PrimitiveType)t, ia);
      case TYPEVAR -> hashCode((TypeVariable)t, ia);
      case WILDCARD -> hashCode((WildcardType)t, ia);
      case ERROR, OTHER, UNION -> System.identityHashCode(t); // basically illegal argument
      };
    }
  }

  private static final int hashCode(final ArrayType t, final boolean ia) {
    if (t == null) {
      return 0;
    } else if (t.getKind() != TypeKind.ARRAY) {
      return System.identityHashCode(t); // illegal argument
    }
    return
      hashCode(t.getKind().hashCode(),
               hashCode(t.getComponentType(), ia),
               ia ? hashCode(t.getAnnotationMirrors(), ia) : 0);
  }

  private static final int hashCode(final DeclaredType t, final boolean ia) {
    if (t == null) {
      return 0;
    } else if (t.getKind() != TypeKind.DECLARED) {
      return System.identityHashCode(t); // illegal argument
    }
    return
      hashCode(t.getKind().hashCode(),
               hashCode(t.asElement(), ia),
               hashCode(t.getTypeArguments(), ia),
               ia ? hashCode(t.getAnnotationMirrors(), ia) : 0);
  }

  private static final int hashCode(final ExecutableType t, final boolean ia) {
    if (t == null) {
      return 0;
    } else if (t.getKind() != TypeKind.EXECUTABLE) {
      return System.identityHashCode(t); // illegal argument
    }
    // See https://github.com/openjdk/jdk/blob/jdk-20+16/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L4194-L4202
    return
      hashCode(t.getKind().hashCode(),
               hashCode(t.getParameterTypes(), ia),
               // hashCode(t.getReceiverType(), ia), // not sure this is necessary
               hashCode(t.getReturnType(), ia),
               // hashCode(t.getTypeVariables(), ia), // not sure this is necessary
               ia ? hashCode(t.getAnnotationMirrors(), ia) : 0);
  }

  private static final int hashCode(final IntersectionType t, final boolean ia) {
    if (t == null) {
      return 0;
    } else if (t.getKind() != TypeKind.INTERSECTION) {
      return System.identityHashCode(t); // illegal argument
    }
    return
      hashCode(t.getKind().hashCode(),
               hashCode(t.getBounds(), ia),
               ia ? hashCode(t.getAnnotationMirrors(), ia) : 0);
  }

  private static final int hashCode(final NoType t, final boolean ia) {
    if (t == null) {
      return 0;
    }
    final TypeKind k = t.getKind();
    return switch (k) {
    case MODULE, PACKAGE, NONE, VOID -> k.hashCode(); // No need to check ia; a NoType cannot have annotations
    default -> System.identityHashCode(t); // illegal argument
    };
  }

  private static final int hashCode(final NullType t, final boolean ia) {
    if (t == null) {
      return 0;
    }
    final TypeKind k = t.getKind();
    // No need to check ia because a NullType cannot have annotations.
    return k == TypeKind.NULL ? k.hashCode() : System.identityHashCode(t);
  }

  private static final int hashCode(final PrimitiveType t, final boolean ia) {
    if (t == null) {
      return 0;
    }
    final TypeKind k = t.getKind();
    if (!k.isPrimitive()) {
      return System.identityHashCode(t); // illegal argument, actually
    }
    return ia ? hashCode(k.hashCode(), hashCode(t.getAnnotationMirrors(), ia)) : k.hashCode();
  }

  private static final int hashCode(final TypeVariable t, final boolean ia) {
    if (t == null) {
      return 0;
    }
    final TypeKind k = t.getKind();
    if (k != TypeKind.TYPEVAR) {
      return System.identityHashCode(t); // illegal argument
    }
    return
      hashCode(k.hashCode(),
               hashCode(t.asElement(), ia),
               hashCode(t.getUpperBound(), ia),
               hashCode(t.getLowerBound(), ia),
               ia ? hashCode(t.getAnnotationMirrors(), ia) : 0);
  }

  private static final int hashCode(final WildcardType t, final boolean ia) {
    if (t == null) {
      return 0;
    }
    final TypeKind k = t.getKind();
    if (k != TypeKind.WILDCARD) {
      return System.identityHashCode(t); // illegal argument
    }
    return
      hashCode(k.hashCode(),
               hashCode(t.getExtendsBound(), ia),
               hashCode(t.getSuperBound(), ia),
               ia ? hashCode(t.getAnnotationMirrors(), ia) : 0);
  }

  private static final int hashCode(final Directive d, final boolean ia) {
    // TODO: do we need to synchronize on the completion lock here?
    return d == null ? 0 : switch (d.getKind()) {
    case EXPORTS -> hashCode((ExportsDirective)d, ia);
    case OPENS -> hashCode((OpensDirective)d, ia);
    case PROVIDES -> hashCode((ProvidesDirective)d, ia);
    case REQUIRES -> hashCode((RequiresDirective)d, ia);
    case USES -> hashCode((UsesDirective)d, ia);
    };
  }

  private static final int hashCode(final ExportsDirective d, final boolean ia) {
    if (d == null) {
      return 0;
    }
    final DirectiveKind k = d.getKind();
    if (k != DirectiveKind.EXPORTS) {
      return System.identityHashCode(d);
    }
    return
      hashCode(k.hashCode(),
               hashCode(d.getPackage(), ia));
  }

  private static final int hashCode(final OpensDirective d, final boolean ia) {
    if (d == null) {
      return 0;
    }
    final DirectiveKind k = d.getKind();
    if (k != DirectiveKind.OPENS) {
      return System.identityHashCode(d);
    }
    return
      hashCode(k.hashCode(),
               hashCode(d.getPackage(), ia),
               hashCode(d.getTargetModules(), ia));
  }

  private static final int hashCode(final ProvidesDirective d, final boolean ia) {
    if (d == null) {
      return 0;
    }
    final DirectiveKind k = d.getKind();
    if (k != DirectiveKind.PROVIDES) {
      return System.identityHashCode(d);
    }
    return
      hashCode(k.hashCode(),
               hashCode(d.getImplementations(), ia),
               hashCode(d.getService(), ia));
  }

  private static final int hashCode(final RequiresDirective d, final boolean ia) {
    if (d == null) {
      return 0;
    }
    final DirectiveKind k = d.getKind();
    if (k != DirectiveKind.REQUIRES) {
      return System.identityHashCode(d);
    }
    return
      hashCode(k.hashCode(),
               hashCode(d.getDependency(), ia),
               d.isStatic() ? 1 : 0,
               d.isTransitive() ? 1 : 0);
  }

  private static final int hashCode(final UsesDirective d, final boolean ia) {
    if (d == null) {
      return 0;
    }
    final DirectiveKind k = d.getKind();
    if (k != DirectiveKind.USES) {
      return System.identityHashCode(d);
    }
    return
      hashCode(k.hashCode(),
               hashCode(d.getService(), ia));
  }


  /*
   * equals()
   */


  public static final boolean equalsIncludingAnnotations(final Object o1, final Object o2) {
    return equals(o1, o2, true);
  }

  public static final boolean equalsNotIncludingAnnotations(final Object o1, final Object o2) {
    return equals(o1, o2, false);
  }

  public static final boolean equals(final Object o1, final Object o2, final boolean ia) {
    return o1 == o2 || o2 != null && switch (o1) {
    case null -> false;
    case AnnotationMirror am1 -> o2 instanceof AnnotationMirror am2 && equals(am1, am2, ia);
    case AnnotationValue av1 -> o2 instanceof AnnotationValue av2 && equals(av1, av2, ia);
    case AnnotatedConstruct ac1 -> o2 instanceof AnnotatedConstruct ac2 && equals(ac1, ac2, ia);
    case CharSequence c1 -> o2 instanceof CharSequence c2 && equals(c1, c2, ia);
    case List<?> list1 -> o2 instanceof List<?> list2 && equals(list1, list2, ia);
    case Directive d1 -> o2 instanceof Directive d2 && equals(d1, d2, ia);
    default -> o1.equals(o2);
    };
  }

  private static final boolean equals(final List<?> list1, final List<?> list2, final boolean ia) {
    if (list1 == list2) {
      return true;
    } else if (list1 == null || list2 == null) {
      return false;
    }
    final int size = list1.size();
    if (size != list2.size()) {
      return false;
    }
    for (int i = 0; i < size; i++) {
      if (!equals(list1.get(i), list2.get(i), ia)) {
        return false;
      }
    }
    return true;
  }

  private static final boolean equals(final CharSequence c1, final CharSequence c2) {
    if (c1 == c2) {
      return true;
    } else if (c1 == null || c2 == null) {
      return false;
    } else if (c1 instanceof Name n1) {
      return n1.contentEquals(c2);
    } else if (c2 instanceof Name n2) {
      return n2.contentEquals(c1);
    } else {
      return c1.equals(c2);
    }
  }

  private static final boolean equals(final Name n1, final Name n2) {
    if (n1 == n2) {
      return true;
    } else if (n1 == null || n2 == null) {
      return false;
    }
    return n1.contentEquals(n2);
  }

  private static final boolean equals(final AnnotationMirror am1, final AnnotationMirror am2, final boolean ia) {
    if (am1 == am2) {
      return true;
    } else if (am1 == null || am2 == null || !equals(am1.getAnnotationType(), am2.getAnnotationType(), ia)) {
      return false;
    }
    return equals(values(am1), values(am2), ia);
  }

  private static final List<AnnotationValue> values(final AnnotationMirror am) {
    final Collection<AnnotationValue> v = toMap(am).values();
    return v instanceof List ? Collections.unmodifiableList((List<AnnotationValue>)v) : List.copyOf(v);
  }

  private static final Map<String, AnnotationValue> toMap(final AnnotationMirror am) {
    if (am == null) {
      return Map.of();
    }
    final DeclaredType at = am.getAnnotationType();
    // See https://mail.openjdk.org/pipermail/compiler-dev/2023-July/023750.html
    // assert at.getKind() == TypeKind.DECLARED : "Unexpected kind for am (" + am + "): " + at.getKind();
    // assert at.asElement().getKind() == ElementKind.ANNOTATION_TYPE : "Unexpected kind for at.asElement() (" + at.asElement() + "): " + at.asElement().getKind();
    final Map<String, AnnotationValue> map = new TreeMap<>();
    for (final Object e : at.asElement().getEnclosedElements()) {
      if (e instanceof ExecutableElement ee && ee.getKind() == ElementKind.METHOD) {
        final AnnotationValue dv = ee.getDefaultValue();
        if (dv != null) {
          map.put(((TypeElement)ee.getEnclosingElement()).getQualifiedName().toString() + '.' + ee.getSimpleName().toString(),
                  dv instanceof org.microbean.lang.element.AnnotationValue av ? av : org.microbean.lang.element.AnnotationValue.of(dv.getValue()));
        }
      }
    }
    for (final Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
      final ExecutableElement ee = entry.getKey();
      final AnnotationValue av = entry.getValue();
      map.put(((TypeElement)ee.getEnclosingElement()).getQualifiedName().toString() + '.' + ee.getSimpleName().toString(),
              av instanceof org.microbean.lang.element.AnnotationValue mav ? mav : org.microbean.lang.element.AnnotationValue.of(av.getValue()));
    }
    return Collections.unmodifiableMap(map);
  }

  private static final boolean equals(final AnnotationValue av1, final AnnotationValue av2, final boolean ia) {
    if (av1 == av2) {
      return true;
    } else if (av1 == null || av2 == null) {
      return false;
    }
    final Object v1 = av1.getValue(); // annotation elements cannot return null
    return switch (v1) {
    case AnnotationMirror am1 -> av2.getValue() instanceof AnnotationMirror am2 && equals(am1, am2, ia);
    case List<?> list1 -> av2.getValue() instanceof List<?> list2 && equals(list1, list2, ia);
    case TypeMirror t1 -> av2.getValue() instanceof TypeMirror t2 && equals(t1, t2, ia);
    case VariableElement ve1 -> av2.getValue() instanceof VariableElement ve2 && equals(ve1, ve2, ia);
    default -> v1.equals(av2.getValue()); // illegal argument
    };
  }

  @SuppressWarnings("deprecation")
  private static final boolean equals(final AnnotatedConstruct ac1, final AnnotatedConstruct ac2, final boolean ia) {
    return ac1 == ac2 || ac2 != null && switch (ac1) {
    case null -> false;
    case Element e1 -> ac2 instanceof Element e2 && equals(e1, e2, ia);
    case TypeMirror t1 -> ac2 instanceof TypeMirror t2 && equals(t1, t2, ia);
    default -> ac1.equals(ac2); // illegal argument
    };
  }

  private static final boolean equals(final Element e1, final Element e2, final boolean ia) {
    if (e1 == e2) {
      return true;
    } else if (e1 == null || e2 == null) {
      return false;
    }
    // This is the entry point for all Element equality calculations. We have to synchronize on the completion lock
    // because various elements' getKind() methods may trigger symbol completion.
    synchronized (CompletionLock.monitor()) {
      final ElementKind k = e1.getKind();
      return k == e2.getKind() && switch (k) {
      case ANNOTATION_TYPE, CLASS, ENUM, INTERFACE, RECORD -> equals((TypeElement)e1, (TypeElement)e2, ia);
      case TYPE_PARAMETER -> equals((TypeParameterElement)e1, (TypeParameterElement)e2, ia);
      case BINDING_VARIABLE, ENUM_CONSTANT, EXCEPTION_PARAMETER, FIELD, LOCAL_VARIABLE, PARAMETER, RESOURCE_VARIABLE -> equals((VariableElement)e1, (VariableElement)e2, ia);
      case RECORD_COMPONENT -> equals((RecordComponentElement)e1, (RecordComponentElement)e2, ia);
      case CONSTRUCTOR, INSTANCE_INIT, METHOD, STATIC_INIT -> equals((ExecutableElement)e1, (ExecutableElement)e2, ia);
      case PACKAGE -> equals((PackageElement)e1, (PackageElement)e2, ia);
      case MODULE -> equals((ModuleElement)e1, (ModuleElement)e2, ia);
      case OTHER -> false;
      };
    }
  }

  private static final boolean equals(final ExecutableElement e1, final ExecutableElement e2, final boolean ia) {
    if (e1 == e2) {
      return true;
    } else if (e1 == null || e2 == null || ia && !equals(e1.getAnnotationMirrors(), e2.getAnnotationMirrors(), ia)) {
      return false;
    }
    final ElementKind k1 = e1.getKind();
    // This is kind of the runtime equality contract of, say, java.lang.reflect.Method.  Note in particular that
    // TypeParameterElements are not evaluated.
    return
      k1 == e2.getKind() &&
      k1.isExecutable() &&
      equals(e1.getSimpleName(), e2.getSimpleName()) &&
      equals(e1.getParameters(), e2.getParameters(), ia) &&
      equals(e1.getReturnType(), e2.getReturnType(), ia) &&
      equals(e1.getEnclosingElement(), e2.getEnclosingElement(), ia);
  }

  private static final boolean equals(final ModuleElement e1, final ModuleElement e2, final boolean ia) {
    if (e1 == e2) {
      return true;
    } else if (e1 == null || e2 == null || ia && !equals(e1.getAnnotationMirrors(), e2.getAnnotationMirrors(), ia)) {
      return false;
    }
    return
      e1.getKind() == ElementKind.MODULE && e2.getKind() == ElementKind.MODULE &&
      equals(e1.getQualifiedName(), e2.getQualifiedName());
  }

  private static final boolean equals(final PackageElement e1, final PackageElement e2, final boolean ia) {
    if (e1 == e2) {
      return true;
    } else if (e1 == null || e2 == null || ia && !equals(e1.getAnnotationMirrors(), e2.getAnnotationMirrors(), ia)) {
      return false;
    }
    return
      e1.getKind() == ElementKind.PACKAGE && e2.getKind() == ElementKind.PACKAGE &&
      equals(e1.getQualifiedName(), e2.getQualifiedName());
  }

  private static final boolean equals(final RecordComponentElement e1, final RecordComponentElement e2, final boolean ia) {
    if (e1 == e2) {
      return true;
    } else if (e1 == null || e2 == null || ia && !equals(e1.getAnnotationMirrors(), e2.getAnnotationMirrors(), ia)) {
      return false;
    }
    return
      e1.getKind() == ElementKind.RECORD_COMPONENT && e2.getKind() == ElementKind.RECORD_COMPONENT &&
      equals(e1.getSimpleName(), e2.getSimpleName()) &&
      equals(e1.getEnclosingElement(), e2.getEnclosingElement(), ia);
  }

  private static final boolean equals(final TypeElement e1, final TypeElement e2, final boolean ia) {
    if (e1 == e2) {
      return true;
    } else if (e1 == null || e2 == null || ia && !equals(e1.getAnnotationMirrors(), e2.getAnnotationMirrors(), ia)) {
      return false;
    }
    final ElementKind k1 = e1.getKind();
    return
      k1 == e2.getKind() &&
      (k1.isClass() || k1.isInterface()) &&
      equals(e1.getQualifiedName(), e2.getQualifiedName());
  }

  private static final boolean equals(final TypeParameterElement e1, final TypeParameterElement e2, final boolean ia) {
    if (e1 == e2) {
      return true;
    } else if (e1 == null || e2 == null || ia && !equals(e1.getAnnotationMirrors(), e2.getAnnotationMirrors(), ia)) {
      return false;
    }
    // This is also the equality contract of sun.reflect.generics.reflectiveObjects.TypeVariableImpl, interestingly.
    return
      e1.getKind() == ElementKind.TYPE_PARAMETER && e2.getKind() == ElementKind.TYPE_PARAMETER &&
      equals(e1.getSimpleName(), e2.getSimpleName()) &&
      equals(e1.getGenericElement(), e2.getGenericElement(), ia);
  }

  private static final boolean equals(final VariableElement e1, final VariableElement e2, final boolean ia) {
    if (e1 == e2) {
      return true;
    } else if (e1 == null || e2 == null || ia && !equals(e1.getAnnotationMirrors(), e2.getAnnotationMirrors(), ia)) {
      return false;
    }
    final ElementKind k1 = e1.getKind();
    return
      k1 == e2.getKind() &&
      k1.isVariable() &&
      equals(e1.getSimpleName(), e2.getSimpleName()) &&
      equals(e1.getEnclosingElement(), e2.getEnclosingElement(), ia);
  }

  private static final boolean equals(final TypeMirror t1, final TypeMirror t2, final boolean ia) {
    if (t1 == t2) {
      return true;
    } else if (t1 == null || t2 == null || ia && !equals(t1.getAnnotationMirrors(), t2.getAnnotationMirrors(), ia)) {
      return false;
    }
    // This is the entry point for all TypeMirror equality calculations. We have to synchronize on the completion lock
    // because a declared type's getKind() method may trigger symbol completion.
    synchronized (CompletionLock.monitor()) {
      final TypeKind k = t1.getKind();
      return k == t2.getKind() && switch (k) {
      case ARRAY -> equals((ArrayType)t1, (ArrayType)t2, ia);
      case DECLARED -> equals((DeclaredType)t1, (DeclaredType)t2, ia);
      case EXECUTABLE -> equals((ExecutableType)t1, (ExecutableType)t2, ia);
      case INTERSECTION -> equals((IntersectionType)t1, (IntersectionType)t2, ia);
      case MODULE, NONE, PACKAGE, VOID -> equals((NoType)t1, (NoType)t2, ia);
      case NULL -> equals((NullType)t1, (NullType)t2, ia);
      case BOOLEAN, BYTE, CHAR, DOUBLE, FLOAT, INT, LONG, SHORT -> equals((PrimitiveType)t1, (PrimitiveType)t2, ia);
      case TYPEVAR -> equals((TypeVariable)t1, (TypeVariable)t2, ia);
      case WILDCARD -> equals((WildcardType)t1, (WildcardType)t2, ia);
      case ERROR, OTHER, UNION -> t1.equals(t2); // unhandled argument
      };
    }
  }

  private static final boolean equals(final ArrayType t1, final ArrayType t2, final boolean ia) {
    if (t1 == t2) {
      return true;
    } else if (t1 == null || t2 == null || ia && !equals(t1.getAnnotationMirrors(), t2.getAnnotationMirrors(), ia)) {
      return false;
    }
    return
      t1.getKind() == TypeKind.ARRAY && t2.getKind() == TypeKind.ARRAY &&
      equals(t1.getComponentType(), t2.getComponentType(), ia);
  }

  private static final boolean equals(final DeclaredType t1, final DeclaredType t2, final boolean ia) {
    if (t1 == t2) {
      return true;
    } else if (t1 == null || t2 == null || ia && !equals(t1.getAnnotationMirrors(), t2.getAnnotationMirrors(), ia)) {
      return false;
    }
    return
      t1.getKind() == TypeKind.DECLARED && t2.getKind() == TypeKind.DECLARED &&
      equals(t1.asElement(), t2.asElement(), ia) &&
      equals(t1.getTypeArguments(), t2.getTypeArguments(), ia);
  }

  private static final boolean equals(final ExecutableType t1, final ExecutableType t2, final boolean ia) {
    if (t1 == t2) {
      return true;
    } else if (t1 == null || t2 == null || ia && !equals(t1.getAnnotationMirrors(), t2.getAnnotationMirrors(), ia)) {
      return false;
    }
    return
      t1.getKind() == TypeKind.EXECUTABLE && t2.getKind() == TypeKind.EXECUTABLE &&
      equals(t1.getParameterTypes(), t2.getParameterTypes(), ia) &&
      equals(t1.getReceiverType(), t2.getReceiverType(), ia) &&
      equals(t1.getReturnType(), t2.getReturnType(), ia) &&
      // no thrown types
      equals(t1.getTypeVariables(), t2.getTypeVariables(), ia); // not super sure this is necessary
  }

  private static final boolean equals(final IntersectionType t1, final IntersectionType t2, final boolean ia) {
    if (t1 == t2) {
      return true;
    } else if (t1 == null || t2 == null || ia && !equals(t1.getAnnotationMirrors(), t2.getAnnotationMirrors(), ia)) {
      return false;
    }
    return
      t1.getKind() == TypeKind.INTERSECTION && t2.getKind() == TypeKind.INTERSECTION &&
      equals(t1.getBounds(), t2.getBounds(), ia);
  }

  private static final boolean equals(final NoType t1, final NoType t2, final boolean ia) {
    if (t1 == t2) {
      return true;
    } else if (t1 == null || t2 == null || ia && !equals(t1.getAnnotationMirrors(), t2.getAnnotationMirrors(), ia)) {
      return false;
    }
    final TypeKind k = t1.getKind();
    return switch (k) {
    case MODULE, PACKAGE, NONE, VOID -> k == t2.getKind();
    default -> false;
    };
  }

  private static final boolean equals(final NullType t1, final NullType t2, final boolean ia) {
    if (t1 == t2) {
      return true;
    } else if (t1 == null || t2 == null) {
      return false;
    }
    return t1.getKind() == TypeKind.NULL && t2.getKind() == TypeKind.NULL;
  }

  private static final boolean equals(final PrimitiveType t1, final PrimitiveType t2, final boolean ia) {
    if (t1 == t2) {
      return true;
    } else if (t1 == null || t2 == null || ia && !equals(t1.getAnnotationMirrors(), t2.getAnnotationMirrors(), ia)) {
      return false;
    }
    final TypeKind k = t1.getKind();
    return k.isPrimitive() && k == t2.getKind();
  }

  private static final boolean equals(final TypeVariable t1, final TypeVariable t2, final boolean ia) {
    if (t1 == t2) {
      return true;
    } else if (t1 == null || t2 == null || ia && !equals(t1.getAnnotationMirrors(), t2.getAnnotationMirrors(), ia)) {
      return false;
    }
    return
      t1.getKind() == TypeKind.TYPEVAR && t2.getKind() == TypeKind.TYPEVAR &&
      equals(t1.asElement(), t2.asElement(), ia) &&
      equals(t1.getUpperBound(), t2.getUpperBound(), ia) &&
      equals(t2.getLowerBound(), t2.getLowerBound(), ia);
  }

  private static final boolean equals(final WildcardType t1, final WildcardType t2, final boolean ia) {
    if (t1 == t2) {
      return true;
    } else if (t1 == null || t2 == null) {
      return false;
    }
    // The Java type system doesn't actually say that a wildcard type is a type.  It also says that "?" is "equivalent
    // to" "? extends Object".  Let's start by simply comparing bounds exactly.
    return
      t1.getKind() == TypeKind.WILDCARD && t2.getKind() == TypeKind.WILDCARD &&
      equals(t1.getExtendsBound(), t2.getExtendsBound(), ia) &&
      equals(t1.getSuperBound(), t2.getSuperBound(), ia);
  }

  private static final boolean equals(final Directive d1, final Directive d2, final boolean ia) {
    final DirectiveKind k = d1.getKind();
    if (d2.getKind() != k) {
      return false;
    }
    return k == d2.getKind() && switch (k) {
    case EXPORTS -> equals((ExportsDirective)d1, (ExportsDirective)d2, ia);
    case OPENS -> equals((OpensDirective)d1, (OpensDirective)d2, ia);
    case PROVIDES -> equals((ProvidesDirective)d1, (ProvidesDirective)d2, ia);
    case REQUIRES -> equals((RequiresDirective)d1, (RequiresDirective)d2, ia);
    case USES -> equals((UsesDirective)d1, (UsesDirective)d2, ia);
    };
  }

  private static final boolean equals(final ExportsDirective d1, final ExportsDirective d2, final boolean ia) {
    if (d1 == d2) {
      return true;
    } else if (d1 == null || d2 == null) {
      return false;
    }
    return
      d1.getKind() == DirectiveKind.EXPORTS && d2.getKind() == DirectiveKind.EXPORTS &&
      equals(d1.getPackage(), d2.getPackage(), ia) &&
      equals(d1.getTargetModules(), d2.getTargetModules(), ia);
  }

  private static final boolean equals(final OpensDirective d1, final OpensDirective d2, final boolean ia) {
    if (d1 == d2) {
      return true;
    } else if (d1 == null || d2 == null) {
      return false;
    }
    return
      d1.getKind() == DirectiveKind.OPENS && d2.getKind() == DirectiveKind.OPENS &&
      equals(d1.getPackage(), d2.getPackage(), ia) &&
      equals(d1.getTargetModules(), d2.getTargetModules(), ia);
  }

  private static final boolean equals(final ProvidesDirective d1, final ProvidesDirective d2, final boolean ia) {
    if (d1 == d2) {
      return true;
    } else if (d1 == null || d2 == null) {
      return false;
    }
    return
      d1.getKind() == DirectiveKind.PROVIDES && d2.getKind() == DirectiveKind.PROVIDES &&
      equals(d1.getImplementations(), d2.getImplementations(), ia) &&
      equals(d1.getService(), d2.getService(), ia);
  }

  private static final boolean equals(final RequiresDirective d1, final RequiresDirective d2, final boolean ia) {
    if (d1 == d2) {
      return true;
    } else if (d1 == null || d2 == null) {
      return false;
    }
    return
      d1.getKind() == DirectiveKind.REQUIRES && d2.getKind() == DirectiveKind.REQUIRES &&
      equals(d1.getDependency(), d2.getDependency(), ia) &&
      d1.isStatic() && d2.isStatic() &&
      d1.isTransitive() && d2.isTransitive();
  }

  private static final boolean equals(final UsesDirective d1, final UsesDirective d2, final boolean ia) {
    if (d1 == d2) {
      return true;
    } else if (d1 == null || d2 == null) {
      return false;
    }
    return
      d1.getKind() == DirectiveKind.USES && d2.getKind() == DirectiveKind.USES &&
      equals(d1.getService(), d2.getService(), ia);
  }

}
