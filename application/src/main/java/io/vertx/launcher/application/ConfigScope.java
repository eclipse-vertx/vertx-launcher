package io.vertx.launcher.application;

/**
 * Identifies the configuration scope being applied by a {@link ConfigLoader}.
 * <p>
 * Each scope corresponds to a specific Vert.x options type.
 */
public enum ConfigScope {

  /**
   * Vert.x core options ({@link io.vertx.core.VertxOptions}).
   */
  VERTX,

  /**
   * Event bus options ({@link io.vertx.core.eventbus.EventBusOptions}), applied when clustering is enabled.
   */
  EVENTBUS,

  /**
   * Deployment options ({@link io.vertx.core.DeploymentOptions}) for the main verticle.
   */
  DEPLOYMENT,

  /**
   * Metrics options ({@link io.vertx.core.metrics.MetricsOptions}), applied when a metrics factory is present.
   */
  METRICS
}
