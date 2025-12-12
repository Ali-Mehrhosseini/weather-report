package com.weather.report.operations.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.weather.report.WeatherReport;
import com.weather.report.exceptions.ElementNotFoundException;
import com.weather.report.exceptions.IdAlreadyInUseException;
import com.weather.report.exceptions.InvalidInputDataException;
import com.weather.report.exceptions.UnauthorizedException;
import com.weather.report.model.UserType;
import com.weather.report.model.entities.Gateway;
import com.weather.report.model.entities.Measurement;
import com.weather.report.model.entities.Parameter;
import com.weather.report.model.entities.User;
import com.weather.report.operations.GatewayOperations;
import com.weather.report.reports.GatewayReport;
import com.weather.report.reports.Report;
import com.weather.report.repositories.CRUDRepository;
import com.weather.report.repositories.MeasurementRepository;
import com.weather.report.services.AlertingService;

public class GatewayOperationsImpl implements GatewayOperations {

  private static final Pattern GATEWAY_CODE_PATTERN = Pattern.compile("^GW_\\d{4}$");
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(WeatherReport.DATE_FORMAT);

  private final CRUDRepository<Gateway, String> gatewayRepository = new CRUDRepository<>(Gateway.class);
  private final CRUDRepository<User, String> userRepository = new CRUDRepository<>(User.class);
  private final MeasurementRepository measurementRepository = new MeasurementRepository();

  @Override
  public Gateway createGateway(String code, String name, String description, String username)
      throws IdAlreadyInUseException, InvalidInputDataException, UnauthorizedException {
    validateMaintainer(username);
    validateGatewayCode(code);

    if (gatewayRepository.read(code) != null) {
      throw new IdAlreadyInUseException("Gateway with code " + code + " already exists");
    }

    Gateway gateway = new Gateway(code, name, description);
    gateway.setCreatedBy(username);
    gateway.setCreatedAt(LocalDateTime.now());

    return gatewayRepository.create(gateway);
  }

  @Override
  public Gateway updateGateway(String code, String name, String description, String username)
      throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException {
    validateMaintainer(username);

    if (code == null) {
      throw new InvalidInputDataException("Gateway code cannot be null");
    }

    Gateway gateway = gatewayRepository.read(code);
    if (gateway == null) {
      throw new ElementNotFoundException("Gateway with code " + code + " not found");
    }

    gateway.setName(name);
    gateway.setDescription(description);
    gateway.setModifiedBy(username);
    gateway.setModifiedAt(LocalDateTime.now());

    return gatewayRepository.update(gateway);
  }

  @Override
  public Gateway deleteGateway(String code, String username)
      throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException {
    validateMaintainer(username);

    if (code == null) {
      throw new InvalidInputDataException("Gateway code cannot be null");
    }

    Gateway gateway = gatewayRepository.read(code);
    if (gateway == null) {
      throw new ElementNotFoundException("Gateway with code " + code + " not found");
    }

    Gateway deleted = gatewayRepository.delete(code);
    AlertingService.notifyDeletion(username, code, Gateway.class);
    return deleted;
  }

  @Override
  public Collection<Gateway> getGateways(String... gatewayCodes) {
    if (gatewayCodes == null || gatewayCodes.length == 0) {
      return gatewayRepository.read();
    }

    List<Gateway> result = new ArrayList<>();
    for (String code : gatewayCodes) {
      Gateway gateway = gatewayRepository.read(code);
      if (gateway != null) {
        result.add(gateway);
      }
    }
    return result;
  }

  @Override
  public Parameter createParameter(String gatewayCode, String code, String name, String description, double value,
      String username) throws IdAlreadyInUseException, InvalidInputDataException, ElementNotFoundException,
      UnauthorizedException {
    validateMaintainer(username);

    if (gatewayCode == null) {
      throw new InvalidInputDataException("Gateway code is mandatory");
    }
    if (code == null) {
      throw new InvalidInputDataException("Parameter code is mandatory");
    }

    Gateway gateway = gatewayRepository.read(gatewayCode);
    if (gateway == null) {
      throw new ElementNotFoundException("Gateway with code " + gatewayCode + " not found");
    }

    // Check if parameter already exists
    Parameter existing = gateway.getParameter(code);
    if (existing != null) {
      throw new IdAlreadyInUseException("Parameter with code " + code + " already exists in gateway " + gatewayCode);
    }

    Parameter parameter = new Parameter(code, name, description, value);
    gateway.addParameter(parameter);
    gatewayRepository.update(gateway);

    return parameter;
  }

  @Override
  public Parameter updateParameter(String gatewayCode, String code, double value, String username)
      throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException {
    validateMaintainer(username);

    if (gatewayCode == null) {
      throw new InvalidInputDataException("Gateway code is mandatory");
    }
    if (code == null) {
      throw new InvalidInputDataException("Parameter code is mandatory");
    }

    Gateway gateway = gatewayRepository.read(gatewayCode);
    if (gateway == null) {
      throw new ElementNotFoundException("Gateway with code " + gatewayCode + " not found");
    }

    Parameter parameter = gateway.getParameter(code);
    if (parameter == null) {
      throw new ElementNotFoundException("Parameter with code " + code + " not found in gateway " + gatewayCode);
    }

    parameter.setValue(value);
    gatewayRepository.update(gateway);

    return parameter;
  }

  @Override
  public GatewayReport getGatewayReport(String code, String startDate, String endDate)
      throws ElementNotFoundException, InvalidInputDataException {
    if (code == null) {
      throw new InvalidInputDataException("Gateway code is mandatory");
    }

    Gateway gateway = gatewayRepository.read(code);
    if (gateway == null) {
      throw new ElementNotFoundException("Gateway with code " + code + " not found");
    }

    LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate, DATE_FORMATTER) : null;
    LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate, DATE_FORMATTER) : null;

    // Get all measurements for this gateway
    List<Measurement> allMeasurements = measurementRepository.read();
    List<Measurement> gatewayMeasurements = allMeasurements.stream()
        .filter(m -> code.equals(m.getGatewayCode()))
        .filter(m -> start == null || !m.getTimestamp().isBefore(start))
        .filter(m -> end == null || !m.getTimestamp().isAfter(end))
        .collect(Collectors.toList());

    return new GatewayReportImpl(code, startDate, endDate, gatewayMeasurements, gateway);
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

  private void validateGatewayCode(String code) throws InvalidInputDataException {
    if (code == null || !GATEWAY_CODE_PATTERN.matcher(code).matches()) {
      throw new InvalidInputDataException("Invalid gateway code format. Must be GW_#### where #### are four digits");
    }
  }

  // Inner class for GatewayReport implementation
  private static class GatewayReportImpl implements GatewayReport {
    private final String code;
    private final String startDate;
    private final String endDate;
    private final long numberOfMeasurements;
    private final Collection<String> mostActiveSensors;
    private final Collection<String> leastActiveSensors;
    private final Map<String, Double> sensorsLoadRatio;
    private final Collection<String> outlierSensors;
    private final double batteryChargePercentage;
    private final SortedMap<Report.Range<Duration>, Long> histogram;

    public GatewayReportImpl(String code, String startDate, String endDate, List<Measurement> measurements, Gateway gateway) {
      this.code = code;
      this.startDate = startDate;
      this.endDate = endDate;
      this.numberOfMeasurements = measurements.size();

      // Calculate sensor statistics
      Map<String, Long> sensorMeasurementCounts = measurements.stream()
          .collect(Collectors.groupingBy(Measurement::getSensorCode, Collectors.counting()));

      // Find most and least active sensors
      if (sensorMeasurementCounts.isEmpty()) {
        this.mostActiveSensors = new ArrayList<>();
        this.leastActiveSensors = new ArrayList<>();
        this.sensorsLoadRatio = new HashMap<>();
      } else {
        long maxCount = sensorMeasurementCounts.values().stream().max(Long::compare).orElse(0L);
        long minCount = sensorMeasurementCounts.values().stream().min(Long::compare).orElse(0L);

        this.mostActiveSensors = sensorMeasurementCounts.entrySet().stream()
            .filter(e -> e.getValue() == maxCount)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        this.leastActiveSensors = sensorMeasurementCounts.entrySet().stream()
            .filter(e -> e.getValue() == minCount)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // Calculate load ratio
        this.sensorsLoadRatio = new HashMap<>();
        for (Map.Entry<String, Long> entry : sensorMeasurementCounts.entrySet()) {
          double ratio = (entry.getValue() * 100.0) / numberOfMeasurements;
          sensorsLoadRatio.put(entry.getKey(), ratio);
        }
      }

      // Calculate outlier sensors
      this.outlierSensors = calculateOutlierSensors(measurements, gateway);

      // Get battery charge
      Parameter batteryParam = gateway.getParameter(Parameter.BATTERY_CHARGE_PERCENTAGE_CODE);
      this.batteryChargePercentage = batteryParam != null ? batteryParam.getValue() : 0.0;

      // Build histogram
      this.histogram = buildHistogram(measurements);
    }

    private Collection<String> calculateOutlierSensors(List<Measurement> measurements, Gateway gateway) {
      Parameter expectedMeanParam = gateway.getParameter(Parameter.EXPECTED_MEAN_CODE);
      Parameter expectedStdDevParam = gateway.getParameter(Parameter.EXPECTED_STD_DEV_CODE);

      if (expectedMeanParam == null || expectedStdDevParam == null) {
        return new ArrayList<>();
      }

      double expectedMean = expectedMeanParam.getValue();
      double expectedStdDev = expectedStdDevParam.getValue();

      // Group measurements by sensor and calculate mean for each
      Map<String, List<Measurement>> measurementsBySensor = measurements.stream()
          .collect(Collectors.groupingBy(Measurement::getSensorCode));

      List<String> outliers = new ArrayList<>();
      for (Map.Entry<String, List<Measurement>> entry : measurementsBySensor.entrySet()) {
        double sensorMean = entry.getValue().stream()
            .mapToDouble(Measurement::getValue)
            .average()
            .orElse(0.0);

        if (Math.abs(sensorMean - expectedMean) >= 2 * expectedStdDev) {
          outliers.add(entry.getKey());
        }
      }

      return outliers;
    }

    private SortedMap<Report.Range<Duration>, Long> buildHistogram(List<Measurement> measurements) {
      SortedMap<Report.Range<Duration>, Long> result = new TreeMap<>(Comparator.comparing(Report.Range::getStart));

      if (measurements.size() < 2) {
        return result;
      }

      // Sort measurements by timestamp
      List<Measurement> sorted = measurements.stream()
          .sorted(Comparator.comparing(Measurement::getTimestamp))
          .collect(Collectors.toList());

      // Calculate inter-arrival times
      List<Duration> interArrivalTimes = new ArrayList<>();
      for (int i = 0; i < sorted.size() - 1; i++) {
        Duration duration = Duration.between(sorted.get(i).getTimestamp(), sorted.get(i + 1).getTimestamp());
        interArrivalTimes.add(duration);
      }

      if (interArrivalTimes.isEmpty()) {
        return result;
      }

      // Find min and max duration
      Duration minDuration = interArrivalTimes.stream().min(Duration::compareTo).orElse(Duration.ZERO);
      Duration maxDuration = interArrivalTimes.stream().max(Duration::compareTo).orElse(Duration.ZERO);

      // Create 20 buckets
      int numBuckets = 20;
      long totalNanos = maxDuration.toNanos() - minDuration.toNanos();
      long bucketSize = totalNanos / numBuckets;
      
      if (bucketSize == 0) {
        bucketSize = 1; // Avoid division by zero
      }

      List<DurationRange> buckets = new ArrayList<>();
      for (int i = 0; i < numBuckets; i++) {
        Duration bucketStart = minDuration.plusNanos(i * bucketSize);
        Duration bucketEnd = (i == numBuckets - 1) ? maxDuration : minDuration.plusNanos((i + 1) * bucketSize);
        boolean isLast = (i == numBuckets - 1);
        buckets.add(new DurationRange(bucketStart, bucketEnd, isLast));
      }

      // Count inter-arrival times in each bucket
      for (DurationRange bucket : buckets) {
        long count = interArrivalTimes.stream()
            .filter(bucket::contains)
            .count();
        result.put(bucket, count);
      }

      return result;
    }

    @Override
    public String getCode() {
      return code;
    }

    @Override
    public String getStartDate() {
      return startDate;
    }

    @Override
    public String getEndDate() {
      return endDate;
    }

    @Override
    public long getNumberOfMeasurements() {
      return numberOfMeasurements;
    }

    @Override
    public Collection<String> getMostActiveSensors() {
      return mostActiveSensors;
    }

    @Override
    public Collection<String> getLeastActiveSensors() {
      return leastActiveSensors;
    }

    @Override
    public Map<String, Double> getSensorsLoadRatio() {
      return sensorsLoadRatio;
    }

    @Override
    public Collection<String> getOutlierSensors() {
      return outlierSensors;
    }

    @Override
    public double getBatteryChargePercentage() {
      return batteryChargePercentage;
    }

    @Override
    public SortedMap<Report.Range<Duration>, Long> getHistogram() {
      return histogram;
    }
  }

  // Range implementation for Duration
  private static class DurationRange implements Report.Range<Duration>, Comparable<DurationRange> {
    private final Duration start;
    private final Duration end;
    private final boolean isLastBucket;

    public DurationRange(Duration start, Duration end, boolean isLastBucket) {
      this.start = start;
      this.end = end;
      this.isLastBucket = isLastBucket;
    }

    @Override
    public Duration getStart() {
      return start;
    }

    @Override
    public Duration getEnd() {
      return end;
    }

    @Override
    public boolean contains(Duration value) {
      if (value == null) return false;
      boolean afterOrAtStart = value.compareTo(start) >= 0;
      boolean beforeEnd = value.compareTo(end) < 0;
      boolean atEnd = value.equals(end);

      if (isLastBucket) {
        return afterOrAtStart && (beforeEnd || atEnd);
      }
      return afterOrAtStart && beforeEnd;
    }

    @Override
    public int compareTo(DurationRange other) {
      return this.start.compareTo(other.start);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      DurationRange other = (DurationRange) obj;
      return start.equals(other.start) && end.equals(other.end);
    }

    @Override
    public int hashCode() {
      return start.hashCode() * 31 + end.hashCode();
    }
  }
}
