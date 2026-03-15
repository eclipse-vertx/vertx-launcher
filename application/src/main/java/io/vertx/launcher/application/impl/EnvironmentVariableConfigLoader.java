package io.vertx.launcher.application.impl;

import io.vertx.launcher.application.ConfigScope;

import java.util.EnumMap;
import java.util.Map;

public class EnvironmentVariableConfigLoader extends AbstractConfigLoader {

  private static final EnumMap<ConfigScope, String> PREFIXES = new EnumMap<>(Map.of(
    ConfigScope.VERTX, "VERTX_OPTIONS_",
    ConfigScope.EVENTBUS, "VERTX_EVENTBUS_OPTIONS_",
    ConfigScope.DEPLOYMENT, "VERTX_DEPLOYMENT_OPTIONS_",
    ConfigScope.METRICS, "VERTX_METRICS_OPTIONS_"
  ));

  @Override
  public int order() {
    return 1000;
  }

  @Override
  public void configure(Object target, ConfigScope scope) {
    String prefix = PREFIXES.get(scope);
    if (prefix == null) {
      return;
    }
    for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
      String envName = entry.getKey();
      if (envName.startsWith(prefix)) {
        String fieldName = envName.substring(prefix.length()).replace("_", "").toLowerCase();
        configureOption(target, fieldName, entry.getValue());
      }
    }
  }
}
