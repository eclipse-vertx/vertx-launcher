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

/**
 * An exception than can be thrown to interrupt the processing of {@link VertxApplicationCommand} and specify which exit code should be used.
 */
public class CommandException extends VertxException {

  private final int code;

  public CommandException(int code) {
    super((String) null, true);
    this.code = code;
  }

  public int getCode() {
    return code;
  }
}
