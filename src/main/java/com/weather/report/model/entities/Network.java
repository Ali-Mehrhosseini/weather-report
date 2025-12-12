package com.weather.report.model.entities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.weather.report.model.Timestamped;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;

/// A _monitoring network_ that represents a logical set of system elements.
/// 
/// It may have a list of _operators_ responsible for receiving notifications.
@Entity
public class Network extends Timestamped {

  @Id
  private String code;
  private String name;
  private String description;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
    name = "network_operator",
    joinColumns = @JoinColumn(name = "network_code"),
    inverseJoinColumns = @JoinColumn(name = "operator_email")
  )
  private List<Operator> operators = new ArrayList<>();

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "network_code")
  private List<Gateway> gateways = new ArrayList<>();

  public Network() {
    // JPA compliance
  }

  public Network(String code, String name, String description) {
    this.code = code;
    this.name = name;
    this.description = description;
  }

  public Collection<Operator> getOperators() {
    return operators;
  }

  public void addOperator(Operator operator) {
    if (!operators.contains(operator)) {
      operators.add(operator);
    }
  }

  public Collection<Gateway> getGateways() {
    return gateways;
  }

  public void addGateway(Gateway gateway) {
    if (!gateways.contains(gateway)) {
      gateways.add(gateway);
    }
  }

  public void removeGateway(Gateway gateway) {
    gateways.remove(gateway);
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

}
