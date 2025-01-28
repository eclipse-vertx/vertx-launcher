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

import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.spi.VertxMetricsFactory;
import io.vertx.core.spi.metrics.VertxMetrics;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class CustomMetricsFactory implements VertxMetricsFactory {

  public static final VertxMetrics DUMMY = new VertxMetrics() {
  };

  @Override
  public VertxMetrics metrics(VertxOptions options) {
    return DUMMY;
  }

  @Override
  public MetricsOptions newOptions() {
    return new CustomMetricsOptions();
  }

  @Override
  public MetricsOptions newOptions(JsonObject jsonObject) {
    return new CustomMetricsOptions(jsonObject);
  }
}
