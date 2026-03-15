package io.vertx.launcher.application.impl;

import io.vertx.launcher.application.ConfigScope;

import java.util.EnumMap;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

public class SystemPropertyConfigLoader extends AbstractConfigLoader {

  private static final EnumMap<ConfigScope, String> PREFIXES = new EnumMap<>(Map.of(
    ConfigScope.VERTX, "vertx.options.",
    ConfigScope.EVENTBUS, "vertx.eventBus.options.",
    ConfigScope.DEPLOYMENT, "vertx.deployment.options.",
    ConfigScope.METRICS, "vertx.metrics.options."
  ));

  @Override
  public int order() {
    return 2000;
  }

  @Override
  public void configure(Object target, ConfigScope scope) {
    String prefix = PREFIXES.get(scope);
    if (prefix == null) {
      return;
    }
    Properties props = System.getProperties();
    Enumeration<?> e = props.propertyNames();
    while (e.hasMoreElements()) {
      String propName = (String) e.nextElement();
      if (propName.startsWith(prefix)) {
        String fieldName = propName.substring(prefix.length());
        configureOption(target, fieldName, props.getProperty(propName));
      }
    }
  }
}
