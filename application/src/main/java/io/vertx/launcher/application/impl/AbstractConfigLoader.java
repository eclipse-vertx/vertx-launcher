package io.vertx.launcher.application.impl;

import io.vertx.core.VertxException;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.launcher.application.ConfigLoader;

import java.lang.reflect.Method;

public abstract class AbstractConfigLoader implements ConfigLoader {

  protected final Logger log = LoggerFactory.getLogger(getClass());

  protected void configureOption(Object options, String fieldName, String value) {
    Method setter = getSetter(fieldName, options.getClass());
    if (setter == null) {
      log.warn("No such property to configure on options: " + options.getClass().getName() + "." + fieldName);
      return;
    }
    Class<?> argType = setter.getParameterTypes()[0];
    Object arg;
    try {
      if (argType.equals(String.class)) {
        arg = value;
      } else if (argType.equals(int.class)) {
        arg = Integer.valueOf(value);
      } else if (argType.equals(long.class)) {
        arg = Long.valueOf(value);
      } else if (argType.equals(boolean.class)) {
        arg = Boolean.valueOf(value);
      } else if (argType.isEnum()) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Object enumVal = Enum.valueOf((Class<? extends Enum>) argType, value);
        arg = enumVal;
      } else {
        log.warn("Invalid type for setter: " + argType);
        return;
      }
    } catch (IllegalArgumentException e) {
      log.warn("Invalid argtype:" + argType + " on options: " + options.getClass().getName() + "." + fieldName);
      return;
    }
    try {
      setter.invoke(options, arg);
    } catch (Exception ex) {
      throw new VertxException("Failed to invoke setter: " + setter, ex);
    }
  }

  private static Method getSetter(String fieldName, Class<?> clazz) {
    Method[] meths = clazz.getDeclaredMethods();
    for (Method meth : meths) {
      if (("set" + fieldName).equalsIgnoreCase(meth.getName())) {
        return meth;
      }
    }

    // This set contains the overridden methods
    meths = clazz.getMethods();
    for (Method meth : meths) {
      if (("set" + fieldName).equalsIgnoreCase(meth.getName())) {
        return meth;
      }
    }

    return null;
  }
}
