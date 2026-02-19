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

package examples;

import io.vertx.core.VertxOptions;
import io.vertx.launcher.application.HookContext;
import io.vertx.launcher.application.VertxApplication;
import io.vertx.launcher.application.VertxApplicationHooks;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class Examples {

  public void hooks(String[] args) {
    VertxApplicationHooks hooks = new VertxApplicationHooks() {
      @Override
      public void beforeStartingVertx(HookContext context) {
        VertxOptions vertxOptions = context.vertxOptions();
        // You could customize the metrics/tracer options here
      }

      @Override
      public void afterVerticleDeployed(HookContext context) {
        System.out.println("Hooray!");
      }
    };
    VertxApplication app = new VertxApplication(args, hooks);
    app.launch();
  }

  public void customEnvironment(String[] args) {
    VertxApplicationHooks hooks = new VertxApplicationHooks() {
      @Override
      public Map<String, String> getEnvironmentVariables() {
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("VERTX_OPTIONS_EVENT_LOOP_POOL_SIZE", "8");
        return env;
      }
    };
    VertxApplication app = new VertxApplication(args, hooks);
    app.launch();
  }
}
