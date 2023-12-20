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
package org.microbean.lang.jandex;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import java.net.URI;
import java.net.URISyntaxException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Collection;

import java.util.function.Predicate;

import java.util.stream.Stream;

import javax.lang.model.element.Element;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.EmptyTypeTarget;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.TypeParameterTypeTarget;
import org.jboss.jandex.TypeTarget;
import org.jboss.jandex.TypeVariable;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

final class TestJandex {

  private static IndexView jdk;

  private Jandex jandex;

  private TestJandex() {
    super();
  }

  @BeforeAll
  static final void indexJdk() {
    // See https://stackoverflow.com/a/46451977/208288
    jdk =
      index(Path.of(URI.create("jrt:/")),
            p -> {
              // System.out.println("*** p: " + p);
              return p.getFileName().toString().endsWith(".class");
            });
    // p -> (p.getNameCount() < 3 || p.getName(2).toString().startsWith("java")) && p.getFileName().toString().endsWith(".class"));
  }

  @BeforeEach
  final void setup() {
    this.jandex = new Jandex(jdk);
  }

  @Test
  final void testClassInfoIdentity() {
    final ClassInfo ci = jdk.getClassByName("java.lang.String");
    assertNotNull(ci);
    assertSame(ci, jdk.getClassByName("java.lang.String"));
    DotName javaLangString = DotName.createComponentized(null, "java");
    javaLangString = DotName.createComponentized(javaLangString, "lang");
    javaLangString = DotName.createComponentized(javaLangString, "String");
    assertEquals("java.lang.String", javaLangString.toString());
    assertSame(ci, jdk.getClassByName(javaLangString));
  }

  @Test
  final void testMethodInfoNonIdentity() {
    final MethodInfo mi = jdk.getClassByName("java.lang.String").method("charAt", PrimitiveType.INT);
    assertNotNull(mi);
    assertEquals(mi, jdk.getClassByName("java.lang.String").method("charAt", PrimitiveType.INT));
    assertNotSame(mi, jdk.getClassByName("java.lang.String").method("charAt", PrimitiveType.INT));
  }

  @Test
  final void testPackageStuff() throws IOException, URISyntaxException {
    final IndexView i = index(Path.of(org.microbean.lang.jandex.testjandex.Frobnicator.class.getProtectionDomain().getCodeSource().getLocation().toURI()),
                              p -> p.endsWith(".class"));
    final Collection<AnnotationInstance> as = i.getAnnotations(org.microbean.lang.jandex.testjandex.Flabrous.class);
    // Note that Jandex does not allow you to get package annotations. There is no such thing as PackageInfo. This is
    // true even for inherited annotations.
    assertEquals(0, as.size());
  }

  @Test
  final void testEnum() {
    // Nice class to test "circular" type variables.
    final Element e = jandex.element(jdk.getClassByName("java.lang.Enum"));
  }

  @Test
  final void testObject() {
    jandex.element(jdk.getClassByName("java.lang.Object"));
  }

  @Test
  final void testClass() {
    jandex.element(jdk.getClassByName("java.lang.Class"));
  }

  @Test
  final void testDocumented() {
    final Element e = jandex.element(jdk.getClassByName("java.lang.annotation.Documented"));
  }

  @Test
  final void testTypeParameterAnnotations() throws IOException {
    final Indexer indexer = new Indexer();
    indexer.indexClass(Flob.class);
    final IndexView i = indexer.complete();
    final ClassInfo ci = i.getClassByName(Flob.class.getName());

    // The class element itself has no declared annotations.
    assertEquals(0, ci.declaredAnnotations().size());

    // The only way to get to the annotations declared on its sole type parameter element is using this horrible
    // mechanism.  annotations() is documented to return "the annotation instances declared on this annotation target
    // and nested annotation targets".  This doesn't include nested classes, just fields, constructors and methods, I
    // guess.

    // 3 is:
    // * 1 @Borf annotation on T, the type parameter declared by Flob
    // * 1 @Borf annotation on yeet()
    // * 1 @Borf type-use annotation on yeet()'s return value, a String
    // * 0 @Borf annotations on Bozo because it isn't seen
    assertEquals(3, ci.annotations().size(), "ci.annotations(): " + ci.annotations());

    // As you can see, Jandex gets confused about what a type parameter is versus what a type variable is.
    for (final AnnotationInstance ai : ci.annotations()) {
      final AnnotationTarget target = ai.target();
      switch (target.kind()) {
      case TYPE:
        final TypeTarget tt = target.asType();
        switch (tt.usage()) {
        case EMPTY:
          final EmptyTypeTarget ett = tt.asEmpty();
          assertFalse(ett.isReceiver());
          assertEquals("java.lang.String", ett.target().name().toString());
          assertEquals("yeet", ett.enclosingTarget().asMethod().name());
          break;
        case TYPE_PARAMETER:
          // expected
          break;
        default:
          fail();
        }
        break;
      case METHOD:
        break;
      default:
        fail();
      }
    }

  }

  @Test
  final void testEnclosingElementOfLocalClassInsideMethod() throws ClassNotFoundException, IOException {
    final Indexer indexer = new Indexer();
    indexer.indexClass(Flob.class);
    final IndexView i = indexer.complete();
    final ClassInfo ci = i.getClassByName(Flob.class.getName());

    // Flob declares a method, yeet(), that declares a local class. You can't "get to" the local class at all from
    // Jandex. See https://github.com/smallrye/jandex/issues/180#issue-1179577350.
    //
    // This turns out to be OK, because the javax.lang.model.* hierarchy also doesn't let you "get to" local or
    // anonymous classes in any way.

    assertEquals(2, ci.memberClasses().size());

    // You can't get it from the index…
    assertNull(i.getClassByName(Flob.class.getName() + "$1Bozo"));

    // …but it does exist under that name:
    assertNotNull(Class.forName(Flob.class.getName() + "$1Bozo"));

    for (final AnnotationInstance a : ci.annotations()) {
      final AnnotationTarget target = a.target();
      switch (target.kind()) {
      case METHOD:
        assertEquals("yeet", target.asMethod().name());
        break;
      case TYPE:
        // This is where things get stupid.
        final TypeTarget tt = target.asType();
        switch (tt.usage()) {
        case EMPTY:
          final EmptyTypeTarget ett = tt.asEmpty();
          assertFalse(ett.isReceiver());
          assertEquals("java.lang.String", ett.target().name().toString());
          assertEquals("yeet", ett.enclosingTarget().asMethod().name());
          break;
        case TYPE_PARAMETER:
          // Stupid. Actually an *element* annotation.
          assertEquals(0, tt.asTypeParameter().position()); // T in Flob<@Borf T>
          break;
        default:
          fail();
        }
        break;
      default:
        fail();
      }
    }
  }

  private static final IndexView index(final Path indexRoot,
                                       final Predicate<? super Path> filterPredicate) {
    final Indexer indexer = new Indexer();
    try (final Stream<Path> pathStream = Files.walk(indexRoot, Integer.MAX_VALUE)) {
      pathStream
        .filter(filterPredicate)
        .forEach(p -> {
            try (final InputStream inputStream = Files.newInputStream(p)) {
              indexer.index(inputStream);
            } catch (final IOException ioException) {
              throw new UncheckedIOException(ioException.getMessage(), ioException);
            }
          });
    } catch (final IOException e) {
      throw new UncheckedIOException(e.getMessage(), e);
    }
    return indexer.complete();
  }

  private static interface Grop {

    void flop(Grop this);

  }

  private static final class Flob<@Borf T> {

    @Borf
    private static final String yeet() {
      // (Jandex will see nothing in here.)
      @Borf
      class Bozo {};
      return "Yeet";
    }

    @Borf
    private final class Blotz {}

    @Borf
    private static final class Greep {}

  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.TYPE_USE, ElementType.TYPE_PARAMETER, ElementType.METHOD })
  private @interface Borf {}

}
