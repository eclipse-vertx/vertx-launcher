/*
 * Copyright (c) 2011-2019 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */

package io.vertx.core.impl.launcher.commands;

import io.vertx.core.cli.CLIException;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.spi.launcher.DefaultCommand;

/**
 * Comment to display the vert.x (core) version.
 *
 * @author Clement Escoffier <clement@apache.org>
 */
@Name("version")
@Summary("Displays the version.")
@Description("Prints the vert.x core version used by the application.")
public class VersionCommand extends DefaultCommand {

  private static final Logger log = LoggerFactory.getLogger(VersionCommand.class);

  @Override
  public void run() throws CLIException {
    log.info(getVersion());
  }

  /**
   * Reads the version from the {@code vertx-version.txt} file.
   *
   * @return the version
   */
  public static String getVersion() {
    return VertxInternal.version();
  }
}
