package com.weather.report.model.entities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.weather.report.model.Timestamped;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

/// A _gateway_ groups multiple devices that monitor the same physical quantity.  
/// 
/// It can be configured through parameters that provide information about its state or values needed for interpreting the measurements.
@Entity
public class Gateway extends Timestamped {

  @Id
  private String code;
  private String name;
  private String description;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
  @JoinColumn(name = "gateway_code")
  private List<Parameter> parameters = new ArrayList<>();

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "gateway_code")
  private List<Sensor> sensors = new ArrayList<>();

  public Gateway() {
    // JPA compliance
  }

  public Gateway(String code, String name, String description) {
    this.code = code;
    this.name = name;
    this.description = description;
  }

  public Collection<Parameter> getParameters() {
    return parameters;
  }

  public Parameter getParameter(String paramCode) {
    return parameters.stream()
        .filter(p -> paramCode.equals(p.getCode()))
        .findFirst()
        .orElse(null);
  }

  public void addParameter(Parameter parameter) {
    parameters.add(parameter);
  }

  public Collection<Sensor> getSensors() {
    return sensors;
  }

  public void addSensor(Sensor sensor) {
    if (!sensors.contains(sensor)) {
      sensors.add(sensor);
    }
  }

  public void removeSensor(Sensor sensor) {
    sensors.remove(sensor);
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
    Gateway gateway = (Gateway) o;
    return code != null && code.equals(gateway.code);
  }

  @Override
  public int hashCode() {
    return code != null ? code.hashCode() : 0;
  }

}
