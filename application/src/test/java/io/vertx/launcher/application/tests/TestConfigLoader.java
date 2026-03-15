package io.vertx.launcher.application.tests;

import io.vertx.core.VertxOptions;
import io.vertx.launcher.application.ConfigLoader;
import io.vertx.launcher.application.ConfigScope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestConfigLoader implements ConfigLoader {

  static volatile boolean enabled;
  static final List<ConfigScope> invocations = Collections.synchronizedList(new ArrayList<>());
  static volatile int eventLoopPoolSize = -1;

  static void reset() {
    enabled = false;
    invocations.clear();
    eventLoopPoolSize = -1;
  }

  @Override
  public int order() {
    return 1500;
  }

  @Override
  public void configure(Object target, ConfigScope scope) {
    if (!enabled) {
      return;
    }
    invocations.add(scope);
    if (eventLoopPoolSize > 0 && scope == ConfigScope.VERTX && target instanceof VertxOptions) {
      ((VertxOptions) target).setEventLoopPoolSize(eventLoopPoolSize);
    }
  }
}
