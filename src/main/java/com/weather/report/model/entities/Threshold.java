package com.weather.report.model.entities;

import com.weather.report.model.ThresholdType;

/// A _threshold_ defines an acceptable limit for the values measured by a sensor.
/// 
/// It **always** consists of a numeric value and a 
/// [ThresholdType][com.weather.report.model.ThresholdType] that the system must apply to decide whether a measurement is anomalous.
public class Threshold {

  public double getValue() {
    return -1;
  }

  public ThresholdType getType() {
    return null;
  }

}
