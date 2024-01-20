/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2022–2024 microBean™.
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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A utility class logically containing a single {@link Lock} that is used to guard against concurrent symbol
 * completion.
 *
 * @author <a href="https://about.me/lairdnelson/" target="_top">Laird Nelson</a>
 *
 * @see #acquire()
 *
 * @see #release()
 */
public final class CompletionLock {

  private static final long serialVersionUID = 1L;

  private static final Lock LOCK = new ReentrantLock();

  private CompletionLock() {
    super();
  }

  /**
   * Calls {@link Lock#lock() lock()} on the global {@link Lock} and returns it.
   *
   * @return the locked {@link Lock}; never {@code null}
   *
   * @see #release()
   */
  public static final Lock acquire() {
    LOCK.lock();
    return LOCK;
  }

  /**
   * Calls {@link Lock#unlock() unlock()} on the global {@link Lock} and returns it.
   *
   * @return the unlocked {@link Lock}; never {@code null}
   *
   * @see #acquire()
   */
  public static final Lock release() {
    LOCK.unlock();
    return LOCK;
  }

}
