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

package io.vertx.launcher.application.impl;

import io.vertx.core.VertxException;
import picocli.CommandLine.IExitCodeExceptionMapper;

public class CommandException extends VertxException {

  public static final IExitCodeExceptionMapper EXIT_CODE_EXCEPTION_MAPPER = t -> {
    if (t instanceof CommandException) {
      CommandException commandException = (CommandException) t;
      return commandException.code;
    }
    return 1;
  };

  private final int code;

  public CommandException(int code) {
    super((String) null, true);
    this.code = code;
  }
}
