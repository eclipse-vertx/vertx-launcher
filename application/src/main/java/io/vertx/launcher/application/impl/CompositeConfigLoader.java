package io.vertx.launcher.application.impl;

import io.vertx.launcher.application.ConfigLoader;
import io.vertx.launcher.application.ConfigScope;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

public class CompositeConfigLoader {

  private final List<ConfigLoader> loaders;

  public CompositeConfigLoader() {
    List<ConfigLoader> list = new ArrayList<>();
    for (ConfigLoader loader : ServiceLoader.load(ConfigLoader.class, Thread.currentThread().getContextClassLoader())) {
      list.add(loader);
    }
    list.sort(Comparator.comparingInt(ConfigLoader::order));
    this.loaders = List.copyOf(list);
  }

  public void apply(Object target, ConfigScope scope) {
    for (ConfigLoader loader : loaders) {
      loader.configure(target, scope);
    }
  }
}
