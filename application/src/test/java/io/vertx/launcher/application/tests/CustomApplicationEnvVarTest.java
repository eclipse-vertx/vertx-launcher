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
import io.vertx.core.VertxOptions;
import io.vertx.launcher.application.HookContext;
import io.vertx.launcher.application.VertxApplication;
import io.vertx.launcher.application.VertxApplicationHooks;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Boolean.TRUE;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;

public class CustomApplicationEnvVarTest {

  private static final String MSG_PREFIX = CustomApplicationEnvVarTest.class.getSimpleName();

  private Process process;
  private File output;

  @BeforeEach
  public void setUp() throws Exception {
    output = File.createTempFile(CustomApplicationEnvVarTest.class.getSimpleName(), ".txt");
    output.deleteOnExit();
  }

  @AfterEach
  public void tearDown() {
    if (process != null) {
      process.destroyForcibly();
    }
  }

  @Test
  public void testConfigureFromEnvVars() throws Exception {
    startExternalProcess(
      List.of(
        "VERTX_OPTIONS_EVENT_LOOP_POOL_SIZE=42",
        "VERTX_OPTIONS_MAX_EVENT_LOOP_EXECUTE_TIME=123767667",
        "VERTX_OPTIONS_MAX_EVENT_LOOP_EXECUTE_TIME_UNIT=SECONDS"
      ),
      new String[0]
    );

    await(MSG_PREFIX + "-eventLoopPoolSize=42 not found")
      .atMost(Duration.ofSeconds(10))
      .until(() -> outputContains(MSG_PREFIX + "-eventLoopPoolSize=42"), equalTo(TRUE));
    await(MSG_PREFIX + "-maxEventLoopExecuteTime=123767667 not found")
      .atMost(Duration.ofSeconds(10))
      .until(() -> outputContains(MSG_PREFIX + "-maxEventLoopExecuteTime=123767667"), equalTo(TRUE));
    await(MSG_PREFIX + "-maxEventLoopExecuteTimeUnit=SECONDS not found")
      .atMost(Duration.ofSeconds(10))
      .until(() -> outputContains(MSG_PREFIX + "-maxEventLoopExecuteTimeUnit=SECONDS"), equalTo(TRUE));

    stopExternalProcess();
  }

  @Test
  public void testSystemPropertiesOverrideEnvVars() throws Exception {
    startExternalProcess(
      List.of("VERTX_OPTIONS_EVENT_LOOP_POOL_SIZE=42"),
      new String[]{"-Dvertx.options.eventLoopPoolSize=99"}
    );

    await(MSG_PREFIX + "-eventLoopPoolSize=99 not found")
      .atMost(Duration.ofSeconds(10))
      .until(() -> outputContains(MSG_PREFIX + "-eventLoopPoolSize=99"), equalTo(TRUE));

    stopExternalProcess();
  }

  private void startExternalProcess(List<String> envVars, String[] extraJvmArgs) throws IOException {
    String javaHome = System.getProperty("java.home");
    String classpath = System.getProperty("java.class.path");

    List<String> command = new ArrayList<>();
    command.add(javaHome + File.separator + "bin" + File.separator + "java");
    command.addAll(List.of(extraJvmArgs));
    command.add("-classpath");
    command.add(classpath);
    command.add(MyVertxApplication.class.getName());
    command.add(Verticle.class.getName());

    ProcessBuilder pb = new ProcessBuilder(command)
      .redirectOutput(output)
      .redirectErrorStream(true);

    for (String envVar : envVars) {
      int idx = envVar.indexOf('=');
      pb.environment().put(envVar.substring(0, idx), envVar.substring(idx + 1));
    }

    process = pb.start();
  }

  private void stopExternalProcess() throws InterruptedException {
    AtomicBoolean stopped = new AtomicBoolean();
    new Thread(() -> {
      try {
        Thread.sleep(10_000);
      } catch (InterruptedException ignore) {
        return;
      }
      if (!stopped.get()) {
        process.destroy();
      }
    });
    process.waitFor();
    stopped.set(true);
  }

  private boolean outputContains(String line) {
    try {
      return Files.readAllLines(output.toPath()).contains(line);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static class MyVertxApplication extends VertxApplication implements VertxApplicationHooks {

    public MyVertxApplication(String[] args) {
      super(args, null, true, true);
    }

    public static void main(String[] args) {
      new MyVertxApplication(args).launch();
    }

    @Override
    public void beforeStartingVertx(HookContext context) {
      VertxOptions opts = context.vertxOptions();
      System.out.println(MSG_PREFIX + "-eventLoopPoolSize=" + opts.getEventLoopPoolSize());
      System.out.println(MSG_PREFIX + "-maxEventLoopExecuteTime=" + opts.getMaxEventLoopExecuteTime());
      System.out.println(MSG_PREFIX + "-maxEventLoopExecuteTimeUnit=" + opts.getMaxEventLoopExecuteTimeUnit());
    }
  }

  public static class Verticle extends AbstractVerticle {

    @Override
    public void start() {
      context.owner().close();
    }
  }
}
