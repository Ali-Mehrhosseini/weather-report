package com.weather.report.model.entities;

import com.weather.report.model.Timestamped;

/// A _sensor_ measures a physical quantity and periodically sends the corresponding measurements.
/// 
/// A sensor may have a _threshold_ defined by the user to detect anomalous behaviours.
public class Sensor extends Timestamped {

  public Threshold getThreshold() {
    return null;
  }

  public String getCode() {
    return null;
  }

  public String getName() {
    return null;
  }

  public String getDescription() {
    return null;
  }

}
