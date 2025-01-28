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

package io.vertx.launcher.application.tests;

import io.vertx.launcher.application.VertxApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ModularityITest {

  private final TestHooks hooks = new TestHooks();

  @AfterEach
  public void tearDown() {
    if (hooks.vertx != null) {
      hooks.vertx.close().await();
    }
  }

  @Test
  void shouldDeployVerticleOfModule() {
    VertxApplication vertxApplication = new VertxApplication(new String[]{"io.vertx.launcher.application.tests.HttpTestVerticle"}, hooks);
    assertEquals(0, vertxApplication.launch());
  }
}
