package com.weather.report.model.entities;

/// A _parameter_ is a value associated with the gateway it belongs to.
/// 
/// It allows storing state or configuration information.
public class Parameter {

  public static final String EXPECTED_MEAN_CODE = "EXPECTED_MEAN";
  public static final String EXPECTED_STD_DEV_CODE = "EXPECTED_STD_DEV";
  public static final String BATTERY_CHARGE_PERCENTAGE_CODE = "BATTERY_CHARGE";

  public String getCode() {
    return null;
  }

  public String getName() {
    return null;
  }

  public String getDescription() {
    return null;
  }

  public double getValue() {
    return -1;
  }

}
