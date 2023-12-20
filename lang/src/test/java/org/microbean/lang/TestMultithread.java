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

import java.util.ArrayDeque;
import java.util.Deque;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TestMultithread {

  private TestMultithread() {
    super();
  }

  @Test
  final void testMultithread() {
    final Deque<String> dq = new ArrayDeque<>();
    frob("java.lang.invoke.TypeDescriptor$OfField<java.lang.constant.ConstantDesc>", dq);
    assertEquals(2, dq.size());
    assertEquals("java.lang.constant.ConstantDesc", dq.pop());
    assertEquals("java.lang.invoke.TypeDescriptor$OfField", dq.pop());
  }

  private static void frob(final String s, final Deque<String> stack) {
    final StringBuilder sb = new StringBuilder();
    for (char c : s.toCharArray()) {
      switch (c) {
      case '<':
        stack.push(sb.toString());
        sb.setLength(0);
        break;
      case '>':
        stack.push(sb.toString());
        sb.setLength(0);
        break;
      default:
        sb.append(c);
        break;
      }
    }
  }

}
