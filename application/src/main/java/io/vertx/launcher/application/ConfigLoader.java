package io.vertx.launcher.application;

/**
 * SPI for plugging custom configuration sources into the {@link io.vertx.launcher.application.VertxApplication} launch process.
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader} and applied in {@link #order()} sequence
 * to each options object (e.g. {@link io.vertx.core.VertxOptions}, {@link io.vertx.core.DeploymentOptions}).
 * <p>
 * The default implementations load configuration from environment variables (order 1000) and
 * system properties (order 2000). CLI flags always take precedence over all config loaders.
 *
 * <h3>Precedence (lowest to highest)</h3>
 * <ol>
 *   <li>JSON file/string ({@code --options}, {@code --deployment-options})</li>
 *   <li>Config loaders sorted by {@link #order()} (lower = applied first, higher = applied later and wins)</li>
 *   <li>CLI flags ({@code --worker}, {@code --instances}, {@code --cluster-host}, etc.)</li>
 * </ol>
 */
public interface ConfigLoader {

  /**
   * Returns the order of this loader. Loaders are applied from lowest to highest order,
   * so a higher-order loader's values override those set by a lower-order loader.
   * <p>
   * Default loaders use: environment variables = 1000, system properties = 2000.
   *
   * @return the order value
   */
  int order();

  /**
   * Applies configuration to the given target options object.
   *
   * @param target the options object to configure (e.g. {@link io.vertx.core.VertxOptions},
   *               {@link io.vertx.core.eventbus.EventBusOptions}, {@link io.vertx.core.DeploymentOptions},
   *               or {@link io.vertx.core.metrics.MetricsOptions})
   * @param scope  identifies which configuration scope is being applied
   */
  void configure(Object target, ConfigScope scope);

}
