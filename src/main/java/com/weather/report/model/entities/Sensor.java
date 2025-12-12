package com.weather.report.model.entities;

import com.weather.report.model.Timestamped;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

/// A _sensor_ measures a physical quantity and periodically sends the corresponding measurements.
/// 
/// A sensor may have a _threshold_ defined by the user to detect anomalous behaviours.
@Entity
public class Sensor extends Timestamped {

  @Id
  private String code;
  private String name;
  private String description;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "threshold_id")
  private Threshold threshold;

  public Sensor() {
    // JPA compliance
  }

  public Sensor(String code, String name, String description) {
    this.code = code;
    this.name = name;
    this.description = description;
  }

  public Threshold getThreshold() {
    return threshold;
  }

  public void setThreshold(Threshold threshold) {
    this.threshold = threshold;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Sensor sensor = (Sensor) o;
    return code != null && code.equals(sensor.code);
  }

  @Override
  public int hashCode() {
    return code != null ? code.hashCode() : 0;
  }

}
