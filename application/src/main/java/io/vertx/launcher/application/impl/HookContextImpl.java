/*
 * Copyright (c) 2011-2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */

package io.vertx.launcher.application.impl;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.launcher.application.HookContext;

public class HookContextImpl implements HookContext {

  private VertxOptions vertxOptions;
  private Vertx vertx;
  private String mainVerticle;
  private DeploymentOptions deploymentOptions;
  private String deploymentId;

  public synchronized void setVertxOptions(VertxOptions vertxOptions) {
    this.vertxOptions = vertxOptions;
  }

  @Override
  public synchronized VertxOptions vertxOptions() {
    return vertxOptions;
  }

  public synchronized void setVertx(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public synchronized Vertx vertx() {
    return vertx;
  }

  public synchronized void readyToDeploy(String mainVerticle, DeploymentOptions deploymentOptions) {
    this.mainVerticle = mainVerticle;
    this.deploymentOptions = deploymentOptions;
  }

  @Override
  public synchronized String mainVerticle() {
    return mainVerticle;
  }

  @Override
  public synchronized DeploymentOptions deploymentOptions() {
    return deploymentOptions;
  }

  public synchronized void setDeploymentId(String deploymentId) {
    this.deploymentId = deploymentId;
  }

  @Override
  public synchronized String deploymentId() {
    return deploymentId;
  }
}
