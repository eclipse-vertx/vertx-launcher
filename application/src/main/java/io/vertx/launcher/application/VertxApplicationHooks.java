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

package io.vertx.launcher.application;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.VertxTracerFactory;

import java.util.function.Supplier;

/**
 * Methods invoked by the {@link VertxApplication} at different stages of the launch process.
 */
public interface VertxApplicationHooks {

  VertxApplicationHooks DEFAULT = new VertxApplicationHooks() {
  };

  /**
   * Invoked after parsing the {@code options} parameter. The content can be modified or replaced.
   *
   * @param vertxOptions the parsed JSON representation of {@link VertxOptions}
   * @return the JSON value to use
   */
  default JsonObject afterVertxOptionsParsed(JsonObject vertxOptions) {
    return vertxOptions;
  }

  /**
   * Invoked after parsing the {@code deployment-options} parameter. The content can be modified or replaced.
   *
   * @param deploymentOptions the parsed JSON representation of {@link DeploymentOptions}
   * @return the JSON value to use
   */
  default JsonObject afterDeploymentOptionsParsed(JsonObject deploymentOptions) {
    return deploymentOptions;
  }

  /**
   * Invoked after parsing the {@code conf} parameter. The content can be modified or replaced.
   *
   * @param config the parsed JSON representation of {@link DeploymentOptions#getConfig()}
   * @return the JSON value to use
   */
  default JsonObject afterConfigParsed(JsonObject config) {
    return config;
  }

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
   * Default implementation for the {@link VertxBuilder} creation.
   * <p>
   * This can be overridden in order to customize, for example, the tracer, with {@link VertxBuilder#withTracer(VertxTracerFactory)}.
   *
   * @param options the Vert.x options to use
   * @return the Vert.x builder instance
   */
  default VertxBuilder createVertxBuilder(VertxOptions options) {
    return Vertx.builder().with(options);
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
   * Invoked before deploying the main verticle or {@link Deployable}.
   * <p>
   * If the implementation returns a non-{@code null} supplier, it will be used to create the instance(s) of the verticle,
   * regardless of the value provided on the command line or in the JAR's {@code META-INF/MANIFEST.MF} file.
   */
  default Supplier<? extends Deployable> verticleSupplier() {
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
