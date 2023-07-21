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

package io.vertx.application;

import io.vertx.core.Vertx;
import io.vertx.core.application.HookContext;
import io.vertx.core.application.VertxApplication;
import io.vertx.core.application.VertxApplicationHooks;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.test.fakecluster.FakeClusterManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static io.vertx.core.application.ExitCodes.VERTX_DEPLOYMENT;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

public class VertxApplicationTest {

  private MyHooks hooks = new MyHooks();
  private Path manifest;
  private ByteArrayOutputStream out;
  private ByteArrayOutputStream err;

  @AfterEach
  public void tearDown() throws IOException {
    if (manifest != null) {
      Files.deleteIfExists(manifest);
    }
    if (hooks != null && hooks.vertx != null) {
      CompletableFuture<Void> future = hooks.vertx.close()
        .toCompletionStage()
        .toCompletableFuture();
      await("Failure to close Vert.x")
        .atMost(Duration.ofSeconds(10))
        .until(() -> future.isDone());
    }
    FakeClusterManager.reset();
  }

  private void setManifest(String name) throws Exception {
    URI resource = getClass().getClassLoader().getResource(name).toURI();
    assertEquals("file", resource.getScheme());
    Path source = Paths.get(resource);
    manifest = source.getParent().resolve("MANIFEST.MF");
    Files.copy(source, manifest, REPLACE_EXISTING, COPY_ATTRIBUTES);
  }

  @Test
  public void testDeploymentOfJavaVerticle() {
    VertxApplication myVertxApplication = new VertxApplication();
    myVertxApplication.launch(new String[]{HttpTestVerticle.class.getName()}, hooks);
    assertServerStarted();
  }

  @Test
  public void testDeploymentOfJavaVerticleWithCluster() throws IOException {
    VertxApplication myVertxApplication = new VertxApplication();
    myVertxApplication.launch(new String[]{HttpTestVerticle.class.getName(), "-cluster"}, hooks);
    assertServerStarted();
    assertEquals(TRUE, getContent().getBoolean("clustered"));
  }

  @Test
  public void testFatJarWithoutMainVerticle() throws Exception {
    setManifest("META-INF/MANIFEST-No-Main-Verticle.MF");
    Integer exitCode = captureOutput(() -> {
      VertxApplication myVertxApplication = new VertxApplication();
      return myVertxApplication.launch(new String[0], hooks);
    });
    assertEquals(VERTX_DEPLOYMENT, exitCode);
    assertTrue(out.toString().contains("Usage:"));
  }

  @Test
  public void testFatJarWithMissingMainVerticle() throws Exception {
    setManifest("META-INF/MANIFEST-Missing-Main-Verticle.MF");
    Integer exitCode = captureOutput(() -> {
      VertxApplication myVertxApplication = new VertxApplication();
      return myVertxApplication.launch(new String[0], hooks);
    });
    assertEquals(VERTX_DEPLOYMENT, exitCode);
    assertTrue(out.toString().contains("Usage:"));
    assertTrue(err.toString().contains("ClassNotFoundException"));
  }

  @Test
  public void testFatJarWithHTTPVerticle() throws Exception {
    setManifest("META-INF/MANIFEST-Http-Verticle.MF");
    VertxApplication myVertxApplication = new VertxApplication();
    myVertxApplication.launch(new String[0], hooks);
    assertServerStarted();
    assertEquals(FALSE, getContent().getBoolean("clustered"));
  }

  @Test
  public void testFatJarWithHTTPVerticleWithCluster() throws Exception {
    setManifest("META-INF/MANIFEST-Http-Verticle.MF");
    VertxApplication myVertxApplication = new VertxApplication();
    myVertxApplication.launch(new String[]{"-cluster"}, hooks);
    assertServerStarted();
    assertEquals(TRUE, getContent().getBoolean("clustered"));
  }

  @Test
  public void testWithConfProvidedInline() throws Exception {
    setManifest("META-INF/MANIFEST-Http-Verticle.MF");
    VertxApplication myVertxApplication = new VertxApplication();
    long someNumber = new Random().nextLong();
    myVertxApplication.launch(new String[]{"--conf={\"random\":" + someNumber + "}"}, hooks);
    assertServerStarted();
    assertEquals(someNumber, getContent().getJsonObject("conf").getLong("random"));
  }

  @Test
  public void testWithBrokenConfProvidedInline() throws Exception {
    setManifest("META-INF/MANIFEST-Http-Verticle.MF");
    VertxApplication myVertxApplication = new VertxApplication();
    // There is a missing curly brace in the json fragment.
    // This is normal, as the test checks that the configuration is not read in this case.
    myVertxApplication.launch(new String[]{"--conf={\"name\":\"vertx\""}, hooks);
    assertServerStarted();
    assertEquals("{}", getContent().getJsonObject("conf").toString().replaceAll("\\s", ""));
  }

  @Test
  public void testWithConfProvidedAsFile() throws Exception {
    setManifest("META-INF/MANIFEST-Http-Verticle.MF");
    VertxApplication myVertxApplication = new VertxApplication();
    URI resource = getClass().getClassLoader().getResource("verticle-conf.json").toURI();
    assertEquals("file", resource.getScheme());
    Path source = Paths.get(resource);
    myVertxApplication.launch(new String[]{"--conf", source.toString()}, hooks);
    assertServerStarted();
    assertEquals("vertx", getContent().getJsonObject("conf").getString("name"));
  }

  @Test
  public void testMetricsEnabledFromCommandLine() throws Exception {
    setManifest("META-INF/MANIFEST-Http-Verticle.MF");
    VertxApplication myVertxApplication = new VertxApplication();
    AtomicReference<MetricsOptions> metricsOptions = new AtomicReference<>();
    hooks = new MyHooks() {
      @Override
      public void beforeStartingVertx(HookContext context) {
        metricsOptions.set(context.vertxOptions().getMetricsOptions());
      }
    };
    try {
      System.setProperty("vertx.metrics.options.enabled", "true");
      myVertxApplication.launch(new String[0], hooks);
    } finally {
      System.clearProperty("vertx.metrics.options.enabled");
    }
    assertServerStarted();
    assertNotNull(metricsOptions.get());
    assertTrue(metricsOptions.get().isEnabled());
  }


  public static class MyHooks implements VertxApplicationHooks {

    volatile Vertx vertx;

    @Override
    public void afterVertxStarted(HookContext context) {
      vertx = context.vertx();
    }
  }

  private Integer captureOutput(Callable<Integer> callable) throws Exception {
    PrintStream originalOut = System.out;
    PrintStream originalErr = System.err;
    try {
      out = new ByteArrayOutputStream();
      PrintStream psOut = new PrintStream(out);
      System.setOut(psOut);

      err = new ByteArrayOutputStream();
      PrintStream psErr = new PrintStream(err);
      System.setErr(psErr);

      Integer exitCode = callable.call();

      psOut.flush();
      psErr.flush();

      return exitCode;

    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }
  }

  static void assertServerStarted() {
    await("Server not started")
      .atMost(Duration.ofSeconds(10))
      .until(() -> getHttpCode(), equalTo(200));
  }

  static int getHttpCode() throws IOException {
    return ((HttpURLConnection) new URL("http://localhost:8080")
      .openConnection()).getResponseCode();
  }

  static JsonObject getContent() throws IOException {
    URL url = new URL("http://localhost:8080");
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.connect();
    StringBuilder builder = new StringBuilder();
    try (BufferedReader buff = new BufferedReader(new InputStreamReader((InputStream) conn.getContent()))) {
      while (true) {
        String line = buff.readLine();
        if (line == null) {
          break;
        }
        builder.append(line).append("\n");
      }
    }
    return new JsonObject(builder.toString());
  }
}
