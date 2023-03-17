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
import java.io.UncheckedIOException;

import java.net.URI;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;

import java.util.function.Predicate;

import java.util.stream.Stream;

import javax.lang.model.element.TypeElement;

import org.jboss.jandex.Indexer;
import org.jboss.jandex.IndexView;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class TestObject {

  private static IndexView jdk;
  
  private TestObject() {
    super();
  }

  @BeforeAll
  static final void indexJdk() {
    // See https://stackoverflow.com/a/46451977/208288
    jdk = index(Path.of(URI.create("jrt:/")), p -> p.getFileName().toString().endsWith(".class"));
  }
  
  @Test
  final void testObject() {
    final TypeElement e = new Jandex(this.jdk).typeElement("java.lang.Object");
    final List<?> es = e.getEnclosedElements();
    // es.forEach(System.out::println);
  }

  @Test
  final void testString() {
    final TypeElement e = new Jandex(this.jdk).typeElement("java.lang.String");
    final List<?> es = e.getEnclosedElements();
    es.forEach(ee -> { System.out.println(ee + "; class: " + ee.getClass()); });
  }


  /*
   * Static methods.
   */

  
  private static final IndexView index(final Path indexRoot, final Predicate<? super Path> filterPredicate) {
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
