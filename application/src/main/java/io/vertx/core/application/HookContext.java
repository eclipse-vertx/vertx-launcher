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

package io.vertx.core.application;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

/**
 * Exposes objects available at different stages of the {@link VertxApplication} launch process.
 */
public interface HookContext {

  /**
   * The Vert.x options, which can be modified before Vert.x is started.
   */
  VertxOptions vertxOptions();

  /**
   * The Vert.x instance, after it has started.
   */
  Vertx vertx();

  /**
   * The name of the verticle to deploy.
   * <p>
   * May be {@code null} if not provided on the command line or configured in the JAR's {@code META-INF/MANIFEST.MF} file.
   */
  String mainVerticle();

  /**
   * The verticle deployment options, which can be modified before Vert.x is started.
   */
  DeploymentOptions deploymentOptions();

  /**
   * The deployment identifier, after the verticle has started.
   */
  String deploymentId();
}
