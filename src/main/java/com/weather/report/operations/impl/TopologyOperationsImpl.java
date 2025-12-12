package com.weather.report.operations.impl;

import java.util.ArrayList;
import java.util.Collection;

import com.weather.report.exceptions.ElementNotFoundException;
import com.weather.report.exceptions.InvalidInputDataException;
import com.weather.report.exceptions.UnauthorizedException;
import com.weather.report.model.UserType;
import com.weather.report.model.entities.Gateway;
import com.weather.report.model.entities.Network;
import com.weather.report.model.entities.Sensor;
import com.weather.report.model.entities.User;
import com.weather.report.operations.TopologyOperations;
import com.weather.report.repositories.CRUDRepository;

public class TopologyOperationsImpl implements TopologyOperations {

  private final CRUDRepository<Network, String> networkRepository = new CRUDRepository<>(Network.class);
  private final CRUDRepository<Gateway, String> gatewayRepository = new CRUDRepository<>(Gateway.class);
  private final CRUDRepository<Sensor, String> sensorRepository = new CRUDRepository<>(Sensor.class);
  private final CRUDRepository<User, String> userRepository = new CRUDRepository<>(User.class);

  @Override
  public Collection<Gateway> getNetworkGateways(String networkCode)
      throws InvalidInputDataException, ElementNotFoundException {
    if (networkCode == null) {
      throw new InvalidInputDataException("Network code is mandatory");
    }

    Network network = networkRepository.read(networkCode);
    if (network == null) {
      throw new ElementNotFoundException("Network with code " + networkCode + " not found");
    }

    return new ArrayList<>(network.getGateways());
  }

  @Override
  public Network connectGateway(String networkCode, String gatewayCode, String username)
      throws ElementNotFoundException, UnauthorizedException, InvalidInputDataException {
    validateMaintainer(username);

    if (networkCode == null) {
      throw new InvalidInputDataException("Network code is mandatory");
    }
    if (gatewayCode == null) {
      throw new InvalidInputDataException("Gateway code is mandatory");
    }

    Network network = networkRepository.read(networkCode);
    if (network == null) {
      throw new ElementNotFoundException("Network with code " + networkCode + " not found");
    }

    Gateway gateway = gatewayRepository.read(gatewayCode);
    if (gateway == null) {
      throw new ElementNotFoundException("Gateway with code " + gatewayCode + " not found");
    }

    network.addGateway(gateway);
    return networkRepository.update(network);
  }

  @Override
  public Network disconnectGateway(String networkCode, String gatewayCode, String username)
      throws ElementNotFoundException, UnauthorizedException, InvalidInputDataException {
    validateMaintainer(username);

    if (networkCode == null) {
      throw new InvalidInputDataException("Network code is mandatory");
    }
    if (gatewayCode == null) {
      throw new InvalidInputDataException("Gateway code is mandatory");
    }

    Network network = networkRepository.read(networkCode);
    if (network == null) {
      throw new ElementNotFoundException("Network with code " + networkCode + " not found");
    }

    Gateway gateway = gatewayRepository.read(gatewayCode);
    if (gateway == null) {
      throw new ElementNotFoundException("Gateway with code " + gatewayCode + " not found");
    }

    network.removeGateway(gateway);
    return networkRepository.update(network);
  }

  @Override
  public Collection<Sensor> getGatewaySensors(String gatewayCode)
      throws InvalidInputDataException, ElementNotFoundException {
    if (gatewayCode == null) {
      throw new InvalidInputDataException("Gateway code is mandatory");
    }

    Gateway gateway = gatewayRepository.read(gatewayCode);
    if (gateway == null) {
      throw new ElementNotFoundException("Gateway with code " + gatewayCode + " not found");
    }

    return new ArrayList<>(gateway.getSensors());
  }

  @Override
  public Gateway connectSensor(String sensorCode, String gatewayCode, String username)
      throws ElementNotFoundException, UnauthorizedException, InvalidInputDataException {
    validateMaintainer(username);

    if (sensorCode == null) {
      throw new InvalidInputDataException("Sensor code is mandatory");
    }
    if (gatewayCode == null) {
      throw new InvalidInputDataException("Gateway code is mandatory");
    }

    Sensor sensor = sensorRepository.read(sensorCode);
    if (sensor == null) {
      throw new ElementNotFoundException("Sensor with code " + sensorCode + " not found");
    }

    Gateway gateway = gatewayRepository.read(gatewayCode);
    if (gateway == null) {
      throw new ElementNotFoundException("Gateway with code " + gatewayCode + " not found");
    }

    gateway.addSensor(sensor);
    return gatewayRepository.update(gateway);
  }

  @Override
  public Gateway disconnectSensor(String sensorCode, String gatewayCode, String username)
      throws ElementNotFoundException, UnauthorizedException, InvalidInputDataException {
    validateMaintainer(username);

    if (sensorCode == null) {
      throw new InvalidInputDataException("Sensor code is mandatory");
    }
    if (gatewayCode == null) {
      throw new InvalidInputDataException("Gateway code is mandatory");
    }

    Sensor sensor = sensorRepository.read(sensorCode);
    if (sensor == null) {
      throw new ElementNotFoundException("Sensor with code " + sensorCode + " not found");
    }

    Gateway gateway = gatewayRepository.read(gatewayCode);
    if (gateway == null) {
      throw new ElementNotFoundException("Gateway with code " + gatewayCode + " not found");
    }

    gateway.removeSensor(sensor);
    return gatewayRepository.update(gateway);
  }

  private void validateMaintainer(String username) throws UnauthorizedException {
    if (username == null || username.isEmpty()) {
      throw new UnauthorizedException("Username is required");
    }

    User user = userRepository.read(username);
    if (user == null) {
      throw new UnauthorizedException("User " + username + " not found");
    }
    if (user.getType() != UserType.MAINTAINER) {
      throw new UnauthorizedException("User " + username + " is not authorized");
    }
  }
}
