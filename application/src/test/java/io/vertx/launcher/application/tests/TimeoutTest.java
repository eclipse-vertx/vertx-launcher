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
import io.vertx.core.json.JsonObject;
import io.vertx.launcher.application.VertxApplication;
import io.vertx.launcher.application.VertxApplicationHooks;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static io.vertx.launcher.application.ExitCodes.VERTX_DEPLOYMENT;
import static org.awaitility.Awaitility.await;
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
  void testNegativeStartupTimeoutCliOptionFallsBackToDefault() {
    testInvalidCliOptionFallsBackToDefault("-10");
  }

  @Test
  void testZeroStartupTimeoutCliOptionFallsBackToDefault() {
    testInvalidCliOptionFallsBackToDefault("0");
  }

  private void testInvalidCliOptionFallsBackToDefault(String value) {
    JsonObject config = new JsonObject().put(DelayedStartVerticle.DELAY_MS_CONFIG_KEY, 500L);
    int exitCode = new TestVertxApplication(
      new String[]{
        "--startup-timeout-seconds", value,
        "--conf", config.encode(),
        "java:" + DelayedStartVerticle.class.getName()
      },
      hooks
    ).launch();
    assertEquals(0, exitCode);
  }

  @Test
  void testNegativeDeploymentTimeoutEnvVarFallsBackToDefault() throws Exception {
    testInvalidEnvVarFallsBackToDefault("-5");
  }

  @Test
  void testZeroDeploymentTimeoutEnvVarFallsBackToDefault() throws Exception {
    testInvalidEnvVarFallsBackToDefault("0");
  }

  @Test
  void testEmptyStringDeploymentTimeoutEnvVarFallsBackToDefault() throws Exception {
    testInvalidEnvVarFallsBackToDefault("");
  }

  @Test
  void testWhitespaceDeploymentTimeoutEnvVarFallsBackToDefault() throws Exception {
    testInvalidEnvVarFallsBackToDefault("   ");
  }

  @Test
  void testInvalidStartupTimeoutEnvVarFallsBackToDefault() throws Exception {
    testInvalidEnvVarFallsBackToDefault("not-a-number");
  }

  private void testInvalidEnvVarFallsBackToDefault(String value) throws Exception {
    withEnvironmentVariables("VERTX_DEPLOYMENT_TIMEOUT_SECONDS", value).execute(() -> {
      JsonObject config = new JsonObject().put(DelayedStartVerticle.DELAY_MS_CONFIG_KEY, 500L);
      int exitCode = new TestVertxApplication(
        new String[]{
          "--conf", config.encode(),
          "java:" + DelayedStartVerticle.class.getName()
        },
        hooks
      ).launch();
      assertEquals(0, exitCode);
    });
  }

  @Test
  void testDeploymentTimeoutWithCliOption() {
    JsonObject config = new JsonObject().put(DelayedStartVerticle.DELAY_MS_CONFIG_KEY, 5000L);
    int exitCode = new TestVertxApplication(
      new String[]{
        "--deployment-timeout-seconds", "1",
        "--conf", config.encode(),
        "java:" + DelayedStartVerticle.class.getName()
      },
      hooks
    ).launch();
    assertEquals(VERTX_DEPLOYMENT, exitCode);
  }

  @Test
  void testDeploymentTimeoutWithEnvVar() throws Exception {
    JsonObject config = new JsonObject().put(DelayedStartVerticle.DELAY_MS_CONFIG_KEY, 5000L);
    withEnvironmentVariables("VERTX_DEPLOYMENT_TIMEOUT_SECONDS", "1").execute(() -> {
      int exitCode = new TestVertxApplication(
        new String[]{
          "--conf", config.encode(),
          "java:" + DelayedStartVerticle.class.getName()
        },
        hooks
      ).launch();
      assertEquals(VERTX_DEPLOYMENT, exitCode);
    });
  }

  @Test
  void testCliValueOverridesEnvVar() throws Exception {
    JsonObject config = new JsonObject().put(DelayedStartVerticle.DELAY_MS_CONFIG_KEY, 5000L);
    withEnvironmentVariables("VERTX_DEPLOYMENT_TIMEOUT_SECONDS", "10").execute(() -> {
      int exitCode = new TestVertxApplication(
        new String[]{
          "--deployment-timeout-seconds", "1",
          "--conf", config.encode(),
          "java:" + DelayedStartVerticle.class.getName()
        },
        hooks
      ).launch();
      assertEquals(VERTX_DEPLOYMENT, exitCode);
    });
  }

  public static class DelayedStartVerticle extends AbstractVerticle {
    static final String DELAY_MS_CONFIG_KEY = "delayMs";

    @Override
    public void start(Promise<Void> startPromise) {
      long delayMs = config().getLong(DELAY_MS_CONFIG_KEY, 0L);
      if (delayMs <= 0) {
        startPromise.complete();
        return;
      }
      vertx.setTimer(delayMs, id -> {
        startPromise.complete();
      });
    }
  }

  private static class TestVertxApplication extends VertxApplication {
    TestVertxApplication(String[] args, VertxApplicationHooks hooks) {
      super(args, hooks, true, false);
    }
  }
}
