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

package io.vertx.launcher.application.impl;

import io.vertx.core.*;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.internal.VertxInternal;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.spi.VertxMetricsFactory;
import io.vertx.core.spi.VertxServiceProvider;
import io.vertx.core.spi.VertxTracerFactory;
import io.vertx.core.tracing.TracingOptions;
import io.vertx.launcher.application.ExitCodes;
import io.vertx.launcher.application.VertxApplication;
import io.vertx.launcher.application.VertxApplicationHooks;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static io.vertx.launcher.application.impl.Utils.*;
import static java.lang.Boolean.TRUE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static picocli.CommandLine.Parameters.NULL_VALUE;

@Command(name = "VertxApplication", description = "Runs a Vert.x application.", sortOptions = false)
public class VertxApplicationCommand implements Runnable {

  private static final String VERTX_OPTIONS_PROP_PREFIX = "vertx.options.";
  private static final String VERTX_EVENTBUS_PROP_PREFIX = "vertx.eventBus.options.";
  private static final String DEPLOYMENT_OPTIONS_PROP_PREFIX = "vertx.deployment.options.";
  private static final String METRICS_OPTIONS_PROP_PREFIX = "vertx.metrics.options.";

  @Option(
    names = {"-options", "--options", "-vertx-options", "--vertx-options"},
    description = {
      "Specifies the Vert.x options.",
      "It should reference either a JSON file which represents the options OR be a JSON string."
    },
    defaultValue = Option.NULL_VALUE
  )
  @SuppressWarnings("unused")
  private String vertxOptionsStr;

  @Option(
    names = {"-c", "-cluster", "--cluster"},
    description = {
      "If specified, then the Vert.x instance will form a cluster with any other Vert.x instances on the network."
    },
    arity = "0"
  )
  @SuppressWarnings("unused")
  private Boolean clustered;
  @Option(
    names = {"-cluster-port", "--cluster-port"},
    description = {
      "Port to use for cluster communication.",
      "By default, a spare random port is chosen."
    }
  )
  @SuppressWarnings("unused")
  private Integer clusterPort;
  @Option(
    names = {"-cluster-host", "--cluster-host"},
    description = {
      "Host to bind to for cluster communication.",
      "If this is not specified, Vert.x will attempt to choose one from the available interfaces."
    }
  )
  @SuppressWarnings("unused")
  private String clusterHost;
  @Option(
    names = {"-cluster-public-port", "--cluster-public-port"},
    description = {
      "Public port to use for cluster communication.",
      "By default, Vert.x uses the same as the cluster port."
    }
  )
  @SuppressWarnings("unused")
  private Integer clusterPublicPort;
  @Option(
    names = {"-cluster-public-host", "--cluster-public-host"},
    description = {
      "Public host to bind to for cluster communication.",
      "By default, Vert.x uses the same as the cluster host."
    }
  )
  @SuppressWarnings("unused")
  private String clusterPublicHost;

  @Option(
    names = {"-deployment-options", "--deployment-options"},
    description = {
      "Specifies the main verticle deployment options."
    }
  )
  @SuppressWarnings("unused")
  private String deploymentOptionsStr;

  @Option(
    names = {"-w", "-worker", "--worker"},
    description = {
      "If specified, then the main verticle is deployed with the worker threading model.",
      "Takes precedence over the value defined in deployment options.",
    },
    arity = "0"
  )
  @SuppressWarnings("unused")
  private Boolean worker;
  @Option(
    names = {"-vt", "-virtual-thread", "--virtual-thread"},
    description = {
      "If specified, then the main verticle is deployed with the virtual thread threading model.",
      "Takes precedence over the value defined in deployment options.",
    },
    arity = "0"
  )
  @SuppressWarnings("unused")
  private Boolean virtualThread;
  @Option(
    names = {"-instances", "--instances"},
    description = {
      "Specifies how many instances of the verticle will be deployed.",
      "Takes precedence over the value defined in deployment options."
    }
  )
  @SuppressWarnings("unused")
  private Integer instances;

  @Option(
    names = {"-conf", "--conf"},
    description = {
      "Specifies configuration that should be provided to the verticle.",
      "It should reference either a JSON file which represents the options OR be a JSON string."
    }
  )
  @SuppressWarnings("unused")
  private String configStr;

  @Option(
    names = {"-h", "-help", "--help"},
    usageHelp = true,
    description = {
      "Display a help message."
    },
    arity = "0")
  @SuppressWarnings("unused")
  private boolean helpRequested;

  @Parameters(
    index = "0",
    description = {
      "The main verticle fully qualified class name."
    },
    defaultValue = NULL_VALUE
  )
  @SuppressWarnings("unused")
  private String mainVerticle;

  private final VertxApplication vertxApplication;
  private final VertxApplicationHooks hooks;
  private final Logger log;
  private final HookContextImpl hookContext = new HookContextImpl();

  private volatile VertxInternal vertx;

  public VertxApplicationCommand(VertxApplication vertxApplication, VertxApplicationHooks hooks, Logger log) {
    this.vertxApplication = vertxApplication;
    this.hooks = hooks;
    this.log = log;
  }

  @Override
  public void run() {
    JsonObject optionsParam = hooks.afterVertxOptionsParsed(readJsonFileOrString(log, "options", vertxOptionsStr));
    JsonObject deploymentOptionsParam = hooks.afterDeploymentOptionsParsed(readJsonFileOrString(log, "deploymentOptions", deploymentOptionsStr));
    JsonObject conf = hooks.afterConfigParsed(readJsonFileOrString(log, "conf", configStr));

    VertxOptions options;
    if (optionsParam != null) {
      options = new VertxOptions(optionsParam);
    } else {
      options = new VertxOptions();
    }
    VertxBuilder builder = hooks.createVertxBuilder(options);
    processVertxOptions(options, optionsParam);

    hookContext.setVertxOptions(options);
    hooks.beforeStartingVertx(hookContext);
    vertx = (VertxInternal) withTCCLAwait(() -> createVertx(builder), Duration.ofMinutes(2), "startup", VertxApplicationHooks::afterFailureToStartVertx, ExitCodes.VERTX_INITIALIZATION);
    hookContext.setVertx(vertx);
    hooks.afterVertxStarted(hookContext);

    vertx.addCloseHook(this::beforeStoppingVertx);
    Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook(vertx, this::afterShutdownHookExecuted)));

    DeploymentOptions deploymentOptions = createDeploymentOptions(deploymentOptionsParam, conf);

    Supplier<Future<String>> deployer;
    Supplier<? extends Deployable> verticleSupplier = hooks.verticleSupplier();
    if (verticleSupplier == null) {
      String verticleName = computeVerticleName(vertxApplication.getClass(), mainVerticle);
      if (verticleName == null) {
        log.error("If the <mainVerticle> parameter is not provided, the 'Main-Verticle' manifest attribute must be provided.");
        throw new CommandException(ExitCodes.VERTX_DEPLOYMENT);
      }
      deployer = () -> vertx.deployVerticle(verticleName, deploymentOptions);
      hookContext.readyToDeploy(verticleName, deploymentOptions);
    } else {
      deployer = () -> vertx.deployVerticle(verticleSupplier, deploymentOptions);
      hookContext.readyToDeploy(null, deploymentOptions);
    }

    hooks.beforeDeployingVerticle(hookContext);
    String message = hookContext.deploymentOptions().getThreadingModel() == ThreadingModel.WORKER ? "deploying worker verticle" : "deploying verticle";
    String deploymentId = withTCCLAwait(deployer, Duration.ofMinutes(2), message, VertxApplicationHooks::afterFailureToDeployVerticle, ExitCodes.VERTX_DEPLOYMENT);
    log.info("Succeeded in " + message);
    hookContext.setDeploymentId(deploymentId);
    hooks.afterVerticleDeployed(hookContext);
  }

  private void processVertxOptions(VertxOptions vertxOptions, JsonObject optionsJson) {
    if (clustered == TRUE) {
      EventBusOptions eventBusOptions = vertxOptions.getEventBusOptions();
      if (clusterHost != null) {
        eventBusOptions.setHost(clusterHost);
      }
      if (clusterPort != null) {
        eventBusOptions.setPort(clusterPort);
      }
      if (clusterPublicHost != null) {
        eventBusOptions.setClusterPublicHost(clusterPublicHost);
      }
      if (clusterPublicPort != null) {
        eventBusOptions.setClusterPublicPort(clusterPublicPort);
      }
      configureFromSystemProperties(log, eventBusOptions, VERTX_EVENTBUS_PROP_PREFIX);
    }
    configureFromSystemProperties(log, vertxOptions, VERTX_OPTIONS_PROP_PREFIX);
    VertxMetricsFactory metricsFactory = findServiceProvider(VertxMetricsFactory.class);
    if (metricsFactory != null) {
      MetricsOptions metricsOptions;
      if (optionsJson != null && optionsJson.containsKey("metricsOptions")) {
        metricsOptions = metricsFactory.newOptions(optionsJson.getJsonObject("metricsOptions"));
      } else {
        metricsOptions = vertxOptions.getMetricsOptions();
        if (metricsOptions == null) {
          metricsOptions = metricsFactory.newOptions();
        } else {
          metricsOptions = metricsFactory.newOptions(metricsOptions);
        }
      }
      configureFromSystemProperties(log, metricsOptions, METRICS_OPTIONS_PROP_PREFIX);
      vertxOptions.setMetricsOptions(metricsOptions);
    }
    VertxTracerFactory tracerFactory = findServiceProvider(VertxTracerFactory.class);
    if (tracerFactory != null) {
      if (optionsJson != null && optionsJson.containsKey("tracingOptions")) {
        TracingOptions tracingOptions = tracerFactory.newOptions(optionsJson.getJsonObject("tracingOptions"));
        vertxOptions.setTracingOptions(tracingOptions);
      }
    }
  }

  private <SP> SP findServiceProvider(Class<SP> serviceProviderClass) {
    List<SP> serviceProviders = new ArrayList<>();
    for (VertxServiceProvider vsp : ServiceLoader.load(VertxServiceProvider.class)) {
      if (serviceProviderClass.isAssignableFrom(vsp.getClass())) {
        serviceProviders.add(serviceProviderClass.cast(vsp));
      }
    }
    if (serviceProviders.isEmpty()) {
      return null;
    }
    if (serviceProviders.size() != 1) {
      log.warn("Cannot convert options, there are several implementations of " + serviceProviderClass);
      return null;
    }
    return serviceProviders.get(0);
  }

  private DeploymentOptions createDeploymentOptions(JsonObject deploymentOptionsParam, JsonObject confParam) {
    DeploymentOptions deploymentOptions = deploymentOptionsParam != null ? new DeploymentOptions(deploymentOptionsParam) : new DeploymentOptions();
    if (worker == TRUE) {
      if (virtualThread == TRUE) {
        log.error("Cannot choose the threading model, the virtual thread and worker options are both set.");
        throw new CommandException(ExitCodes.VERTX_DEPLOYMENT);
      }
      deploymentOptions.setThreadingModel(ThreadingModel.WORKER);
    } else if (virtualThread == TRUE) {
      deploymentOptions.setThreadingModel(ThreadingModel.VIRTUAL_THREAD);
    }
    if (instances != null) {
      deploymentOptions.setInstances(instances);
    }
    if (confParam != null) {
      deploymentOptions.setConfig(confParam);
    } else {
      deploymentOptions.setConfig(new JsonObject());
    }
    configureFromSystemProperties(log, deploymentOptions, DEPLOYMENT_OPTIONS_PROP_PREFIX);
    return deploymentOptions;
  }

  private Future<Vertx> createVertx(VertxBuilder builder) {
    try {
      if (clustered == TRUE) {
        log.info("Starting clustering...");
        return builder.buildClustered().onFailure(t -> {
          log.error("Failed to form cluster", t);
        });
      } else {
        return Future.succeededFuture(builder.build());
      }
    } catch (Exception e) {
      log.error("Failed to create the Vert.x instance", e);
      return Future.failedFuture(e);
    }
  }

  private <T> T withTCCLAwait(Supplier<Future<T>> supplier, Duration duration, String logMessage, FailureHook failureHook, int exitCode) {
    ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      CompletableFuture<T> future = supplier.get().toCompletionStage().toCompletableFuture();
      return future.get(duration.toMillis(), MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Thread interrupted in " + logMessage);
      failureHook.invokeHook(hooks, hookContext, e);
      throw new CommandException(exitCode);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      failureHook.invokeHook(hooks, hookContext, cause);
      log.error("Failed in " + logMessage, cause);
      throw new CommandException(exitCode);
    } catch (TimeoutException e) {
      log.error("Timed out in " + logMessage);
      failureHook.invokeHook(hooks, hookContext, null);
      throw new CommandException(exitCode);
    } finally {
      Thread.currentThread().setContextClassLoader(originalClassLoader);
    }
  }

  private void beforeStoppingVertx(Completable<Void> promise) {
    try {
      hooks.beforeStoppingVertx(hookContext);
      promise.succeed();
    } catch (Exception e) {
      promise.fail(e);
    }
  }

  private void afterShutdownHookExecuted(AsyncResult<Void> ar) {
    if (ar == null) {
      log.error("Timed out waiting for Vert.x to be closed");
      hooks.afterFailureToStopVertx(hookContext, null);
    } else if (ar.failed()) {
      log.error("Failure in stopping Vert.x", ar.cause());
      hooks.afterFailureToStopVertx(hookContext, ar.cause());
    } else {
      hooks.afterVertxStopped(hookContext);
    }
  }
}
