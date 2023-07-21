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

package io.vertx.core.application;

import io.vertx.core.application.impl.CommandException;
import io.vertx.core.application.impl.VertxApplicationCommand;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import picocli.CommandLine;

import java.util.Objects;

import static picocli.CommandLine.Help.Ansi;

/**
 * A {@code main()} class that can be used to create the Vert.x instance and deploy a verticle.
 * <p>
 * It can be used, for example, as the main class of an executable uber-jar, so that you can run verticles directly with:
 * {@code java -jar myapp.jar}.
 */
public class VertxApplication {

  private static final Logger log = LoggerFactory.getLogger(VertxApplication.class);

  public static void main(String[] args) {
    VertxApplication vertxApplication = new VertxApplication();
    int exitCode = vertxApplication.launch(args);
    vertxApplication.processExitCode(exitCode);
  }

  /**
   * Launches the Vert.x application.
   * <p>
   * Subclasses may implement {@link VertxApplicationHooks}.
   * In this case, this object's hook methods will be invoked at different stages of the launch process.
   *
   * @param args the application arguments, usually provided on the command line
   * @return an exit code, {@code 0} means the verticle has been deployed successfully
   */
  public int launch(String[] args) {
    VertxApplicationHooks hooks;
    if (this instanceof VertxApplicationHooks) {
      hooks = (VertxApplicationHooks) this;
    } else {
      hooks = new VertxApplicationHooks() {
      };
    }
    return launch(args, hooks);
  }

  /**
   * Launches the Vert.x application.
   *
   * @param args  the application arguments, usually provided on the command line
   * @param hooks an instance of {@link VertxApplicationHooks} to be invoked at different stages of the launch process
   * @return an exit code, {@code 0} means the verticle has been deployed successfully
   */
  public int launch(String[] args, VertxApplicationHooks hooks) {
    return launch(args, hooks, true);
  }

  /**
   * Launches the Vert.x application.
   *
   * @param args                the application arguments, usually provided on the command line
   * @param hooks               an instance of {@link VertxApplicationHooks} to be invoked at different stages of the launch process
   * @param printUsageOnFailure whether usage should be printed to {@link System#out} if the returned value is not zero
   * @return an exit code, {@code 0} means the verticle has been deployed successfully
   */
  public int launch(String[] args, VertxApplicationHooks hooks, boolean printUsageOnFailure) {
    VertxApplicationCommand command = new VertxApplicationCommand(this, Objects.requireNonNull(hooks), log);
    CommandLine commandLine = new CommandLine(command)
      .setOptionsCaseInsensitive(true)
      .setExitCodeExceptionMapper(CommandException.EXIT_CODE_EXCEPTION_MAPPER);
    int exitCode = commandLine.execute(args);
    if (exitCode != 0 && printUsageOnFailure) { // Don't print usage if the verticle has been deployed
      CommandLine.usage(command, System.out, Ansi.ON);
    }
    return exitCode;
  }

  /**
   * Processes the exit code returned by {@link #launch(String[], VertxApplicationHooks, boolean)}.
   * <p>
   * Terminates the current JVM with the {@code exitCode} value if it is different from zero.
   *
   * @param exitCode the exit code value
   */
  public void processExitCode(int exitCode) {
    if (exitCode != 0) { // Don't exit if the verticle has been deployed
      System.exit(exitCode);
    }
  }
}
