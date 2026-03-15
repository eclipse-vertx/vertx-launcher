/*
 * Copyright (c) 2011-2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */

package io.vertx.launcher.application.tests;

import io.vertx.core.VertxOptions;
import io.vertx.launcher.application.ConfigScope;
import io.vertx.launcher.application.HookContext;
import io.vertx.launcher.application.VertxApplication;
import io.vertx.launcher.application.VertxApplicationHooks;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariables;

public class CustomConfigLoaderTest {

  private TestHooks hooks;

  @BeforeEach
  void setUp() {
    TestVerticle.instanceCount.set(0);
    TestVerticle.conf = null;
    TestConfigLoader.reset();
    TestConfigLoader.enabled = true;
    hooks = new TestHooks();
  }

  @AfterEach
  void tearDown() {
    TestConfigLoader.reset();
    if (hooks != null && hooks.vertx != null) {
      CompletableFuture<Void> future = hooks.vertx.close().toCompletionStage().toCompletableFuture();
      await("Failure to close Vert.x").atMost(Duration.ofSeconds(10)).until(future::isDone);
    }
  }

  @Test
  public void testCustomConfigLoaderIsInvoked() {
    TestConfigLoader.eventLoopPoolSize = 77;

    AtomicReference<VertxOptions> vertxOptions = new AtomicReference<>();
    hooks = new TestHooks() {
      @Override
      public void beforeStartingVertx(HookContext context) {
        vertxOptions.set(context.vertxOptions());
      }
    };

    TestVertxApplication app = new TestVertxApplication(new String[] { "java:" + TestVerticle.class.getCanonicalName() }, hooks);

    app.launch();

    await("Server not started")
      .atMost(Duration.ofSeconds(10))
      .until(() -> TestVerticle.instanceCount.get(), equalTo(1));

    assertEquals(77, vertxOptions.get().getEventLoopPoolSize());
    assertTrue(TestConfigLoader.invocations.contains(ConfigScope.VERTX));
  }

  @Test
  public void testCustomConfigLoaderOrder() throws Exception {
    // Custom loader (order=1500) sets eventLoopPoolSize=77
    // Env vars (order=1000) set eventLoopPoolSize=42
    // Custom loader should win because it has higher order (applied later)
    TestConfigLoader.eventLoopPoolSize = 77;

    AtomicReference<VertxOptions> vertxOptions = new AtomicReference<>();
    hooks = new TestHooks() {
      @Override
      public void beforeStartingVertx(HookContext context) {
        vertxOptions.set(context.vertxOptions());
      }
    };

    withEnvironmentVariables("VERTX_OPTIONS_EVENT_LOOP_POOL_SIZE", "42")
      .execute(() -> {
        TestVertxApplication app = new TestVertxApplication(new String[] { "java:" + TestVerticle.class.getCanonicalName() }, hooks);
        app.launch();
      });

    await("Server not started")
      .atMost(Duration.ofSeconds(10))
      .until(() -> TestVerticle.instanceCount.get(), equalTo(1));

    assertEquals(77, vertxOptions.get().getEventLoopPoolSize());
  }

  @Test
  public void testSystemPropertiesOverrideCustomConfigLoader() {
    // Custom loader (order=1500) sets eventLoopPoolSize=77
    // System properties (order=2000) set eventLoopPoolSize=99
    // System properties should win because they have higher order
    TestConfigLoader.eventLoopPoolSize = 77;

    AtomicReference<VertxOptions> vertxOptions = new AtomicReference<>();
    hooks = new TestHooks() {
      @Override
      public void beforeStartingVertx(HookContext context) {
        vertxOptions.set(context.vertxOptions());
      }
    };

    try {
      System.setProperty("vertx.options.eventLoopPoolSize", "99");
      TestVertxApplication app = new TestVertxApplication(new String[] { "java:" + TestVerticle.class.getCanonicalName() }, hooks);
      app.launch();
    } finally {
      System.clearProperty("vertx.options.eventLoopPoolSize");
    }

    await("Server not started")
      .atMost(Duration.ofSeconds(10))
      .until(() -> TestVerticle.instanceCount.get(), equalTo(1));

    assertEquals(99, vertxOptions.get().getEventLoopPoolSize());
  }

  private static class TestVertxApplication extends VertxApplication {
    public TestVertxApplication(String[] args, VertxApplicationHooks hooks) {
      super(args, hooks, true, false);
    }
  }
}
