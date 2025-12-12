package com.weather.report.operations.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.weather.report.WeatherReport;
import com.weather.report.exceptions.ElementNotFoundException;
import com.weather.report.exceptions.IdAlreadyInUseException;
import com.weather.report.exceptions.InvalidInputDataException;
import com.weather.report.exceptions.UnauthorizedException;
import com.weather.report.model.ThresholdType;
import com.weather.report.model.UserType;
import com.weather.report.model.entities.Measurement;
import com.weather.report.model.entities.Sensor;
import com.weather.report.model.entities.Threshold;
import com.weather.report.model.entities.User;
import com.weather.report.operations.SensorOperations;
import com.weather.report.reports.Report;
import com.weather.report.reports.SensorReport;
import com.weather.report.repositories.CRUDRepository;
import com.weather.report.repositories.MeasurementRepository;
import com.weather.report.services.AlertingService;

public class SensorOperationsImpl implements SensorOperations {

  private static final Pattern SENSOR_CODE_PATTERN = Pattern.compile("^S_\\d{6}$");
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(WeatherReport.DATE_FORMAT);

  private final CRUDRepository<Sensor, String> sensorRepository = new CRUDRepository<>(Sensor.class);
  private final CRUDRepository<User, String> userRepository = new CRUDRepository<>(User.class);
  private final MeasurementRepository measurementRepository = new MeasurementRepository();

  @Override
  public Sensor createSensor(String code, String name, String description, String username)
      throws IdAlreadyInUseException, InvalidInputDataException, UnauthorizedException {
    validateMaintainer(username);
    validateSensorCode(code);

    if (sensorRepository.read(code) != null) {
      throw new IdAlreadyInUseException("Sensor with code " + code + " already exists");
    }

    Sensor sensor = new Sensor(code, name, description);
    sensor.setCreatedBy(username);
    sensor.setCreatedAt(LocalDateTime.now());

    return sensorRepository.create(sensor);
  }

  @Override
  public Sensor updateSensor(String code, String name, String description, String username)
      throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException {
    validateMaintainer(username);

    if (code == null) {
      throw new InvalidInputDataException("Sensor code cannot be null");
    }

    Sensor sensor = sensorRepository.read(code);
    if (sensor == null) {
      throw new ElementNotFoundException("Sensor with code " + code + " not found");
    }

    sensor.setName(name);
    sensor.setDescription(description);
    sensor.setModifiedBy(username);
    sensor.setModifiedAt(LocalDateTime.now());

    return sensorRepository.update(sensor);
  }

  @Override
  public Sensor deleteSensor(String code, String username)
      throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException {
    validateMaintainer(username);

    if (code == null) {
      throw new InvalidInputDataException("Sensor code cannot be null");
    }

    Sensor sensor = sensorRepository.read(code);
    if (sensor == null) {
      throw new ElementNotFoundException("Sensor with code " + code + " not found");
    }

    Sensor deleted = sensorRepository.delete(code);
    AlertingService.notifyDeletion(username, code, Sensor.class);
    return deleted;
  }

  @Override
  public Collection<Sensor> getSensors(String... sensorCodes) {
    if (sensorCodes == null || sensorCodes.length == 0) {
      return sensorRepository.read();
    }

    List<Sensor> result = new ArrayList<>();
    for (String code : sensorCodes) {
      Sensor sensor = sensorRepository.read(code);
      if (sensor != null) {
        result.add(sensor);
      }
    }
    return result;
  }

  @Override
  public Threshold createThreshold(String sensorCode, ThresholdType type, double value, String username)
      throws InvalidInputDataException, ElementNotFoundException, IdAlreadyInUseException, UnauthorizedException {
    validateMaintainer(username);

    if (sensorCode == null) {
      throw new InvalidInputDataException("Sensor code is mandatory");
    }
    if (type == null) {
      throw new InvalidInputDataException("Threshold type is mandatory");
    }

    Sensor sensor = sensorRepository.read(sensorCode);
    if (sensor == null) {
      throw new ElementNotFoundException("Sensor with code " + sensorCode + " not found");
    }

    if (sensor.getThreshold() != null) {
      throw new IdAlreadyInUseException("Threshold already exists for sensor " + sensorCode);
    }

    Threshold threshold = new Threshold(type, value);
    sensor.setThreshold(threshold);
    sensorRepository.update(sensor);

    return threshold;
  }

  @Override
  public Threshold updateThreshold(String sensorCode, ThresholdType type, double value, String username)
      throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException {
    validateMaintainer(username);

    if (sensorCode == null) {
      throw new InvalidInputDataException("Sensor code is mandatory");
    }
    if (type == null) {
      throw new InvalidInputDataException("Threshold type is mandatory");
    }

    Sensor sensor = sensorRepository.read(sensorCode);
    if (sensor == null) {
      throw new ElementNotFoundException("Sensor with code " + sensorCode + " not found");
    }

    Threshold threshold = sensor.getThreshold();
    if (threshold == null) {
      throw new ElementNotFoundException("Threshold not found for sensor " + sensorCode);
    }

    threshold.setType(type);
    threshold.setValue(value);
    sensorRepository.update(sensor);

    return threshold;
  }

  @Override
  public SensorReport getSensorReport(String code, String startDate, String endDate)
      throws InvalidInputDataException, ElementNotFoundException {
    if (code == null) {
      throw new InvalidInputDataException("Sensor code is mandatory");
    }

    Sensor sensor = sensorRepository.read(code);
    if (sensor == null) {
      throw new ElementNotFoundException("Sensor with code " + code + " not found");
    }

    LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate, DATE_FORMATTER) : null;
    LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate, DATE_FORMATTER) : null;

    // Get all measurements for this sensor
    List<Measurement> allMeasurements = measurementRepository.read();
    List<Measurement> sensorMeasurements = allMeasurements.stream()
        .filter(m -> code.equals(m.getSensorCode()))
        .filter(m -> start == null || !m.getTimestamp().isBefore(start))
        .filter(m -> end == null || !m.getTimestamp().isAfter(end))
        .collect(Collectors.toList());

    return new SensorReportImpl(code, startDate, endDate, sensorMeasurements);
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

  private void validateSensorCode(String code) throws InvalidInputDataException {
    if (code == null || !SENSOR_CODE_PATTERN.matcher(code).matches()) {
      throw new InvalidInputDataException("Invalid sensor code format. Must be S_###### where ###### are six digits");
    }
  }

  // Inner class for SensorReport implementation
  private static class SensorReportImpl implements SensorReport {
    private final String code;
    private final String startDate;
    private final String endDate;
    private final long numberOfMeasurements;
    private final double mean;
    private final double variance;
    private final double stdDev;
    private final double minimumMeasuredValue;
    private final double maximumMeasuredValue;
    private final List<Measurement> outliers;
    private final SortedMap<Report.Range<Double>, Long> histogram;

    public SensorReportImpl(String code, String startDate, String endDate, List<Measurement> measurements) {
      this.code = code;
      this.startDate = startDate;
      this.endDate = endDate;
      this.numberOfMeasurements = measurements.size();

      if (measurements.isEmpty()) {
        this.mean = 0.0;
        this.variance = 0.0;
        this.stdDev = 0.0;
        this.minimumMeasuredValue = 0.0;
        this.maximumMeasuredValue = 0.0;
        this.outliers = new ArrayList<>();
        this.histogram = new TreeMap<>(Comparator.comparing(Report.Range::getStart));
        return;
      }

      // Calculate statistics
      double sum = measurements.stream().mapToDouble(Measurement::getValue).sum();
      this.mean = sum / measurements.size();

      this.minimumMeasuredValue = measurements.stream()
          .mapToDouble(Measurement::getValue)
          .min()
          .orElse(0.0);

      this.maximumMeasuredValue = measurements.stream()
          .mapToDouble(Measurement::getValue)
          .max()
          .orElse(0.0);

      if (measurements.size() < 2) {
        this.variance = 0.0;
        this.stdDev = 0.0;
        this.outliers = new ArrayList<>();
      } else {
        // Sample variance
        double sumSquaredDiff = measurements.stream()
            .mapToDouble(m -> Math.pow(m.getValue() - mean, 2))
            .sum();
        this.variance = sumSquaredDiff / (measurements.size() - 1);
        this.stdDev = Math.sqrt(variance);

        // Find outliers: |x - mean| >= 2 * stdDev
        this.outliers = measurements.stream()
            .filter(m -> Math.abs(m.getValue() - mean) >= 2 * stdDev)
            .collect(Collectors.toList());
      }

      // Build histogram with non-outlier measurements
      this.histogram = buildHistogram(measurements, outliers);
    }

    private SortedMap<Report.Range<Double>, Long> buildHistogram(List<Measurement> measurements, List<Measurement> outliers) {
      SortedMap<Report.Range<Double>, Long> result = new TreeMap<>(Comparator.comparing(Report.Range::getStart));

      // Get non-outlier measurements
      List<Measurement> nonOutliers = measurements.stream()
          .filter(m -> !outliers.contains(m))
          .collect(Collectors.toList());

      if (nonOutliers.isEmpty()) {
        return result;
      }

      // Find min and max values
      double min = nonOutliers.stream().mapToDouble(Measurement::getValue).min().orElse(0.0);
      double max = nonOutliers.stream().mapToDouble(Measurement::getValue).max().orElse(0.0);

      // Create 20 equal-width buckets
      int numBuckets = 20;
      double range = max - min;
      double bucketWidth = range / numBuckets;

      if (bucketWidth == 0) {
        bucketWidth = 1.0; // Avoid division by zero
      }

      List<DoubleRange> buckets = new ArrayList<>();
      for (int i = 0; i < numBuckets; i++) {
        double bucketStart = min + i * bucketWidth;
        double bucketEnd = (i == numBuckets - 1) ? max : min + (i + 1) * bucketWidth;
        boolean isLast = (i == numBuckets - 1);
        buckets.add(new DoubleRange(bucketStart, bucketEnd, isLast));
      }

      // Count measurements in each bucket
      for (DoubleRange bucket : buckets) {
        long count = nonOutliers.stream()
            .filter(m -> bucket.contains(m.getValue()))
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
    public double getMean() {
      return mean;
    }

    @Override
    public double getVariance() {
      return variance;
    }

    @Override
    public double getStdDev() {
      return stdDev;
    }

    @Override
    public double getMinimumMeasuredValue() {
      return minimumMeasuredValue;
    }

    @Override
    public double getMaximumMeasuredValue() {
      return maximumMeasuredValue;
    }

    @Override
    public List<Measurement> getOutliers() {
      return outliers;
    }

    @Override
    public SortedMap<Report.Range<Double>, Long> getHistogram() {
      return histogram;
    }
  }

  // Range implementation for Double
  private static class DoubleRange implements Report.Range<Double>, Comparable<DoubleRange> {
    private final double start;
    private final double end;
    private final boolean isLastBucket;

    public DoubleRange(double start, double end, boolean isLastBucket) {
      this.start = start;
      this.end = end;
      this.isLastBucket = isLastBucket;
    }

    @Override
    public Double getStart() {
      return start;
    }

    @Override
    public Double getEnd() {
      return end;
    }

    @Override
    public boolean contains(Double value) {
      if (value == null) return false;
      boolean afterOrAtStart = value >= start;
      boolean beforeEnd = value < end;
      boolean atEnd = Math.abs(value - end) < 1e-10;

      if (isLastBucket) {
        return afterOrAtStart && (beforeEnd || atEnd);
      }
      return afterOrAtStart && beforeEnd;
    }

    @Override
    public int compareTo(DoubleRange other) {
      return Double.compare(this.start, other.start);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      DoubleRange other = (DoubleRange) obj;
      return Double.compare(start, other.start) == 0 && Double.compare(end, other.end) == 0;
    }

    @Override
    public int hashCode() {
      return Double.hashCode(start) * 31 + Double.hashCode(end);
    }
  }
}
