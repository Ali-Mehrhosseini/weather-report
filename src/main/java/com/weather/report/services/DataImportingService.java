package com.weather.report.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

import com.weather.report.WeatherReport;
import com.weather.report.model.ThresholdType;
import com.weather.report.model.entities.Measurement;
import com.weather.report.model.entities.Network;
import com.weather.report.model.entities.Operator;
import com.weather.report.model.entities.Sensor;
import com.weather.report.model.entities.Threshold;
import com.weather.report.repositories.CRUDRepository;
import com.weather.report.repositories.MeasurementRepository;

/**
 * Service responsible for importing measurements from CSV files and validating
 * them
 * against sensor thresholds, triggering notifications when needed (see README).
 */
public class DataImportingService {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(WeatherReport.DATE_FORMAT);

  private DataImportingService(){
    // utility class
  }

  /**
   * Reads measurements from CSV files, persists them through repositories and
   * invokes {@link #checkMeasurement(Measurement)} after each insertion. 
   * The time window format and CSV location are defined in the README.
   *
   * @param filePath path to the CSV file to import
   */
  public static void storeMeasurements(String filePath) {
    MeasurementRepository measurementRepository = new MeasurementRepository();
    
    try {
      BufferedReader reader = null;
      
      // Try to read from classpath first (for resources)
      InputStream is = DataImportingService.class.getClassLoader().getResourceAsStream(filePath);
      if (is != null) {
        reader = new BufferedReader(new InputStreamReader(is));
      } else {
        // Try to read from file system
        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
          reader = Files.newBufferedReader(path);
        } else {
          // Try with csv/ prefix
          is = DataImportingService.class.getClassLoader().getResourceAsStream("csv/" + filePath);
          if (is != null) {
            reader = new BufferedReader(new InputStreamReader(is));
          }
        }
      }
      
      if (reader == null) {
        throw new IOException("Cannot find file: " + filePath);
      }
      
      String line;
      boolean firstLine = true;
      
      while ((line = reader.readLine()) != null) {
        // Skip header line
        if (firstLine) {
          firstLine = false;
          continue;
        }
        
        // Skip empty lines
        if (line.trim().isEmpty()) {
          continue;
        }
        
        // Parse CSV line: date, networkCode, gatewayCode, sensorCode, value
        String[] parts = line.split(",");
        if (parts.length < 5) {
          continue;
        }
        
        String dateStr = parts[0].trim();
        String networkCode = parts[1].trim();
        String gatewayCode = parts[2].trim();
        String sensorCode = parts[3].trim();
        double value = Double.parseDouble(parts[4].trim());
        
        LocalDateTime timestamp = LocalDateTime.parse(dateStr, DATE_FORMATTER);
        
        Measurement measurement = new Measurement(networkCode, gatewayCode, sensorCode, value, timestamp);
        measurementRepository.create(measurement);
        
        // Check for threshold violation
        checkMeasurement(measurement);
      }
      
      reader.close();
      
    } catch (IOException e) {
      throw new RuntimeException("Error reading CSV file: " + filePath, e);
    }
  }

  /**
   * Validates the saved measurement against the threshold of the corresponding
   * sensor
   * and notifies operators when the value is out of bounds. To be implemented in
   * R1.
   *
   * @param measurement newly stored measurement
   */
  private static void checkMeasurement(Measurement measurement) {
    /***********************************************************************/
    /* Do not change these lines, use currentSensor to check for possible */
    /* threshold violation, tests mocks this db interaction */
    /***********************************************************************/
    CRUDRepository<Sensor, String> sensorRepository = new CRUDRepository<>(Sensor.class);
    Sensor currentSensor = sensorRepository.read().stream()
        .filter(s -> measurement.getSensorCode().equals(s.getCode()))
        .findFirst()
        .orElse(null);
    /***********************************************************************/
    
    // Check if sensor exists and has a threshold
    if (currentSensor == null) {
      return;
    }
    
    Threshold threshold = currentSensor.getThreshold();
    if (threshold == null) {
      return;
    }
    
    // Check if the measured value violates the threshold
    // Use getType() and getValue() directly for compatibility with test mocks
    boolean violated = isThresholdViolated(threshold.getType(), threshold.getValue(), measurement.getValue());
    
    if (violated) {
      // Get the network to find operators
      CRUDRepository<Network, String> networkRepository = new CRUDRepository<>(Network.class);
      Network network = networkRepository.read().stream()
          .filter(n -> measurement.getNetworkCode().equals(n.getCode()))
          .findFirst()
          .orElse(null);
      
      if (network != null) {
        Collection<Operator> operators = network.getOperators();
        if (operators != null && !operators.isEmpty()) {
          // Use sensor code for notification (as per updated test requirement)
          AlertingService.notifyThresholdViolation(operators, measurement.getSensorCode());
        }
      }
    }
  }
  
  /**
   * Checks if a measurement value violates a threshold.
   */
  private static boolean isThresholdViolated(ThresholdType type, double thresholdValue, double measuredValue) {
    if (type == null) return false;
    switch (type) {
      case LESS_THAN:
        return measuredValue < thresholdValue;
      case GREATER_THAN:
        return measuredValue > thresholdValue;
      case LESS_OR_EQUAL:
        return measuredValue <= thresholdValue;
      case GREATER_OR_EQUAL:
        return measuredValue >= thresholdValue;
      case EQUAL:
        return measuredValue == thresholdValue;
      case NOT_EQUAL:
        return measuredValue != thresholdValue;
      default:
        return false;
    }
  }

}
