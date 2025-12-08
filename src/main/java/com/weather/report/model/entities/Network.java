package com.weather.report.model.entities;

import java.util.Collection;

import com.weather.report.model.Timestamped;

/// A _monitoring network_ that represents a logical set of system elements.
/// 
/// It may have a list of _operators_ responsible for receiving notifications.
public class Network extends Timestamped {

  public Collection<Operator> getOperators() {
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
