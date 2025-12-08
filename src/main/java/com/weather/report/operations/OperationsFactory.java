package com.weather.report.operations;

/**
 * Central factory providing concrete implementations of the operations
 * interfaces.
 * {@link com.weather.report.WeatherReport} delegates to these methods to obtain
 * the correct instances for requirements R1-R4.
 */
public final class OperationsFactory {

  private OperationsFactory() {
    // utility class
  }

  /**
   * @return implementation of {@link NetworkOperations} configured for R1/R4
   */
  public static NetworkOperations getNetworkOperations() {
    return null;
  }

  /**
   * @return implementation of {@link GatewayOperations} configured for R2/R4
   */
  public static GatewayOperations getGatewayOperations() {
    return null;
  }

  /**
   * @return implementation of {@link SensorOperations} configured for R3/R4
   */
  public static SensorOperations getSensorOperations() {
    return null;
  }

  /**
   * @return implementation of {@link TopologyOperations} configured for R4
   */
  public static TopologyOperations getTopologyOperations() {
    return null;
  }

}
