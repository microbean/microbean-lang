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
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
            p -> (p.getNameCount() < 3 || p.getName(2).toString().startsWith("java")) && p.getFileName().toString().endsWith(".class"));
  }
  
  @BeforeEach
  final void setup() {
    this.jandex = new Jandex(jdk);
  }

  @Test
  final void testString() {
    assertNotNull(jdk.getClassByName("java.lang.String"));
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
  final void testDocumented() {
    final Element e = jandex.element(jdk.getClassByName("java.lang.annotation.Documented"));
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
  
}
