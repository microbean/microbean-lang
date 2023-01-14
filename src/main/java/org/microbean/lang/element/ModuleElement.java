/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2022 microBean™.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.NoType;

public final class ModuleElement extends org.microbean.lang.element.Element implements javax.lang.model.element.ModuleElement {


  /*
   * Instance fields.
   */


  private final boolean open;

  private final List<Directive> directives;

  private final List<Directive> unmodifiableDirectives;


  /*
   * Constructors.
   */


  public ModuleElement() {
    this(false);
  }
  
  public ModuleElement(final boolean open) {
    super(ElementKind.MODULE);
    this.open = open;
    this.directives = new ArrayList<>();
    this.unmodifiableDirectives = Collections.unmodifiableList(this.directives);
  }


  /*
   * Instance methods.
   */


  @Override // Element
  public final <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    return v.visitModule(this, p);
  }

  @Override // ModuleElement
  public final boolean isOpen() {
    return this.open;
  }

  @Override // ModuleElement
  public final List<? extends Directive> getDirectives() {
    return this.unmodifiableDirectives;
  }

  public final void addDirective(final Directive directive) {
    this.directives.add(validateDirective(directive));
  }

  @Override // ModuleElement
  public final Element getEnclosingElement() {
    return null;
  }

  @Override // ModuleElement
  public final void setEnclosingElement(final Element e) {
    throw new UnsupportedOperationException();
  }

  @Override // QualifiedNameable
  public final javax.lang.model.element.Name getQualifiedName() {
    return this.getSimpleName();
  }


  /*
   * Static methods.
   */

  
  private static final Directive validateDirective(final Directive directive) {
    return Objects.requireNonNull(directive, "directive");
  }


  /*
   * Inner and nested classes.
   */


  public static sealed class Directive
    implements javax.lang.model.element.ModuleElement.Directive
    permits ExportsDirective, OpensDirective, ProvidesDirective, RequiresDirective, UsesDirective {

    private final DirectiveKind kind;

    protected Directive(final DirectiveKind kind) {
      super();
      this.kind = Objects.requireNonNull(kind, "kind");
    }

    @Override // Directive
    public <R, P> R accept(final DirectiveVisitor<R, P> v, final P p) {
      switch (this.getKind()) {
      case EXPORTS:
        return v.visitExports((ExportsDirective)this, p);
      case OPENS:
        return v.visitOpens((OpensDirective)this, p);
      case PROVIDES:
        return v.visitProvides((ProvidesDirective)this, p);
      case REQUIRES:
        return v.visitRequires((RequiresDirective)this, p);
      case USES:
        return v.visitUses((UsesDirective)this, p);
      default:
        return v.visitUnknown(this, p);
      }
    }

    @Override // Directive
    public final DirectiveKind getKind() {
      return this.kind;
    }

  }

  public static final class ExportsDirective extends Directive implements javax.lang.model.element.ModuleElement.ExportsDirective {

    private final PackageElement pkg;

    private final List<javax.lang.model.element.ModuleElement> targetModules;

    public ExportsDirective(final PackageElement pkg, final List<? extends javax.lang.model.element.ModuleElement> targetModules) {
      super(DirectiveKind.EXPORTS);
      this.pkg = Objects.requireNonNull(pkg, "pkg");
      this.targetModules = targetModules == null || targetModules.isEmpty() ? List.of() : List.copyOf(targetModules);
    }

    @Override // Directive
    public final <R, P> R accept(final DirectiveVisitor<R, P> v, final P p) {
      return v.visitExports(this, p);
    }

    @Override // ExportsDirective
    public final PackageElement getPackage() {
      return this.pkg;
    }

    @Override // ExportsDirective
    public final List<? extends javax.lang.model.element.ModuleElement> getTargetModules() {
      return this.targetModules;
    }

  }

  public static final class OpensDirective extends Directive implements javax.lang.model.element.ModuleElement.OpensDirective {

    private final PackageElement pkg;

    private final List<javax.lang.model.element.ModuleElement> targetModules;

    public OpensDirective(final PackageElement pkg, final List<? extends javax.lang.model.element.ModuleElement> targetModules) {
      super(DirectiveKind.OPENS);
      this.pkg = Objects.requireNonNull(pkg, "pkg");
      this.targetModules = targetModules == null || targetModules.isEmpty() ? List.of() : List.copyOf(targetModules);
    }

    @Override // Directive
    public final <R, P> R accept(final DirectiveVisitor<R, P> v, final P p) {
      return v.visitOpens(this, p);
    }

    @Override // OpensDirective
    public final PackageElement getPackage() {
      return this.pkg;
    }

    @Override // OpensDirective
    public final List<? extends javax.lang.model.element.ModuleElement> getTargetModules() {
      return this.targetModules;
    }

  }

  public final class ProvidesDirective extends Directive implements javax.lang.model.element.ModuleElement.ProvidesDirective {

    private final TypeElement service;

    private final List<TypeElement> implementations;

    public ProvidesDirective(final TypeElement service, final List<? extends TypeElement> implementations) {
      super(DirectiveKind.PROVIDES);
      this.service = Objects.requireNonNull(service, "service");
      this.implementations = implementations == null || implementations.isEmpty() ? List.of() : List.copyOf(implementations);
    }

    @Override // Directive
    public final <R, P> R accept(final DirectiveVisitor<R, P> v, final P p) {
      return v.visitProvides(this, p);
    }

    @Override // ProvidesDirective
    public final TypeElement getService() {
      return this.service;
    }

    @Override // ProvidesDirective
    public final List<? extends TypeElement> getImplementations() {
      return this.implementations;
    }

  }

  public final class RequiresDirective extends Directive implements javax.lang.model.element.ModuleElement.RequiresDirective {

    private final javax.lang.model.element.ModuleElement dependency;

    private final boolean isStatic;

    private final boolean transitive;

    public RequiresDirective(final javax.lang.model.element.ModuleElement dependency,
                             final boolean isStatic,
                             final boolean transitive) {
      super(DirectiveKind.REQUIRES);
      this.dependency = Objects.requireNonNull(dependency, "dependency");
      this.isStatic = isStatic;
      this.transitive = transitive;
    }

    @Override // Directive
    public final <R, P> R accept(final DirectiveVisitor<R, P> v, final P p) {
      return v.visitRequires(this, p);
    }

    @Override // RequiresDirective
    public final javax.lang.model.element.ModuleElement getDependency() {
      return this.dependency;
    }

    @Override // RequiresDirective
    public final boolean isStatic() {
      return this.isStatic;
    }

    @Override // RequiresDirective
    public final boolean isTransitive() {
      return this.transitive;
    }

  }

  public final class UsesDirective extends Directive implements javax.lang.model.element.ModuleElement.UsesDirective {

    private final TypeElement service;

    public UsesDirective(final TypeElement service) {
      super(DirectiveKind.USES);
      this.service = Objects.requireNonNull(service, "service");
    }

    @Override // Directive
    public final <R, P> R accept(final DirectiveVisitor<R, P> v, final P p) {
      return v.visitUses(this, p);
    }

    @Override // UsesDirective
    public final TypeElement getService() {
      return this.service;
    }

  }


}
