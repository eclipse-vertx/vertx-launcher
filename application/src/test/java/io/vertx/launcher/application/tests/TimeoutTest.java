/*
 * Copyright (c) 2011-2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */

package io.vertx.launcher.application.tests;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.launcher.application.VertxApplication;
import io.vertx.launcher.application.VertxApplicationHooks;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static io.vertx.launcher.application.ExitCodes.VERTX_DEPLOYMENT;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariables;

public class TimeoutTest {

  private TestHooks hooks;

  @BeforeEach
  void setUp() {
    hooks = new TestHooks();
    TestVerticle.instanceCount.set(0);
  }

  @AfterEach
  void tearDown() {
    if (hooks.vertx != null) {
      CompletableFuture<Void> future = hooks.vertx.close().toCompletionStage().toCompletableFuture();
      await("Failure to close Vert.x")
        .atMost(Duration.ofSeconds(10))
        .until(future::isDone);
    }
  }

  @Test
  void testDefaultTimeoutsStartApplicationSuccessfully() {
    new TestVertxApplication(new String[]{"java:" + TestVerticle.class.getCanonicalName()}, hooks).launch();
    await("Verticle not deployed")
      .atMost(Duration.ofSeconds(10))
      .until(TestVerticle.instanceCount::get, equalTo(1));
  }

  @Test
  void testStartupTimeoutCliOption() {
    new TestVertxApplication(
      new String[]{"--startup-timeout-seconds", "10", "java:" + TestVerticle.class.getCanonicalName()}, hooks
    ).launch();
    await("Verticle not deployed")
      .atMost(Duration.ofSeconds(10))
      .until(TestVerticle.instanceCount::get, equalTo(1));
  }

  @Test
  void testDeploymentTimeoutCliOption() {
    new TestVertxApplication(
      new String[]{"--deployment-timeout-seconds", "30", "java:" + TestVerticle.class.getCanonicalName()}, hooks
    ).launch();
    await("Verticle not deployed")
      .atMost(Duration.ofSeconds(10))
      .until(TestVerticle.instanceCount::get, equalTo(1));
  }

  @Test
  void testShutdownTimeoutCliOption() {
    new TestVertxApplication(
      new String[]{"--shutdown-timeout-seconds", "5", "java:" + TestVerticle.class.getCanonicalName()}, hooks
    ).launch();
    await("Verticle not deployed")
      .atMost(Duration.ofSeconds(10))
      .until(TestVerticle.instanceCount::get, equalTo(1));
  }

  @Test
  void testStartupTimeoutFromEnvVar() throws Exception {
    withEnvironmentVariables("VERTX_STARTUP_TIMEOUT_SECONDS", "10").execute(() -> {
      new TestVertxApplication(
        new String[]{"java:" + TestVerticle.class.getCanonicalName()}, hooks
      ).launch();
    });
    await("Verticle not deployed")
      .atMost(Duration.ofSeconds(10))
      .until(TestVerticle.instanceCount::get, equalTo(1));
  }

  @Test
  void testDeploymentTimeoutFromEnvVar() throws Exception {
    withEnvironmentVariables("VERTX_DEPLOYMENT_TIMEOUT_SECONDS", "30").execute(() -> {
      new TestVertxApplication(
        new String[]{"java:" + TestVerticle.class.getCanonicalName()}, hooks
      ).launch();
    });
    await("Verticle not deployed")
      .atMost(Duration.ofSeconds(10))
      .until(TestVerticle.instanceCount::get, equalTo(1));
  }

  @Test
  void testInvalidStartupTimeoutEnvVarFallsBackToDefault() throws Exception {
    withEnvironmentVariables("VERTX_STARTUP_TIMEOUT_SECONDS", "not-a-number").execute(() -> {
      new TestVertxApplication(
        new String[]{"java:" + TestVerticle.class.getCanonicalName()}, hooks
      ).launch();
    });
    await("Verticle not deployed")
      .atMost(Duration.ofSeconds(10))
      .until(TestVerticle.instanceCount::get, equalTo(1));
  }

  @Test
  void testDeploymentTimeoutExpiryReturnsDeploymentExitCode() {
    int exitCode = new TestVertxApplication(
      new String[]{"--deployment-timeout-seconds", "1", "java:" + NeverDeployingVerticle.class.getName()}, hooks
    ).launch();
    assertEquals(VERTX_DEPLOYMENT, exitCode);
  }

  /**
   * A verticle whose start promise is intentionally never completed, causing any deployment
   * attempt to hang until the configured deployment timeout fires.
   */
  public static class NeverDeployingVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) {
      // Intentionally never complete the start promise so the deployment times out.
    }
  }

  private static class TestVertxApplication extends VertxApplication {
    TestVertxApplication(String[] args, VertxApplicationHooks hooks) {
      super(args, hooks, true, false);
    }
  }
}
