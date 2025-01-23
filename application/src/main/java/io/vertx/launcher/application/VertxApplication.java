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

import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.launcher.application.impl.CommandExceptionHandler;
import io.vertx.launcher.application.impl.VertxApplicationCommand;
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

  private final String[] args;
  private final VertxApplicationHooks hooks;
  private final boolean printUsageOnFailure;
  private final boolean exitOnFailure;

  public static void main(String[] args) {
    VertxApplication vertxApplication = new VertxApplication(args);
    vertxApplication.launch();
  }

  /**
   * Create a new instance with the given program arguments and default behavior (print usage and exit on failure).
   * <p>
   * Subclasses may implement {@link VertxApplicationHooks}.
   * In this case, this object's hook methods will be invoked at different stages of the launch process.
   *
   * @param args the program arguments
   */
  public VertxApplication(String[] args) {
    this(args, null);
  }

  /**
   * Like {@link #VertxApplication(String[])}, with the provided {@code hooks}.
   *
   * @param args  the program arguments
   * @param hooks an instance of {@link VertxApplicationHooks} to be invoked at different stages of the launch process
   */
  public VertxApplication(String[] args, VertxApplicationHooks hooks) {
    this(args, hooks, true, true);
  }

  /**
   * May be invoked by subclasses to customize behavior.
   * <p>
   * When the {@code hooks} parameter is {@code null}, the application instance will be used if it implements {@link  VertxApplicationHooks}.
   *
   * @param args                the program arguments
   * @param hooks               an instance of {@link VertxApplicationHooks} to be invoked at different stages of the launch process (maybe null)
   * @param printUsageOnFailure whether usage should be printed to {@link System#out} if the application failed to start
   * @param exitOnFailure       whether the JVM should be terminated with a specific {@code exitCode} if the application failed to start
   */
  protected VertxApplication(String[] args, VertxApplicationHooks hooks, boolean printUsageOnFailure, boolean exitOnFailure) {
    this.args = Objects.requireNonNull(args);
    if (hooks == null) {
      if (this instanceof VertxApplicationHooks) {
        this.hooks = (VertxApplicationHooks) this;
      } else {
        this.hooks = VertxApplicationHooks.DEFAULT;
      }
    } else {
      this.hooks = hooks;
    }
    this.printUsageOnFailure = printUsageOnFailure;
    this.exitOnFailure = exitOnFailure;
  }

  /**
   * Launches the Vert.x application.
   *
   * @return an exit code, {@code 0} means the verticle has been deployed successfully
   */
  public int launch() {
    VertxApplicationCommand command = new VertxApplicationCommand(this, Objects.requireNonNull(hooks), log);
    CommandLine commandLine = new CommandLine(command)
      .setOptionsCaseInsensitive(true)
      .setExecutionExceptionHandler(new CommandExceptionHandler());
    int exitCode = commandLine.execute(args);
    if (exitCode == ExitCodes.USAGE && printUsageOnFailure) {
      CommandLine.usage(command, System.out, Ansi.ON);
    }
    if (exitOnFailure) {
      System.exit(exitCode);
    }
    return exitCode;
  }
}
