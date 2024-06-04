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

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * A utility class logically containing a single {@link Lock} that is used to guard against concurrent <em>symbol
 * completion</em>.
 *
 * <p>The {@code javax.lang.model.*} classes make no guarantees about thread safety. Certain operations on {@link
 * javax.lang.model.type.TypeMirror} and {@link javax.lang.model.element.Element}, particularly {@link
 * javax.lang.model.type.TypeMirror#getKind()} and {@link javax.lang.model.element.Element#getKind()} operations, will
 * result in symbol completion, which may involve modification of the {@link javax.lang.model.type.TypeMirror} or {@link
 * javax.lang.model.element.Element} in question. It is imperative that this modification be performed under global lock
 * if thread safety is to be preserved, and that is the function of this class.</p>
 *
 * <p>Note that {@link org.microbean.lang.type.DelegatingTypeMirror} and {@link
 * org.microbean.lang.element.DelegatingElement} automatically perform such locking, and all operations in {@link Lang}
 * that could result in symbol completion also perform such locking.</p>
 *
 * @author <a href="https://about.me/lairdnelson/" target="_top">Laird Nelson</a>
 *
 * @see #acquire()
 *
 * @see #release()
 */
public final class CompletionLock {


  /*
   * Static fields.
   */


  private static final long serialVersionUID = 1L;

  private static final Lock LOCK = new ReentrantLock();


  /*
   * Constructors.
   */


  private CompletionLock() {
    super();
  }


  /*
   * Static methods.
   */


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

  /**
   * Calls {@link #acquire()}, then {@link Supplier#get() get()} on the supplied {@link Supplier}, then {@link
   * #release()} in a {@code finally} block, and returns the result of the {@link Supplier#get() get()} invocation.
   *
   * @param <T> the type of the object this method will return
   *
   * @param s a {@link Supplier}; must not be {@code null}
   *
   * @return the result of an invocation of the supplied {@link Supplier}'s {@link Supplier#get() get()} method, which
   * may be {@code null}
   *
   * @exception NullPointerException if {@code s} is {@code null}
   */
  public static final <T> T guard(final Supplier<? extends T> s) {
    acquire();
    try {
      return s.get();
    } finally {
      release();
    }
  }

  /**
   * Calls {@link #acquire()}, then {@link Supplier#get() get()} on the supplied {@link BooleanSupplier}, then {@link
   * #release()} in a {@code finally} block, and returns the result of the {@link BooleanSupplier#getAsBoolean()
   * getAsBoolean()} invocation.
   *
   * @param s a {@link BooleanSupplier}; must not be {@code null}
   *
   * @return the result of an invocation of the supplied {@link BooleanSupplier}'s {@link BooleanSupplier#getAsBoolean()
   * getAsBoolean()} method
   *
   * @exception NullPointerException if {@code s} is {@code null}
   */
  public static final boolean guard(final BooleanSupplier s) {
    acquire();
    try {
      return s.getAsBoolean();
    } finally {
      release();
    }
  }

  /**
   * Calls {@link #acquire()}, then {@link Runnable#run() run()} on the supplied {@link Supplier}, then {@link
   * #release()} in a {@code finally} block.
   *
   * @param r a {@link Runnable}; must not be {@code null}
   *
   * @exception NullPointerException if {@code r} is {@code null}
   */
  public static final void guard(final Runnable r) {
    acquire();
    try {
      r.run();
    } finally {
      release();
    }
  }

}
