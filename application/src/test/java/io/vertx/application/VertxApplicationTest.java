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

import io.vertx.core.CustomMetricsOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.application.HookContext;
import io.vertx.core.application.VertxApplication;
import io.vertx.core.application.VertxApplicationHooks;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.spi.VertxServiceProvider;
import io.vertx.test.fakecluster.FakeClusterManager;
import io.vertx.test.verticles.TestVerticle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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

  @BeforeEach
  void setUp() {
    TestVerticle.instanceCount.set(0);
    TestVerticle.conf = null;
  }

  @AfterEach
  void tearDown() throws IOException {
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
    ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(createMetricsFromMetaInfLoader("io.vertx.core.CustomMetricsFactory"));
    try {
      System.setProperty("vertx.metrics.options.enabled", "true");
      myVertxApplication.launch(new String[0], hooks);
    } finally {
      Thread.currentThread().setContextClassLoader(oldCL);
      clearProperties();
    }
    assertServerStarted();
    assertNotNull(metricsOptions.get());
    assertTrue(metricsOptions.get().isEnabled());
  }

  @Test
  public void testRunVerticle() {
    testRunVerticleMultiple(1);
  }

  @Test
  public void testRunVerticleMultipleInstances() {
    testRunVerticleMultiple(10);
  }

  public void testRunVerticleMultiple(int instances) {
    VertxApplication myVertxApplication = new VertxApplication();
    myVertxApplication.launch(new String[]{"java:" + TestVerticle.class.getCanonicalName(), "-instances", String.valueOf(instances)}, hooks);
    await("Server not started")
      .atMost(Duration.ofSeconds(10))
      .until(() -> TestVerticle.instanceCount.get(), equalTo(instances));
  }

  @TempDir
  File testFolder;

  @Test
  public void testConfigureFromJsonFile() throws Exception {
    testConfigureFromJson(true);
  }

  @Test
  public void testConfigureFromJsonString() throws Exception {
    testConfigureFromJson(false);
  }

  private void testConfigureFromJson(boolean jsonFile) throws Exception {
    JsonObject json = new JsonObject()
      .put("eventLoopPoolSize", 123)
      .put("maxEventLoopExecuteTime", 123767667)
      .put("metricsOptions", new JsonObject().put("enabled", true))
      .put("eventBusOptions", new JsonObject().put("clusterPublicHost", "mars"))
      .put("maxEventLoopExecuteTimeUnit", "SECONDS");

    String optionsArg;
    if (jsonFile) {
      File file = new File(testFolder, "options.json");
      Files.write(file.toPath(), json.toBuffer().getBytes());
      optionsArg = file.getPath();
    } else {
      optionsArg = json.toString();
    }

    VertxApplication myVertxApplication = new VertxApplication();
    AtomicReference<VertxOptions> vertxOptions = new AtomicReference<>();
    hooks = new MyHooks() {
      @Override
      public void beforeStartingVertx(HookContext context) {
        vertxOptions.set(context.vertxOptions());
      }
    };
    myVertxApplication.launch(new String[]{"java:" + TestVerticle.class.getCanonicalName(), "-options", optionsArg}, hooks);
    await("Server not started")
      .atMost(Duration.ofSeconds(10))
      .until(() -> TestVerticle.instanceCount.get(), equalTo(1));

    VertxOptions opts = vertxOptions.get();

    assertEquals(123, opts.getEventLoopPoolSize(), 0);
    assertEquals(123767667L, opts.getMaxEventLoopExecuteTime());
    assertTrue(opts.getMetricsOptions().isEnabled());
    assertEquals("mars", opts.getEventBusOptions().getClusterPublicHost());
    assertEquals(TimeUnit.SECONDS, opts.getMaxEventLoopExecuteTimeUnit());
  }

  @Test
  public void testConfigureFromSystemProperties() {
    testConfigureFromSystemProperties(false);
  }

  @Test
  public void testConfigureFromSystemPropertiesClustered() {
    testConfigureFromSystemProperties(true);
  }

  private void testConfigureFromSystemProperties(boolean clustered) {
    VertxApplication myVertxApplication = new VertxApplication();
    String[] args;
    if (clustered) {
      args = new String[]{"java:" + TestVerticle.class.getCanonicalName(), "-cluster"};
    } else {
      args = new String[]{"java:" + TestVerticle.class.getCanonicalName()};
    }

    AtomicReference<VertxOptions> vertxOptions = new AtomicReference<>();
    hooks = new MyHooks() {
      @Override
      public void beforeStartingVertx(HookContext context) {
        vertxOptions.set(context.vertxOptions());
      }
    };

    ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(createMetricsFromMetaInfLoader("io.vertx.core.CustomMetricsFactory"));
    try {
      System.setProperty("vertx.options.eventLoopPoolSize", "123");
      System.setProperty("vertx.options.maxEventLoopExecuteTime", "123767667");
      System.setProperty("vertx.metrics.options.enabled", "true");
      System.setProperty("vertx.options.maxEventLoopExecuteTimeUnit", "SECONDS");
      myVertxApplication.launch(args, hooks);
    } finally {
      Thread.currentThread().setContextClassLoader(oldCL);
      clearProperties();
    }
    await("Server not started")
      .atMost(Duration.ofSeconds(10))
      .until(() -> TestVerticle.instanceCount.get(), equalTo(1));


    VertxOptions opts = vertxOptions.get();

    assertEquals(123, opts.getEventLoopPoolSize(), 0);
    assertEquals(123767667L, opts.getMaxEventLoopExecuteTime());
    assertTrue(opts.getMetricsOptions().isEnabled());
    assertEquals(TimeUnit.SECONDS, opts.getMaxEventLoopExecuteTimeUnit());
  }

  private void clearProperties() {
    Set<String> toClear = new HashSet<>();
    Enumeration<?> e = System.getProperties().propertyNames();
    // Uhh, properties suck
    while (e.hasMoreElements()) {
      String propName = (String) e.nextElement();
      if (propName.startsWith("vertx.options")) {
        toClear.add(propName);
      }
    }
    toClear.forEach(System::clearProperty);
  }

  @Test
  public void testCustomMetricsOptions() {
    VertxApplication myVertxApplication = new VertxApplication();

    AtomicReference<VertxOptions> vertxOptions = new AtomicReference<>();
    hooks = new MyHooks() {
      @Override
      public void beforeStartingVertx(HookContext context) {
        vertxOptions.set(context.vertxOptions());
      }
    };
    ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(createMetricsFromMetaInfLoader("io.vertx.core.CustomMetricsFactory"));
    try {
      System.setProperty("vertx.metrics.options.enabled", "true");
      System.setProperty("vertx.metrics.options.customProperty", "customPropertyValue");
      myVertxApplication.launch(new String[]{"java:" + TestVerticle.class.getCanonicalName()}, hooks);
    } finally {
      Thread.currentThread().setContextClassLoader(oldCL);
      clearProperties();
    }
    await("Server not started")
      .atMost(Duration.ofSeconds(10))
      .until(() -> TestVerticle.instanceCount.get(), equalTo(1));


    VertxOptions opts = vertxOptions.get();

    CustomMetricsOptions custom = (CustomMetricsOptions) opts.getMetricsOptions();
    assertEquals("customPropertyValue", custom.getCustomProperty());
    assertTrue(hooks.vertx.isMetricsEnabled());
  }

  private ClassLoader createMetricsFromMetaInfLoader(String factoryFqn) {
    return new URLClassLoader(new URL[0], Thread.currentThread().getContextClassLoader()) {
      @Override
      public Enumeration<URL> findResources(String name) throws IOException {
        if (name.equals("META-INF/services/" + VertxServiceProvider.class.getName())) {
          File f = new File(testFolder, "vertx.txt");
          Files.write(f.toPath(), factoryFqn.getBytes());
          return Collections.enumeration(Collections.singleton(f.toURI().toURL()));
        }
        return super.findResources(name);
      }
    };
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
