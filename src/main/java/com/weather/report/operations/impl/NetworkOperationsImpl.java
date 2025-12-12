package com.weather.report.operations.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
import com.weather.report.model.entities.Measurement;
import com.weather.report.model.entities.Network;
import com.weather.report.model.entities.Operator;
import com.weather.report.model.entities.User;
import com.weather.report.operations.NetworkOperations;
import com.weather.report.reports.NetworkReport;
import com.weather.report.reports.Report;
import com.weather.report.repositories.CRUDRepository;
import com.weather.report.repositories.MeasurementRepository;
import com.weather.report.services.AlertingService;

public class NetworkOperationsImpl implements NetworkOperations {

  private static final Pattern NETWORK_CODE_PATTERN = Pattern.compile("^NET_\\d{2}$");
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(WeatherReport.DATE_FORMAT);

  private final CRUDRepository<Network, String> networkRepository = new CRUDRepository<>(Network.class);
  private final CRUDRepository<Operator, String> operatorRepository = new CRUDRepository<>(Operator.class);
  private final CRUDRepository<User, String> userRepository = new CRUDRepository<>(User.class);
  private final MeasurementRepository measurementRepository = new MeasurementRepository();

  @Override
  public Network createNetwork(String code, String name, String description, String username)
      throws IdAlreadyInUseException, InvalidInputDataException, UnauthorizedException {
    validateMaintainer(username);
    validateNetworkCode(code);

    if (networkRepository.read(code) != null) {
      throw new IdAlreadyInUseException("Network with code " + code + " already exists");
    }

    Network network = new Network(code, name, description);
    network.setCreatedBy(username);
    network.setCreatedAt(LocalDateTime.now());

    return networkRepository.create(network);
  }

  @Override
  public Network updateNetwork(String code, String name, String description, String username)
      throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException {
    validateMaintainer(username);
    
    if (code == null) {
      throw new InvalidInputDataException("Network code cannot be null");
    }

    Network network = networkRepository.read(code);
    if (network == null) {
      throw new ElementNotFoundException("Network with code " + code + " not found");
    }

    network.setName(name);
    network.setDescription(description);
    network.setModifiedBy(username);
    network.setModifiedAt(LocalDateTime.now());

    return networkRepository.update(network);
  }

  @Override
  public Network deleteNetwork(String code, String username)
      throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException {
    validateMaintainer(username);
    
    if (code == null) {
      throw new InvalidInputDataException("Network code cannot be null");
    }

    Network network = networkRepository.read(code);
    if (network == null) {
      throw new ElementNotFoundException("Network with code " + code + " not found");
    }

    Network deleted = networkRepository.delete(code);
    AlertingService.notifyDeletion(username, code, Network.class);
    return deleted;
  }

  @Override
  public Collection<Network> getNetworks(String... codes) {
    if (codes == null || codes.length == 0) {
      return networkRepository.read();
    }

    List<Network> result = new ArrayList<>();
    for (String code : codes) {
      Network network = networkRepository.read(code);
      if (network != null) {
        result.add(network);
      }
    }
    return result;
  }

  @Override
  public Operator createOperator(String firstName, String lastName, String email, String phoneNumber, String username)
      throws InvalidInputDataException, IdAlreadyInUseException, UnauthorizedException {
    validateMaintainer(username);

    if (firstName == null || firstName.isEmpty()) {
      throw new InvalidInputDataException("First name is mandatory");
    }
    if (lastName == null || lastName.isEmpty()) {
      throw new InvalidInputDataException("Last name is mandatory");
    }
    if (email == null || email.isEmpty()) {
      throw new InvalidInputDataException("Email is mandatory");
    }

    if (operatorRepository.read(email) != null) {
      throw new IdAlreadyInUseException("Operator with email " + email + " already exists");
    }

    Operator operator = new Operator(firstName, lastName, email, phoneNumber);
    return operatorRepository.create(operator);
  }

  @Override
  public Network addOperatorToNetwork(String networkCode, String operatorEmail, String username)
      throws ElementNotFoundException, InvalidInputDataException, UnauthorizedException {
    validateMaintainer(username);

    if (networkCode == null) {
      throw new InvalidInputDataException("Network code is mandatory");
    }
    if (operatorEmail == null) {
      throw new InvalidInputDataException("Operator email is mandatory");
    }

    Network network = networkRepository.read(networkCode);
    if (network == null) {
      throw new ElementNotFoundException("Network with code " + networkCode + " not found");
    }

    Operator operator = operatorRepository.read(operatorEmail);
    if (operator == null) {
      throw new ElementNotFoundException("Operator with email " + operatorEmail + " not found");
    }

    network.addOperator(operator);
    return networkRepository.update(network);
  }

  @Override
  public NetworkReport getNetworkReport(String code, String startDate, String endDate)
      throws InvalidInputDataException, ElementNotFoundException {
    if (code == null) {
      throw new InvalidInputDataException("Network code is mandatory");
    }

    Network network = networkRepository.read(code);
    if (network == null) {
      throw new ElementNotFoundException("Network with code " + code + " not found");
    }

    LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate, DATE_FORMATTER) : null;
    LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate, DATE_FORMATTER) : null;

    // Get all measurements for this network
    List<Measurement> allMeasurements = measurementRepository.read();
    List<Measurement> networkMeasurements = allMeasurements.stream()
        .filter(m -> code.equals(m.getNetworkCode()))
        .filter(m -> start == null || !m.getTimestamp().isBefore(start))
        .filter(m -> end == null || !m.getTimestamp().isAfter(end))
        .collect(Collectors.toList());

    return new NetworkReportImpl(code, startDate, endDate, networkMeasurements);
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

  private void validateNetworkCode(String code) throws InvalidInputDataException {
    if (code == null || !NETWORK_CODE_PATTERN.matcher(code).matches()) {
      throw new InvalidInputDataException("Invalid network code format. Must be NET_## where ## are two digits");
    }
  }

  // Inner class for NetworkReport implementation
  private static class NetworkReportImpl implements NetworkReport {
    private final String code;
    private final String startDate;
    private final String endDate;
    private final long numberOfMeasurements;
    private final Collection<String> mostActiveGateways;
    private final Collection<String> leastActiveGateways;
    private final Map<String, Double> gatewaysLoadRatio;
    private final SortedMap<Range<LocalDateTime>, Long> histogram;

    public NetworkReportImpl(String code, String startDate, String endDate, List<Measurement> measurements) {
      this.code = code;
      this.startDate = startDate;
      this.endDate = endDate;
      this.numberOfMeasurements = measurements.size();

      // Calculate gateway statistics
      Map<String, Long> gatewayMeasurementCounts = measurements.stream()
          .collect(Collectors.groupingBy(Measurement::getGatewayCode, Collectors.counting()));

      // Find most and least active gateways
      if (gatewayMeasurementCounts.isEmpty()) {
        this.mostActiveGateways = new ArrayList<>();
        this.leastActiveGateways = new ArrayList<>();
        this.gatewaysLoadRatio = new HashMap<>();
      } else {
        long maxCount = gatewayMeasurementCounts.values().stream().max(Long::compare).orElse(0L);
        long minCount = gatewayMeasurementCounts.values().stream().min(Long::compare).orElse(0L);

        this.mostActiveGateways = gatewayMeasurementCounts.entrySet().stream()
            .filter(e -> e.getValue() == maxCount)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        this.leastActiveGateways = gatewayMeasurementCounts.entrySet().stream()
            .filter(e -> e.getValue() == minCount)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // Calculate load ratio
        this.gatewaysLoadRatio = new HashMap<>();
        for (Map.Entry<String, Long> entry : gatewayMeasurementCounts.entrySet()) {
          double ratio = (entry.getValue() * 100.0) / numberOfMeasurements;
          gatewaysLoadRatio.put(entry.getKey(), ratio);
        }
      }

      // Build histogram
      this.histogram = buildHistogram(measurements, startDate, endDate);
    }

    private SortedMap<Range<LocalDateTime>, Long> buildHistogram(List<Measurement> measurements, String startDateStr, String endDateStr) {
      if (measurements.isEmpty()) {
        return new TreeMap<>(Comparator.comparing(Range::getStart));
      }

      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(WeatherReport.DATE_FORMAT);
      
      // Determine effective start and end
      LocalDateTime effectiveStart;
      LocalDateTime effectiveEnd;
      
      if (startDateStr != null) {
        effectiveStart = LocalDateTime.parse(startDateStr, formatter);
      } else {
        effectiveStart = measurements.stream()
            .map(Measurement::getTimestamp)
            .min(LocalDateTime::compareTo)
            .orElse(null);
      }
      
      if (endDateStr != null) {
        effectiveEnd = LocalDateTime.parse(endDateStr, formatter);
      } else {
        effectiveEnd = measurements.stream()
            .map(Measurement::getTimestamp)
            .max(LocalDateTime::compareTo)
            .orElse(null);
      }

      if (effectiveStart == null || effectiveEnd == null) {
        return new TreeMap<>(Comparator.comparing(Range::getStart));
      }

      // Determine granularity: hourly if <= 48 hours, daily otherwise
      long hoursBetween = ChronoUnit.HOURS.between(effectiveStart, effectiveEnd);
      boolean hourly = hoursBetween <= 48;

      SortedMap<Range<LocalDateTime>, Long> result = new TreeMap<>(Comparator.comparing(Range::getStart));
      List<DateTimeRange> buckets = new ArrayList<>();

      if (hourly) {
        // Create hourly buckets
        LocalDateTime current = effectiveStart;
        while (!current.isAfter(effectiveEnd)) {
          LocalDateTime bucketStart = current;
          LocalDateTime bucketEnd;
          
          if (current.equals(effectiveStart)) {
            // First bucket starts at effectiveStart
            bucketEnd = current.truncatedTo(ChronoUnit.HOURS).plusHours(1);
            if (bucketEnd.isAfter(effectiveEnd)) {
              bucketEnd = effectiveEnd;
            }
          } else {
            bucketEnd = current.plusHours(1);
            if (bucketEnd.isAfter(effectiveEnd)) {
              bucketEnd = effectiveEnd;
            }
          }
          
          buckets.add(new DateTimeRange(bucketStart, bucketEnd, false));
          current = bucketEnd;
          
          if (current.equals(effectiveEnd)) {
            break;
          }
        }
      } else {
        // Create daily buckets
        LocalDateTime current = effectiveStart;
        while (!current.isAfter(effectiveEnd)) {
          LocalDateTime bucketStart = current;
          LocalDateTime bucketEnd;
          
          if (current.equals(effectiveStart)) {
            // First bucket starts at effectiveStart
            bucketEnd = current.truncatedTo(ChronoUnit.DAYS).plusDays(1);
            if (bucketEnd.isAfter(effectiveEnd)) {
              bucketEnd = effectiveEnd;
            }
          } else {
            bucketEnd = current.plusDays(1);
            if (bucketEnd.isAfter(effectiveEnd)) {
              bucketEnd = effectiveEnd;
            }
          }
          
          buckets.add(new DateTimeRange(bucketStart, bucketEnd, false));
          current = bucketEnd;
          
          if (current.equals(effectiveEnd)) {
            break;
          }
        }
      }

      // Mark last bucket as inclusive
      if (!buckets.isEmpty()) {
        DateTimeRange lastBucket = buckets.get(buckets.size() - 1);
        buckets.set(buckets.size() - 1, new DateTimeRange(lastBucket.getStart(), lastBucket.getEnd(), true));
      }

      // Count measurements in each bucket
      for (DateTimeRange bucket : buckets) {
        long count = measurements.stream()
            .filter(m -> bucket.contains(m.getTimestamp()))
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
    public Collection<String> getMostActiveGateways() {
      return mostActiveGateways;
    }

    @Override
    public Collection<String> getLeastActiveGateways() {
      return leastActiveGateways;
    }

    @Override
    public Map<String, Double> getGatewaysLoadRatio() {
      return gatewaysLoadRatio;
    }

    @Override
    public SortedMap<Range<LocalDateTime>, Long> getHistogram() {
      return histogram;
    }
  }

  // Range implementation for LocalDateTime
  private static class DateTimeRange implements Report.Range<LocalDateTime>, Comparable<DateTimeRange> {
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final boolean isLastBucket;

    public DateTimeRange(LocalDateTime start, LocalDateTime end, boolean isLastBucket) {
      this.start = start;
      this.end = end;
      this.isLastBucket = isLastBucket;
    }

    @Override
    public LocalDateTime getStart() {
      return start;
    }

    @Override
    public LocalDateTime getEnd() {
      return end;
    }

    @Override
    public boolean contains(LocalDateTime value) {
      if (value == null) return false;
      boolean afterOrAtStart = !value.isBefore(start);
      boolean beforeEnd = value.isBefore(end);
      boolean atEnd = value.equals(end);
      
      if (isLastBucket) {
        return afterOrAtStart && (beforeEnd || atEnd);
      }
      return afterOrAtStart && beforeEnd;
    }

    @Override
    public int compareTo(DateTimeRange other) {
      return this.start.compareTo(other.start);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      DateTimeRange other = (DateTimeRange) obj;
      return start.equals(other.start) && end.equals(other.end);
    }

    @Override
    public int hashCode() {
      return start.hashCode() * 31 + end.hashCode();
    }
  }
}
