# Weather Report System - Complete Project Documentation

## Table of Contents
1. [Project Overview](#1-project-overview)
2. [Architecture Overview](#2-architecture-overview)
3. [Domain Model (Entities)](#3-domain-model-entities)
4. [Persistence Layer](#4-persistence-layer)
5. [Repository Pattern](#5-repository-pattern)
6. [Services Layer](#6-services-layer)
7. [Operations Layer](#7-operations-layer)
8. [Exception Handling](#8-exception-handling)
9. [Reports](#9-reports)
10. [Requirements Breakdown](#10-requirements-breakdown)
11. [How Everything Connects](#11-how-everything-connects)

---

## 1. Project Overview

### What is the Weather Report System?

The Weather Report System is a **monitoring platform** designed to manage weather sensor networks. Think of it like a smart home system but for weather monitoring across multiple locations.

### Real-World Analogy

Imagine you're managing weather stations across a country:
- **Networks** = Different regions (e.g., "Northern Region", "Coastal Area")
- **Gateways** = Weather stations in each region (collect data from multiple sensors)
- **Sensors** = Individual measurement devices (thermometers, barometers, etc.)
- **Operators** = People who receive alerts when something goes wrong
- **Measurements** = The actual temperature/pressure/humidity readings

### Technology Stack

| Technology | Purpose |
|------------|---------|
| **Java 25** | Programming language |
| **JPA/Hibernate** | Object-Relational Mapping (database interaction) |
| **H2 Database** | In-memory database for testing |
| **JUnit 5** | Unit testing framework |
| **Mockito** | Mocking framework for tests |
| **Maven** | Build and dependency management |

---

## 2. Architecture Overview

The project follows a **layered architecture**:

```
┌─────────────────────────────────────────────────────────────┐
│                    WeatherReport (Facade)                    │
│         Entry point - delegates to operations                │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Operations Layer                          │
│   NetworkOperations, GatewayOperations, SensorOperations     │
│              TopologyOperations                              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Services Layer                            │
│        DataImportingService, AlertingService                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   Repository Layer                           │
│          CRUDRepository, MeasurementRepository               │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   Persistence Layer                          │
│           PersistenceManager, EntityManager                  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Database (H2)                           │
│              Stores all entities                             │
└─────────────────────────────────────────────────────────────┘
```

### Design Patterns Used

| Pattern | Where Used | Purpose |
|---------|------------|---------|
| **Facade** | `WeatherReport` class | Single entry point to the system |
| **Factory** | `OperationsFactory` | Creates operation implementations |
| **Repository** | `CRUDRepository` | Abstracts database operations |
| **Strategy** | Operations interfaces | Different implementations possible |

---

## 3. Domain Model (Entities)

### 3.1 Entity Hierarchy

```
                    Timestamped (Abstract)
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
    Network           Gateway           Sensor
        │                 │                 │
        │                 │                 │
   [operators]      [parameters]       [threshold]
   [gateways]        [sensors]
```

### 3.2 Timestamped (Base Class)

**Location:** `com.weather.report.model.Timestamped`

**Purpose:** Provides audit fields for tracking who created/modified entities and when.

```java
@MappedSuperclass
public abstract class Timestamped {
    private LocalDateTime createdAt;    // When entity was created
    private String createdBy;           // Username who created it
    private LocalDateTime modifiedAt;   // When last modified
    private String modifiedBy;          // Username who modified it
}
```

**Why `@MappedSuperclass`?**
- This JPA annotation tells Hibernate that this class's fields should be inherited by subclasses
- The `Timestamped` class itself won't have its own table
- All subclasses (Network, Gateway, Sensor) will have these 4 columns in their tables

### 3.3 Network Entity

**Location:** `com.weather.report.model.entities.Network`

**Purpose:** Represents a monitoring network - a logical grouping of gateways.

**Code Format:** `NET_##` (e.g., NET_01, NET_99)

```java
@Entity
public class Network extends Timestamped {
    @Id
    private String code;           // Primary key: NET_##
    private String name;           // Human-readable name
    private String description;    // Optional description
    
    @ManyToMany
    private List<Operator> operators;  // People who get notifications
    
    @OneToMany
    private List<Gateway> gateways;    // Connected gateways
}
```

**Relationships:**
- **Many-to-Many with Operator**: One network can have multiple operators, and one operator can be assigned to multiple networks
- **One-to-Many with Gateway**: One network contains multiple gateways

**Key Methods:**
| Method | Purpose |
|--------|---------|
| `addOperator(Operator)` | Assigns an operator to receive alerts |
| `addGateway(Gateway)` | Connects a gateway to this network |
| `removeGateway(Gateway)` | Disconnects a gateway |
| `getOperators()` | Returns all assigned operators |
| `getGateways()` | Returns all connected gateways |

### 3.4 Gateway Entity

**Location:** `com.weather.report.model.entities.Gateway`

**Purpose:** Represents a physical device that collects data from multiple sensors.

**Code Format:** `GW_####` (e.g., GW_0001, GW_9999)

```java
@Entity
public class Gateway extends Timestamped {
    @Id
    private String code;           // Primary key: GW_####
    private String name;           // Human-readable name
    private String description;    // Optional description
    
    @OneToMany(cascade = CascadeType.ALL)
    private List<Parameter> parameters;  // Configuration parameters
    
    @OneToMany
    private List<Sensor> sensors;        // Connected sensors
}
```

**Relationships:**
- **One-to-Many with Parameter**: Gateway owns its parameters (cascade delete)
- **One-to-Many with Sensor**: Gateway collects data from multiple sensors

**Key Methods:**
| Method | Purpose |
|--------|---------|
| `getParameter(String code)` | Find a specific parameter by code |
| `addParameter(Parameter)` | Add a configuration parameter |
| `addSensor(Sensor)` | Connect a sensor to this gateway |
| `removeSensor(Sensor)` | Disconnect a sensor |

### 3.5 Sensor Entity

**Location:** `com.weather.report.model.entities.Sensor`

**Purpose:** Represents a physical measurement device (thermometer, barometer, etc.)

**Code Format:** `S_######` (e.g., S_000001, S_999999)

```java
@Entity
public class Sensor extends Timestamped {
    @Id
    private String code;           // Primary key: S_######
    private String name;           // Human-readable name (e.g., "Temperature Sensor A")
    private String description;    // Optional description
    
    @OneToOne(cascade = CascadeType.ALL)
    private Threshold threshold;   // Alert threshold configuration
}
```

**Relationships:**
- **One-to-One with Threshold**: Each sensor can have one threshold for alerts

**Key Methods:**
| Method | Purpose |
|--------|---------|
| `getThreshold()` | Get the alert threshold |
| `setThreshold(Threshold)` | Set/update the threshold |
| `getName()` | Get sensor name (used in notifications) |

### 3.6 Operator Entity

**Location:** `com.weather.report.model.entities.Operator`

**Purpose:** Represents a person who receives alert notifications.

```java
@Entity
public class Operator {
    @Id
    private String email;          // Primary key (unique identifier)
    private String firstName;      // First name
    private String lastName;       // Last name
    private String phoneNumber;    // Optional phone number
}
```

**Why email as ID?**
- Emails are naturally unique
- Used for sending notifications
- Easy to look up operators

### 3.7 Parameter Entity

**Location:** `com.weather.report.model.entities.Parameter`

**Purpose:** Configuration values for gateways (e.g., battery level, firmware version).

```java
@Entity
public class Parameter {
    @Id
    @GeneratedValue
    private Long id;               // Auto-generated ID
    private String code;           // Parameter code (e.g., "BATTERY")
    private String name;           // Human-readable name
    private String description;    // What this parameter represents
    
    @Column(name = "param_value")  // "value" is reserved in H2
    private double value;          // The actual numeric value
}
```

**Why `param_value` instead of `value`?**
- `VALUE` is a reserved keyword in H2 database
- Using `@Column(name = "param_value")` avoids SQL errors

### 3.8 Threshold Entity

**Location:** `com.weather.report.model.entities.Threshold`

**Purpose:** Defines alert conditions for sensors.

```java
@Entity
public class Threshold {
    @Id
    @GeneratedValue
    private Long id;
    
    private ThresholdType type;    // Comparison type (GREATER_THAN, LESS_THAN, etc.)
    
    @Column(name = "threshold_value")
    private double value;          // The threshold value to compare against
}
```

### 3.9 ThresholdType Enum

**Location:** `com.weather.report.model.ThresholdType`

**Purpose:** Defines how to compare measured values against thresholds.

```java
public enum ThresholdType {
    LESS_THAN,        // value < threshold → alert
    GREATER_THAN,     // value > threshold → alert
    LESS_OR_EQUAL,    // value <= threshold → alert
    GREATER_OR_EQUAL, // value >= threshold → alert
    EQUAL,            // value == threshold → alert
    NOT_EQUAL         // value != threshold → alert
}
```

**Example:**
- Sensor: Temperature sensor
- Threshold: type=GREATER_THAN, value=40.0
- If temperature reading is 45°C → Alert triggered!

### 3.10 Measurement Entity

**Location:** `com.weather.report.model.entities.Measurement`

**Purpose:** Stores individual sensor readings.

```java
@Entity
public class Measurement {
    @Id
    @GeneratedValue
    private Long id;
    private String networkCode;    // Which network
    private String gatewayCode;    // Which gateway
    private String sensorCode;     // Which sensor
    private double value;          // The measured value
    private LocalDateTime timestamp; // When it was measured
}
```

### 3.11 User Entity

**Location:** `com.weather.report.model.entities.User`

**Purpose:** System users who perform operations.

```java
@Entity
@Table(name = "WR_USER")  // "USER" is reserved in H2
public class User {
    @Id
    private String username;
    private UserType type;  // MAINTAINER or VIEWER
}
```

### 3.12 UserType Enum

```java
public enum UserType {
    MAINTAINER,  // Can create, update, delete entities
    VIEWER       // Can only read data
}
```

---

## 4. Persistence Layer

### 4.1 PersistenceManager

**Location:** `com.weather.report.persistence.PersistenceManager`

**Purpose:** Manages the database connection and provides `EntityManager` instances.

**How it works:**
1. Reads configuration from `persistence.xml`
2. Creates an `EntityManagerFactory` (expensive, created once)
3. Provides `EntityManager` instances for database operations

```java
public class PersistenceManager {
    private static EntityManagerFactory emf;
    
    public static EntityManager getEntityManager() {
        if (emf == null) {
            emf = Persistence.createEntityManagerFactory("weather-report-pu");
        }
        return emf.createEntityManager();
    }
}
```

### 4.2 persistence.xml

**Location:** `src/main/resources/META-INF/persistence.xml`

**Purpose:** JPA configuration file.

```xml
<persistence-unit name="weather-report-pu">
    <!-- List all entity classes -->
    <class>com.weather.report.model.entities.User</class>
    <class>com.weather.report.model.entities.Measurement</class>
    <class>com.weather.report.model.entities.Network</class>
    <class>com.weather.report.model.entities.Gateway</class>
    <class>com.weather.report.model.entities.Sensor</class>
    <class>com.weather.report.model.entities.Operator</class>
    <class>com.weather.report.model.entities.Parameter</class>
    <class>com.weather.report.model.entities.Threshold</class>
    
    <properties>
        <!-- H2 in-memory database -->
        <property name="jakarta.persistence.jdbc.url" 
                  value="jdbc:h2:mem:weatherdb"/>
        <!-- Auto-create tables from entities -->
        <property name="hibernate.hbm2ddl.auto" value="create-drop"/>
    </properties>
</persistence-unit>
```

---

## 5. Repository Pattern

### 5.1 What is the Repository Pattern?

The repository pattern abstracts database operations, providing a clean interface for:
- **Create** - Insert new entities
- **Read** - Fetch entities
- **Update** - Modify existing entities
- **Delete** - Remove entities

### 5.2 CRUDRepository

**Location:** `com.weather.report.repositories.CRUDRepository`

**Purpose:** Generic repository for all CRUD operations on any entity type.

```java
public class CRUDRepository<T, ID> {
    private final Class<T> entityClass;
    
    public CRUDRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
    }
    
    // CREATE - Insert new entity
    public T create(T entity) {
        EntityManager em = PersistenceManager.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        em.persist(entity);  // INSERT INTO table ...
        tx.commit();
        return entity;
    }
    
    // READ - Get by ID
    public T read(ID id) {
        EntityManager em = PersistenceManager.getEntityManager();
        return em.find(entityClass, id);  // SELECT * FROM table WHERE id = ?
    }
    
    // READ ALL
    public List<T> read() {
        EntityManager em = PersistenceManager.getEntityManager();
        return em.createQuery("SELECT e FROM " + entityClass.getSimpleName() + " e", 
                              entityClass).getResultList();
    }
    
    // UPDATE
    public T update(T entity) {
        EntityManager em = PersistenceManager.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        T merged = em.merge(entity);  // UPDATE table SET ... WHERE id = ?
        tx.commit();
        return merged;
    }
    
    // DELETE
    public T delete(ID id) {
        EntityManager em = PersistenceManager.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        T entity = em.find(entityClass, id);
        if (entity != null) {
            tx.begin();
            em.remove(em.merge(entity));  // DELETE FROM table WHERE id = ?
            tx.commit();
        }
        return entity;
    }
}
```

**Generic Type Parameters:**
- `T` = Entity type (Network, Gateway, Sensor, etc.)
- `ID` = Primary key type (String for Network, Long for Parameter)

**Usage Examples:**
```java
// For networks (String ID)
CRUDRepository<Network, String> networkRepo = new CRUDRepository<>(Network.class);
Network net = networkRepo.read("NET_01");

// For parameters (Long ID)
CRUDRepository<Parameter, Long> paramRepo = new CRUDRepository<>(Parameter.class);
Parameter param = paramRepo.read(1L);
```

### 5.3 MeasurementRepository

**Location:** `com.weather.report.repositories.MeasurementRepository`

**Purpose:** Specialized repository for measurements with time-based queries.

```java
public class MeasurementRepository extends CRUDRepository<Measurement, Long> {
    
    // Find measurements within a time range
    public List<Measurement> findByTimeRange(LocalDateTime start, LocalDateTime end) {
        // Custom query for time filtering
    }
    
    // Find measurements for a specific sensor
    public List<Measurement> findBySensorCode(String sensorCode) {
        // Custom query for sensor filtering
    }
}
```

---

## 6. Services Layer

### 6.1 DataImportingService

**Location:** `com.weather.report.services.DataImportingService`

**Purpose:** Imports sensor measurements from CSV files and checks for threshold violations.

#### CSV File Format

```csv
timestamp,networkCode,gatewayCode,sensorCode,value
2025-01-01 10:00:00,NET_01,GW_0001,S_000001,25.5
2025-01-01 10:05:00,NET_01,GW_0001,S_000001,26.0
```

#### How it works:

```
┌─────────────┐     ┌──────────────────────┐     ┌────────────────┐
│  CSV File   │ --> │ DataImportingService │ --> │   Database     │
│  S_111.csv  │     │  storeMeasurements() │     │  (Measurement) │
└─────────────┘     └──────────────────────┘     └────────────────┘
                              │
                              ▼
                    ┌──────────────────────┐
                    │  checkMeasurement()  │
                    │  - Get sensor        │
                    │  - Check threshold   │
                    │  - Alert if violated │
                    └──────────────────────┘
```

#### Code Walkthrough:

```java
public static void storeMeasurements(String filePath) {
    // 1. Read CSV file line by line
    BufferedReader reader = new BufferedReader(new FileReader(filePath));
    
    // 2. Skip header line
    String line = reader.readLine();  // Skip: timestamp,networkCode,...
    
    // 3. Process each data line
    while ((line = reader.readLine()) != null) {
        String[] parts = line.split(",");
        
        // 4. Parse values
        LocalDateTime timestamp = LocalDateTime.parse(parts[0], formatter);
        String networkCode = parts[1];
        String gatewayCode = parts[2];
        String sensorCode = parts[3];
        double value = Double.parseDouble(parts[4]);
        
        // 5. Create and save measurement
        Measurement m = new Measurement(networkCode, gatewayCode, sensorCode, 
                                        value, timestamp);
        measurementRepository.create(m);
        
        // 6. Check for threshold violation
        checkMeasurement(m);
    }
}
```

#### Threshold Checking Logic:

```java
private static void checkMeasurement(Measurement measurement) {
    // 1. Find the sensor
    Sensor sensor = sensorRepository.findByCode(measurement.getSensorCode());
    
    // 2. Get threshold
    Threshold threshold = sensor.getThreshold();
    if (threshold == null) return;  // No threshold configured
    
    // 3. Check if violated
    boolean violated = isThresholdViolated(
        threshold.getType(), 
        threshold.getValue(), 
        measurement.getValue()
    );
    
    // 4. Send alerts if violated
    if (violated) {
        Network network = findNetworkByCode(measurement.getNetworkCode());
        AlertingService.notifyThresholdViolation(
            network.getOperators(), 
            sensor.getName()
        );
    }
}

private static boolean isThresholdViolated(ThresholdType type, 
                                           double threshold, 
                                           double measured) {
    switch (type) {
        case GREATER_THAN:     return measured > threshold;
        case LESS_THAN:        return measured < threshold;
        case GREATER_OR_EQUAL: return measured >= threshold;
        case LESS_OR_EQUAL:    return measured <= threshold;
        case EQUAL:            return measured == threshold;
        case NOT_EQUAL:        return measured != threshold;
        default:               return false;
    }
}
```

### 6.2 AlertingService

**Location:** `com.weather.report.services.AlertingService`

**Purpose:** Sends notifications to operators.

```java
public class AlertingService {
    
    // Notify operators when a threshold is violated
    public static void notifyThresholdViolation(Collection<Operator> operators, 
                                                 String sensorName) {
        for (Operator op : operators) {
            // Send notification (email, SMS, etc.)
            System.out.println("ALERT to " + op.getEmail() + 
                             ": Threshold violation on " + sensorName);
        }
    }
    
    // Notify when an entity is deleted
    public static void notifyDeletion(String username, String entityCode, 
                                      Class<?> entityClass) {
        System.out.println("DELETION: " + entityClass.getSimpleName() + 
                          " " + entityCode + " deleted by " + username);
    }
}
```

---

## 7. Operations Layer

### 7.1 OperationsFactory

**Location:** `com.weather.report.operations.OperationsFactory`

**Purpose:** Creates instances of operation implementations (Factory Pattern).

```java
public final class OperationsFactory {
    
    public static NetworkOperations getNetworkOperations() {
        return new NetworkOperationsImpl();
    }
    
    public static GatewayOperations getGatewayOperations() {
        return new GatewayOperationsImpl();
    }
    
    public static SensorOperations getSensorOperations() {
        return new SensorOperationsImpl();
    }
    
    public static TopologyOperations getTopologyOperations() {
        return new TopologyOperationsImpl();
    }
}
```

### 7.2 NetworkOperations (R1)

**Interface:** `com.weather.report.operations.NetworkOperations`
**Implementation:** `com.weather.report.operations.impl.NetworkOperationsImpl`

**Purpose:** Manage networks and operators.

| Method | Purpose | Exceptions |
|--------|---------|------------|
| `createNetwork(code, name, desc, username)` | Create a new network | `IdAlreadyInUseException`, `InvalidInputDataException`, `UnauthorizedException` |
| `updateNetwork(code, name, desc, username)` | Update network details | `ElementNotFoundException`, `InvalidInputDataException`, `UnauthorizedException` |
| `deleteNetwork(code, username)` | Delete a network | `ElementNotFoundException`, `InvalidInputDataException`, `UnauthorizedException` |
| `getNetworks(codes...)` | Get networks by codes | - |
| `createOperator(firstName, lastName, email, phone, username)` | Create an operator | `IdAlreadyInUseException`, `InvalidInputDataException`, `UnauthorizedException` |
| `addOperatorToNetwork(networkCode, email, username)` | Assign operator to network | `ElementNotFoundException`, `InvalidInputDataException`, `UnauthorizedException` |
| `getNetworkReport(code, startDate, endDate)` | Generate network report | `ElementNotFoundException`, `InvalidInputDataException` |

#### Code Validation:

```java
private static final Pattern NETWORK_CODE_PATTERN = Pattern.compile("^NET_\\d{2}$");

private void validateNetworkCode(String code) throws InvalidInputDataException {
    if (code == null || !NETWORK_CODE_PATTERN.matcher(code).matches()) {
        throw new InvalidInputDataException(
            "Invalid network code format. Must be NET_## where ## are two digits"
        );
    }
}
```

#### Authorization Check:

```java
private void validateMaintainer(String username) throws UnauthorizedException {
    if (username == null || username.isEmpty()) {
        throw new UnauthorizedException("Username is required");
    }
    
    User user = userRepository.read(username);
    if (user == null) {
        throw new UnauthorizedException("User not found");
    }
    if (user.getType() != UserType.MAINTAINER) {
        throw new UnauthorizedException("User is not authorized");
    }
}
```

### 7.3 GatewayOperations (R2)

**Interface:** `com.weather.report.operations.GatewayOperations`
**Implementation:** `com.weather.report.operations.impl.GatewayOperationsImpl`

**Purpose:** Manage gateways and their parameters.

| Method | Purpose |
|--------|---------|
| `createGateway(code, name, desc, username)` | Create a new gateway |
| `updateGateway(code, name, desc, username)` | Update gateway details |
| `deleteGateway(code, username)` | Delete a gateway |
| `getGateways(codes...)` | Get gateways by codes |
| `createParameter(gatewayCode, code, name, desc, value, username)` | Add a parameter to gateway |
| `updateParameter(gatewayCode, code, value, username)` | Update parameter value |
| `getGatewayReport(code, startDate, endDate)` | Generate gateway report |

#### Gateway Code Validation:

```java
private static final Pattern GATEWAY_CODE_PATTERN = Pattern.compile("^GW_\\d{4}$");
// Valid: GW_0001, GW_1234, GW_9999
// Invalid: GW_1, GW_12345, GATEWAY_01
```

### 7.4 SensorOperations (R3)

**Interface:** `com.weather.report.operations.SensorOperations`
**Implementation:** `com.weather.report.operations.impl.SensorOperationsImpl`

**Purpose:** Manage sensors and their thresholds.

| Method | Purpose |
|--------|---------|
| `createSensor(code, name, desc, username)` | Create a new sensor |
| `updateSensor(code, name, desc, username)` | Update sensor details |
| `deleteSensor(code, username)` | Delete a sensor |
| `getSensors(codes...)` | Get sensors by codes |
| `createThreshold(sensorCode, type, value, username)` | Add threshold to sensor |
| `updateThreshold(sensorCode, type, value, username)` | Update threshold |
| `getSensorReport(code, startDate, endDate)` | Generate sensor report |

#### Sensor Code Validation:

```java
private static final Pattern SENSOR_CODE_PATTERN = Pattern.compile("^S_\\d{6}$");
// Valid: S_000001, S_123456, S_999999
// Invalid: S_1, S_12345, SENSOR_01
```

### 7.5 TopologyOperations (R4)

**Interface:** `com.weather.report.operations.TopologyOperations`
**Implementation:** `com.weather.report.operations.impl.TopologyOperationsImpl`

**Purpose:** Manage relationships between networks, gateways, and sensors.

| Method | Purpose |
|--------|---------|
| `getNetworkGateways(networkCode)` | Get all gateways in a network |
| `connectGateway(networkCode, gatewayCode, username)` | Add gateway to network |
| `disconnectGateway(networkCode, gatewayCode, username)` | Remove gateway from network |
| `getGatewaySensors(gatewayCode)` | Get all sensors in a gateway |
| `connectSensor(sensorCode, gatewayCode, username)` | Add sensor to gateway |
| `disconnectSensor(sensorCode, gatewayCode, username)` | Remove sensor from gateway |

#### Topology Visualization:

```
Network (NET_01)
    │
    ├── Gateway (GW_0001)
    │       ├── Sensor (S_000001)
    │       └── Sensor (S_000002)
    │
    └── Gateway (GW_0002)
            ├── Sensor (S_000003)
            ├── Sensor (S_000004)
            └── Sensor (S_000005)
```

---

## 8. Exception Handling

### Custom Exceptions

| Exception | When Thrown | Example |
|-----------|-------------|---------|
| `WeatherReportException` | Base exception class | - |
| `InvalidInputDataException` | Invalid input data | Null code, wrong format |
| `IdAlreadyInUseException` | Duplicate ID | Creating network with existing code |
| `ElementNotFoundException` | Entity not found | Updating non-existent network |
| `UnauthorizedException` | Insufficient permissions | Viewer trying to create |

### Exception Hierarchy:

```
RuntimeException
    └── WeatherReportException (base)
            ├── InvalidInputDataException
            ├── IdAlreadyInUseException
            ├── ElementNotFoundException
            └── UnauthorizedException
```

---

## 9. Reports

### 9.1 NetworkReport

**Purpose:** Statistics about a network's measurements.

| Field | Description |
|-------|-------------|
| `code` | Network code |
| `startDate` / `endDate` | Report time range |
| `numberOfMeasurements` | Total measurements count |
| `mostActiveGateways` | Gateways with most measurements |
| `leastActiveGateways` | Gateways with fewest measurements |
| `gatewaysLoadRatio` | Percentage distribution per gateway |
| `histogram` | Measurements grouped by time intervals |

### 9.2 GatewayReport

**Purpose:** Statistics about a gateway's measurements.

| Field | Description |
|-------|-------------|
| `code` | Gateway code |
| `numberOfMeasurements` | Total measurements count |
| `mostActiveSensors` | Sensors with most measurements |
| `leastActiveSensors` | Sensors with fewest measurements |
| `sensorsLoadRatio` | Percentage distribution per sensor |
| `outlierSensors` | Sensors with anomalous readings |
| `batteryChargePercentage` | Gateway battery status |
| `histogram` | Measurements grouped by duration |

### 9.3 SensorReport

**Purpose:** Statistical analysis of a sensor's measurements.

| Field | Description |
|-------|-------------|
| `code` | Sensor code |
| `numberOfMeasurements` | Total measurements count |
| `mean` | Average value |
| `variance` | Statistical variance |
| `stdDev` | Standard deviation |
| `minimumMeasuredValue` | Lowest reading |
| `maximumMeasuredValue` | Highest reading |
| `outliers` | Anomalous measurements |
| `histogram` | Measurements grouped by value ranges |

---

## 10. Requirements Breakdown

### R1: Network Management

**Branch:** `1-r1-network`

**Entities:** Network, Operator

**Features:**
- Create/Update/Delete networks
- Create operators
- Assign operators to networks
- Generate network reports

**Test Count:** 28 tests

### R2: Gateway Management

**Branch:** `2-r2-gateway`

**Entities:** Gateway, Parameter

**Features:**
- Create/Update/Delete gateways
- Create/Update parameters
- Generate gateway reports

**Test Count:** 33 tests

### R3: Sensor Management

**Branch:** `3-r3-sensor`

**Entities:** Sensor, Threshold

**Features:**
- Create/Update/Delete sensors
- Create/Update thresholds
- Threshold violation checking
- Generate sensor reports

**Test Count:** 31 tests

### R4: Topology Management

**Branch:** `main` (after all merges)

**Features:**
- Connect/Disconnect gateways to networks
- Connect/Disconnect sensors to gateways
- Query topology relationships

**Test Count:** 21 tests (Test_R4 + Test_R4b)

---

## 11. How Everything Connects

### Complete Flow Example

Let's trace a complete scenario:

#### Step 1: Setup Network Infrastructure

```java
WeatherReport facade = new WeatherReport();

// Create a maintainer user
facade.createUser("admin", UserType.MAINTAINER);

// Create a network
Network net = facade.networks().createNetwork(
    "NET_01", "Northern Region", "Monitors northern area", "admin"
);

// Create an operator
Operator op = facade.networks().createOperator(
    "John", "Doe", "john@example.com", "+1234567890", "admin"
);

// Assign operator to network
facade.networks().addOperatorToNetwork("NET_01", "john@example.com", "admin");
```

#### Step 2: Setup Gateway and Sensors

```java
// Create a gateway
Gateway gw = facade.gateways().createGateway(
    "GW_0001", "Station Alpha", "Main weather station", "admin"
);

// Add battery parameter
facade.gateways().createParameter(
    "GW_0001", "BATTERY", "Battery Level", "Current charge", 85.0, "admin"
);

// Create sensors
Sensor temp = facade.sensors().createSensor(
    "S_000001", "Temperature Sensor", "Measures air temperature", "admin"
);

Sensor humid = facade.sensors().createSensor(
    "S_000002", "Humidity Sensor", "Measures humidity", "admin"
);

// Set thresholds
facade.sensors().createThreshold(
    "S_000001", ThresholdType.GREATER_THAN, 40.0, "admin"  // Alert if temp > 40
);

facade.sensors().createThreshold(
    "S_000002", ThresholdType.GREATER_THAN, 90.0, "admin"  // Alert if humidity > 90%
);
```

#### Step 3: Connect Topology

```java
// Connect gateway to network
facade.topology().connectGateway("NET_01", "GW_0001", "admin");

// Connect sensors to gateway
facade.topology().connectSensor("S_000001", "GW_0001", "admin");
facade.topology().connectSensor("S_000002", "GW_0001", "admin");
```

#### Step 4: Import Measurements

```java
// Import from CSV file
DataImportingService.storeMeasurements("csv/S_000001.csv");

// This will:
// 1. Read each line from CSV
// 2. Create Measurement entity
// 3. Check if value exceeds threshold
// 4. If violated → notify operators (John Doe receives email)
```

#### Step 5: Generate Reports

```java
// Get sensor statistics
SensorReport report = facade.sensors().getSensorReport(
    "S_000001", 
    "2025-01-01 00:00:00",  // Start date
    "2025-01-31 23:59:59"   // End date
);

System.out.println("Measurements: " + report.getNumberOfMeasurements());
System.out.println("Average: " + report.getMean());
System.out.println("Min: " + report.getMinimumMeasuredValue());
System.out.println("Max: " + report.getMaximumMeasuredValue());
```

### Data Flow Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                         USER REQUEST                              │
│                   "Create a network NET_01"                       │
└──────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│                      WeatherReport (Facade)                       │
│                    facade.networks().createNetwork(...)           │
└──────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│                      OperationsFactory                            │
│                 getNetworkOperations() → NetworkOperationsImpl    │
└──────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│                   NetworkOperationsImpl                           │
│  1. validateMaintainer(username) → Check user is MAINTAINER       │
│  2. validateNetworkCode(code) → Check format NET_##               │
│  3. Check if already exists → Throw if duplicate                  │
│  4. Create Network entity                                         │
│  5. Set audit fields (createdBy, createdAt)                       │
│  6. Call repository.create()                                      │
└──────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│                      CRUDRepository                               │
│  1. Get EntityManager                                             │
│  2. Begin transaction                                             │
│  3. em.persist(network)  → INSERT INTO Network ...                │
│  4. Commit transaction                                            │
│  5. Return saved entity                                           │
└──────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│                      H2 Database                                  │
│                 Network table: NET_01 inserted                    │
└──────────────────────────────────────────────────────────────────┘
```

---

## Summary

The Weather Report System is a well-architected Java application demonstrating:

1. **Clean Architecture** - Separation of concerns across layers
2. **Domain-Driven Design** - Rich entity model representing real-world concepts
3. **Repository Pattern** - Clean database abstraction
4. **Factory Pattern** - Flexible operation instantiation
5. **Facade Pattern** - Simple entry point for clients
6. **JPA/Hibernate** - Enterprise-grade persistence
7. **Comprehensive Testing** - 114 tests covering all requirements

The system enables monitoring organizations to:
- Manage hierarchical sensor networks
- Configure alerting thresholds
- Import measurement data
- Generate statistical reports
- Receive notifications on threshold violations

---

*Documentation generated for Weather Report System T135*
*Total Tests: 114 | All Passing ✅*
