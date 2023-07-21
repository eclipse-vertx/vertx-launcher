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

import io.vertx.core.Verticle;

import java.util.function.Supplier;

/**
 * Methods invoked by the {@link VertxApplication} at different stages of the launch process.
 */
public interface VertxApplicationHooks {

  /**
   * Invoked before starting Vert.x.
   * <p>
   * At this stage the {@link HookContext#vertxOptions()} can still be modified.
   *
   * @param context exposes objects available at this stage of the {@link VertxApplication} launch process
   */
  default void beforeStartingVertx(HookContext context) {
  }

  /**
   * Invoked after Vert.x has started successfully.
   *
   * @param context exposes objects available at this stage of the {@link VertxApplication} launch process
   */
  default void afterVertxStarted(HookContext context) {
  }

  /**
   * Invoked after Vert.x has failed to start.
   *
   * @param context exposes objects available at this stage of the {@link VertxApplication} launch process
   * @param t       the failure or {@code null} if Vert.x took too long to start
   */
  default void afterFailureToStartVertx(HookContext context, Throwable t) {
  }

  /**
   * Invoked before deploying the main verticle.
   * <p>
   * If the implementation returns a non-{@code null} supplier, it will be used to create the instance(s) of the verticle,
   * regardless of the value provided on the command line or in the JAR's {@code META-INF/MANIFEST.MF} file.
   */
  default Supplier<Verticle> verticleSupplier() {
    return null;
  }

  /**
   * Invoked before deploying the main verticle.
   * <p>
   * At this stage the {@link HookContext#deploymentOptions()} can still be modified.
   *
   * @param context exposes objects available at this stage of the {@link VertxApplication} launch process
   */
  default void beforeDeployingVerticle(HookContext context) {
  }

  /**
   * Invoked after the verticle has been deployed successfully.
   *
   * @param context exposes objects available at this stage of the {@link VertxApplication} launch process
   */
  default void afterVerticleDeployed(HookContext context) {
  }

  /**
   * Invoked after the verticle has failed to be deployed.
   * <p>
   * By default, the Vert.x instance is closed.
   *
   * @param context exposes objects available at this stage of the {@link VertxApplication} launch process
   * @param t       the failure or {@code null} if the verticle took too long to start
   */
  default void afterFailureToDeployVerticle(HookContext context, Throwable t) {
    context.vertx().close();
  }

  /**
   * Invoked before stopping Vert.x.
   *
   * @param context exposes objects available at this stage of the {@link VertxApplication} launch process
   */
  default void beforeStoppingVertx(HookContext context) {
  }

  /**
   * Invoked after Vert.x has stopped successfully.
   *
   * @param context exposes objects available at this stage of the {@link VertxApplication} launch process
   */
  default void afterVertxStopped(HookContext context) {
  }

  /**
   * Invoked after Vert.x has failed to stop.
   *
   * @param context exposes objects available at this stage of the {@link VertxApplication} launch process
   * @param t       the failure or {@code null} if Vert.x took too long to stop
   */
  default void afterFailureToStopVertx(HookContext context, Throwable t) {
  }
}
