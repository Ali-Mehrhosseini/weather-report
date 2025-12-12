package com.weather.report.model.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/// An _operator_ is an entity that receives notifications when a threshold violation is detected.  
@Entity
public class Operator {

  @Id
  private String email;
  private String firstName;
  private String lastName;
  private String phoneNumber;

  public Operator() {
    // JPA compliance
  }

  public Operator(String firstName, String lastName, String email, String phoneNumber) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.phoneNumber = phoneNumber;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getEmail() {
    return email;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

}
