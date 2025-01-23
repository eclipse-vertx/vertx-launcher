package io.vertx.launcher.application;

import io.vertx.test.TestVerticle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SystemExitTest {

  @TempDir
  Path tempDir;

  private File output;
  private Process process;


  @BeforeEach
  public void setUp() throws Exception {
    output = Files.createFile(tempDir.resolve("output.txt")).toFile();
  }

  @AfterEach
  public void tearDown() {
    if (process != null && process.isAlive()) {
      process.destroyForcibly();
    }
  }

  public static final class ShouldExitWhenHookThrowsException {
    public static void main(String[] args) {
      var hooks = new VertxApplicationHooks() {
        @Override
        public void beforeStartingVertx(HookContext context) {
          throw new RuntimeException("boom");
        }
      };
      var app = new VertxApplication(args, hooks);
      app.launch();
    }
  }

  @Test
  void shouldExitWhenHookThrowsException() throws Exception {
    startExternalProcess(ShouldExitWhenHookThrowsException.class, TestVerticle.class);
    assertEquals(1, process.waitFor());

  }

  private void startExternalProcess(Class<?> mainClass, Class<?> verticleClass) throws IOException {
    String javaHome = System.getProperty("java.home");
    String classpath = System.getProperty("java.class.path");

    List<String> command = new ArrayList<>();
    command.add(javaHome + File.separator + "bin" + File.separator + "java");
    command.add("-classpath");
    command.add(classpath);
    command.add(mainClass.getName());
    command.add(verticleClass.getName());

    process = new ProcessBuilder(command)
      .redirectOutput(output)
      .redirectErrorStream(true)
      .start();
  }
}
