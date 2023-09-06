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
package org.microbean.lang.element;

import java.util.stream.IntStream;

import org.microbean.lang.CompletionLock;

public final class Name implements javax.lang.model.element.Name {

  private static final Name EMPTY = new Name("");

  private final String content;

  private Name() {
    super();
    this.content = "";
  }

  private Name(final String s) {
    super();
    this.content = s == null ? "" : s;
  }

  private Name(final javax.lang.model.element.Name name) {
    super();
    this.content = switch (name) {
    case null -> "";
    case Name n -> n.content;
    default -> {
      final String s;
      CompletionLock.acquire();
      try {
        s = name.toString();
      } finally {
        CompletionLock.release();
      }
      yield s == null ? "" : s;
    }};
  }

  private Name(final CharSequence cs) {
    super();
    this.content = switch (cs) {
    case null -> "";
    case Name n -> n.content;
    case javax.lang.model.element.Name n -> {
      final String s;
      CompletionLock.acquire();
      try {
        s = n.toString();
      } finally {
        CompletionLock.release();
      }
      yield s == null ? "" : s;
    }
    default -> {
      final String s = cs.toString();
      yield s == null ? "" : s;
    }
    };
  }

  private Name(final Name n) {
    super();
    this.content = n == null ? "" : n.content;
  }

  @Override // CharSequence
  public final char charAt(final int index) {
    return this.content.charAt(index);
  }

  @Override // CharSequence
  public final IntStream chars() {
    return this.content.chars();
  }

  @Override // CharSequence
  public final IntStream codePoints() {
    return this.content.codePoints();
  }

  @Override // CharSequence
  public final int length() {
    return this.content.length();
  }

  @Override // CharSequence
  public final Name subSequence(final int start, final int end) {
    return of(this.content.subSequence(start, end));
  }

  @Override // Name
  public final boolean contentEquals(final CharSequence cs) {
    return switch (cs) {
    case null -> false;
    case Name n -> this.content.equals(n.content);
    case javax.lang.model.element.Name n -> {
      CompletionLock.acquire();
      try {
        yield this.content.equals(n.toString());
      } finally {
        CompletionLock.release();
      }
    }
    default -> this.content.equals(cs.toString());
    };
  }

  @Override // Object
  public final int hashCode() {
    return this.content.hashCode();
  }

  @Override // Object
  public final boolean equals(final Object other) {
    if (this == other) {
      return true;
    } else if (other != null && this.getClass() == other.getClass()) {
      return this.content.equals(((Name)other).content);
    } else {
      return false;
    }
  }

  @Override // CharSequence
  public final String toString() {
    return this.content;
  }


  /*
   * Static methods.
   */


  public static final Name of() {
    return EMPTY;
  }

  public static final Name of(final CharSequence cs) {
    return switch (cs) {
    case null -> EMPTY;
    case Name n -> n;
    case javax.lang.model.element.Name n -> {
      CompletionLock.acquire();
      try {
        yield n.length() <= 0 ? EMPTY : new Name(n);
      } finally {
        CompletionLock.release();
      }
    }
    default -> cs.length() <= 0 ? EMPTY : new Name(cs);
    };
  }

  public static final Name of(final Name n) {
    return n == null ? EMPTY : n;
  }

  public static final Name of(final String s) {
    return s == null || s.isEmpty() ? EMPTY : new Name(s);
  }

  public static final Name ofSimple(final CharSequence cs) {
    if (cs == null) {
      return EMPTY;
    }
    String s;
    if (cs instanceof javax.lang.model.element.Name) {
      CompletionLock.acquire();
      try {
        s = cs.toString();
      } finally {
        CompletionLock.release();
      }
    } else {
      s = cs.toString();
    }
    final int lastDotIndex = s.lastIndexOf('.');
    if (lastDotIndex > 0 && s.length() > 2) {
      return of(s.substring(lastDotIndex + 1, s.length()));
    }
    return of(s);
  }

}
