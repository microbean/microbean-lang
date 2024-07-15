/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2024 microBean™.
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

import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;

import java.util.Objects;
import java.util.Optional;

import javax.lang.model.type.TypeMirror;

import org.microbean.constant.Constables;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;

import static org.microbean.lang.ConstantDescs.CD_SameTypeEquality;
import static org.microbean.lang.ConstantDescs.CD_TypeAndElementSource;

public final class SameTypeEquality extends Equality {


  /*
   * Instance fields.
   */

  
  private final TypeAndElementSource tes;


  /*
   * Constructors.
   */

  
  public SameTypeEquality(final TypeAndElementSource tes) {
    super(false);
    this.tes = Objects.requireNonNull(tes, "tes");
  }


  /*
   * Instance methods.
   */

  
  @Override // Equality (Constable)
  public final Optional<DynamicConstantDesc<? extends Equality>> describeConstable() {
    return Constables.describeConstable(this.tes)
      .map(tesDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                             MethodHandleDesc.ofConstructor(CD_SameTypeEquality,
                                                                            CD_TypeAndElementSource),
                                             tesDesc));
  }

  @Override // Equality
  public final boolean equals(final Object o1, final Object o2) {
    if (o1 == o2) {
      return true;
    } else if (o1 == null || o2 == null) {
      return false;
    } else if (o1 instanceof TypeMirror t1) {
      return o2 instanceof TypeMirror t2 && tes.sameType(t1, t2);
    } else if (o2 instanceof TypeMirror) {
      return false;
    } else {
      return super.equals(o1, o2);
    }
  }

}
