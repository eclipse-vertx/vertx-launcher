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

import io.vertx.core.AbstractVerticle;
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

public class CustomApplicationLowMemoryTest {

  private static final String MSG_HOOK = CustomApplicationLowMemoryTest.class.getSimpleName() + "-hook";

  private Process process;
  private File output;

  @BeforeEach
  public void setUp() throws Exception {
    output = File.createTempFile(CustomApplicationLowMemoryTest.class.getSimpleName(), ".txt");
    output.deleteOnExit();
  }

  @AfterEach
  public void tearDown() {
    if (process != null) {
      process.destroyForcibly();
    }
  }

  @Test
  public void testCloseHookInvoked() throws Exception {
    startExternalProcess();
    await("Hook not invoked")
      .atMost(Duration.ofSeconds(10))
      .until(() -> outputContainsMsgHook(), equalTo(TRUE));
    stopExternalProcess();
  }

  private void startExternalProcess() throws IOException {
    String javaHome = System.getProperty("java.home");
    String classpath = System.getProperty("java.class.path");

    List<String> command = new ArrayList<>();
    command.add(javaHome + File.separator + "bin" + File.separator + "java");
    command.add("-Xms100M");
    command.add("-Xmx100M");
    command.add("-classpath");
    command.add(classpath);
    command.add(MyVertxApplication.class.getName());
    command.add(Verticle.class.getName());

    process = new ProcessBuilder(command)
      .redirectOutput(output)
      .redirectErrorStream(true)
      .start();
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

  private boolean outputContainsMsgHook() {
    try {
      return Files.readAllLines(output.toPath()).contains(MSG_HOOK);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static class MyVertxApplication extends VertxApplication implements VertxApplicationHooks {

    public MyVertxApplication(String[] args) {
      super(args, null, true, true);
    }

    public static void main(String[] args) {
      MyVertxApplication app = new MyVertxApplication(args);
      app.launch();
    }

    @Override
    public void beforeStoppingVertx(HookContext context) {
      System.out.println(MSG_HOOK);
    }
  }

  public static class Verticle extends AbstractVerticle {

    private final Runtime runtime;
    @SuppressWarnings("unused")
    private List<byte[]> arrays;

    public Verticle() {
      runtime = Runtime.getRuntime();
    }

    @Override
    public void start() {
      vertx.executeBlocking(() -> {
        List<byte[]> res = new ArrayList<>();
        long l;
        do {
          res.add(new byte[5 * 1024]);
          l = runtime.freeMemory();
        } while (l > 15 * 1024 * 1024);
        runtime.gc();
        Thread.sleep(100);
        return res;
      }).onComplete(ar1 -> {
        if (ar1.succeeded()) {
          arrays = ar1.result();
          context.owner().close();
        } else {
          ar1.cause().printStackTrace();
        }
      });
    }
  }
}
