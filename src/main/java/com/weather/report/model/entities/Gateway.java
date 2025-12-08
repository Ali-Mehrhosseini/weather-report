package com.weather.report.model.entities;

import java.util.Collection;

import com.weather.report.model.Timestamped;

/// A _gateway_ groups multiple devices that monitor the same physical quantity.  
/// 
/// It can be configured through parameters that provide information about its state or values needed for interpreting the measurements.
public class Gateway extends Timestamped {

  public Collection<Parameter> getParameters() {
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
