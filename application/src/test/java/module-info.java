/*
 * Copyright (c) 2011-2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */

open module io.vertx.launcher.application.tests {
  requires io.vertx.core;
  requires io.vertx.launcher.application;

  requires org.junit.jupiter.api;

  // Only required for compilation
  requires static io.vertx.core.tests;
  requires static awaitility;
}
