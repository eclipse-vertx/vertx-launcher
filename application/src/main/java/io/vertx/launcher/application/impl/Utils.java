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

import io.vertx.core.buffer.Buffer;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class Utils {

  public static JsonObject readJsonFileOrString(Logger log, String optionName, String jsonFileOrString) {
    if (jsonFileOrString == null) {
      return null;
    }
    try {
      Path path = Paths.get(jsonFileOrString);
      byte[] bytes = Files.readAllBytes(path);
      return new JsonObject(Buffer.buffer(bytes));
    } catch (InvalidPathException | IOException | DecodeException ignored) {
    }
    try {
      return new JsonObject(jsonFileOrString);
    } catch (DecodeException ignored) {
    }
    log.warn("The " + optionName + " option does not point to an valid JSON file or is not a valid JSON object.");
    return null;
  }

  public static String computeVerticleName(Class<?> mainClass, String mainVerticle) {
    List<String> attributeNames = Arrays.asList("Main-Verticle", "Default-Verticle-Factory");
    Map<String, String> manifestAttributes;
    try {
      manifestAttributes = getAttributesFromManifest(mainClass, attributeNames);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    String mainVerticleAttribute = manifestAttributes.get("Main-Verticle");
    String defaultVerticleFactory = manifestAttributes.get("Default-Verticle-Factory");
    String verticleName = mainVerticle != null ? mainVerticle : mainVerticleAttribute;
    if (defaultVerticleFactory != null && verticleName != null && verticleName.indexOf(':') == -1) {
      verticleName = defaultVerticleFactory + ":" + verticleName;
    }
    return verticleName;
  }

  private static Map<String, String> getAttributesFromManifest(Class<?> mainClass, List<String> attributeNames) throws IOException {
    Enumeration<URL> resources = Utils.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
    while (resources.hasMoreElements()) {
      try (InputStream stream = resources.nextElement().openStream()) {
        Manifest manifest = new Manifest(stream);
        Attributes attributes = manifest.getMainAttributes();
        String mainClassAttributeValue = attributes.getValue("Main-Class");
        if (mainClass.getName().equals(mainClassAttributeValue)) {
          Map<String, String> map = new HashMap<>();
          for (String attributeName : attributeNames) {
            String attributeValue = attributes.getValue(attributeName);
            if (attributeValue != null) {
              map.put(attributeName, attributeValue);
            }
          }
          return Collections.unmodifiableMap(map);
        }
      }
    }
    return Collections.emptyMap();
  }

  private Utils() {
    // Utility
  }
}
