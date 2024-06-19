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

import io.vertx.core.Verticle;
import io.vertx.core.VertxBuilder;
import io.vertx.core.VertxOptions;
import io.vertx.core.internal.VertxInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.impl.DummyVertxMetrics;
import io.vertx.test.fakecluster.FakeClusterManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.function.Supplier;

import static io.vertx.launcher.application.VertxApplicationTest.assertServerStarted;
import static io.vertx.launcher.application.VertxApplicationTest.getContent;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class VertxApplicationExtensibilityTest {

  private TestHooks hooks;

  @AfterEach
  public void tearDown() {
    if (hooks != null && hooks.vertx != null) {
      hooks.vertx.close();
    }
    FakeClusterManager.reset();
  }

  @Test
  public void testExtendingMainVerticle() {
    hooks = new TestHooks() {
      @Override
      public Supplier<Verticle> verticleSupplier() {
        return () -> new HttpTestVerticle();
      }
    };
    TestVertxApplication app = new TestVertxApplication(new String[0], hooks);
    app.launch();
    assertServerStarted();
  }

  @Test
  public void testThatCustomLauncherCanUpdateConfigurationWhenNoneArePassed() throws IOException {
    long time = System.nanoTime();
    hooks = new TestHooks() {
      @Override
      public Supplier<Verticle> verticleSupplier() {
        return () -> new HttpTestVerticle();
      }

      @Override
      public void beforeDeployingVerticle(HookContext context) {
        context.deploymentOptions().setConfig(new JsonObject().put("time", time));
      }
    };
    TestVertxApplication app = new TestVertxApplication(new String[0], hooks);
    app.launch();
    assertServerStarted();
    assertEquals(time, getContent().getJsonObject("conf").getLong("time"));
  }

  @Test
  public void testThatCustomLauncherCanUpdateConfiguration() throws IOException {
    long time = System.nanoTime();
    hooks = new TestHooks() {
      @Override
      public Supplier<Verticle> verticleSupplier() {
        return () -> new HttpTestVerticle();
      }

      @Override
      public void beforeDeployingVerticle(HookContext context) {
        context.deploymentOptions().getConfig().put("time", time);
      }
    };
    TestVertxApplication app = new TestVertxApplication(new String[]{"-conf={\"time\":345667}"}, hooks);
    app.launch();
    assertServerStarted();
    assertEquals(time, getContent().getJsonObject("conf").getLong("time"));
  }

  @Test
  public void testThatCustomLauncherCanCustomizeMetricsOption() throws Exception {
    hooks = new TestHooks() {
      @Override
      public Supplier<Verticle> verticleSupplier() {
        return () -> new HttpTestVerticle();
      }

      @Override
      public void beforeStartingVertx(HookContext context) {
        context.vertxOptions().getMetricsOptions()
          .setEnabled(true);
      }

      @Override
      public VertxBuilder createVertxBuilder(VertxOptions options) {
        return super.createVertxBuilder(options).withMetrics(o -> DummyVertxMetrics.INSTANCE);
      }
    };
    TestVertxApplication app = new TestVertxApplication(new String[0], hooks);
    app.launch();
    assertServerStarted();
    assertEquals(TRUE, getContent().getBoolean("metrics"));
  }

  @Test
  public void testThatCustomLauncherCanCustomizeClusterManager() throws Exception {
    FakeClusterManager clusterManager = new FakeClusterManager();
    hooks = new TestHooks() {
      @Override
      public Supplier<Verticle> verticleSupplier() {
        return () -> new HttpTestVerticle();
      }

      @Override
      public VertxBuilder createVertxBuilder(VertxOptions options) {
        return super.createVertxBuilder(options).withClusterManager(clusterManager);
      }
    };
    TestVertxApplication app = new TestVertxApplication(new String[]{"-cluster"}, hooks);
    app.launch();
    assertServerStarted();
    assertEquals(TRUE, getContent().getBoolean("clustered"));
    assertSame(clusterManager, ((VertxInternal) hooks.vertx).getClusterManager());
  }

  private static class TestVertxApplication extends VertxApplication {

    public TestVertxApplication(String[] args, VertxApplicationHooks hooks) {
      super(args, hooks, false, false);
    }
  }
}
