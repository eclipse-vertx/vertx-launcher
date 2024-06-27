package io.vertx.test;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TestVerticle extends AbstractVerticle {

  public static AtomicInteger instanceCount = new AtomicInteger();
  public static List<String> processArgs;
  public static JsonObject conf;

  public TestVerticle() {
  }

  @Override
  public void start() throws Exception {
    processArgs = context.processArgs();
    conf = context.config();
//    if (Thread.currentThread().getContextClassLoader() != getClass().getClassLoader()) {
//      throw new IllegalStateException("Wrong tccl!");
//    }
    vertx.eventBus().send("testcounts",
      new JsonObject().put("deploymentID", context.deploymentID()).put("count", instanceCount.incrementAndGet()));
  }

  @Override
  public void stop() throws Exception {
  }
}
