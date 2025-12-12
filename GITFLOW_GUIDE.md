# 🌤️ Weather Report - GitFlow Guide for Team T135

## 📌 Repository URL
```
https://git-oop.polito.it/projects-2026/T135-weather-report.git
```

---

## 🌿 Branch Structure

| Branch | Requirement | Assigned To | Description |
|--------|-------------|-------------|-------------|
| `main` | - | Everyone (read) | Protected branch - contains merged code |
| `1-r1-network` | R1 | **1 person only** | Network & Operator management |
| `2-r2-gateway` | R2 | **1 person only** | Gateway & Parameter management |
| `3-r3-sensor` | R3 | **1 person only** | Sensor & Threshold management |
| `4-r4-topology` | R4 | **Everyone** | Integration (created AFTER R1-R3 merge) |

---

## 🚀 Getting Started

### Step 1: Clone the repository
```bash
git clone https://git-oop.polito.it/projects-2026/T135-weather-report.git
cd T135-weather-report
```

### Step 2: Switch to your assigned branch
```bash
# For R1 person:
git checkout 1-r1-network

# For R2 person:
git checkout 2-r2-gateway

# For R3 person:
git checkout 3-r3-sensor
```

---

## 📝 Daily Workflow

### Before starting work:
```bash
git pull origin <your-branch>
```

### After making changes:
```bash
git add .
git commit -m "R1: Description of what you did"
git push origin <your-branch>
```

### Commit message format:
- `R1: Added Network entity with JPA annotations`
- `R2: Implemented GatewayOperations interface`
- `R3: Added threshold validation logic`

---

## ⚠️ Important Rules

1. **ONE person per branch (R1, R2, R3)**
   - Only your commits should appear on your branch
   - Do NOT commit to someone else's branch

2. **Do NOT push directly to `main`**
   - Use Merge Requests (MR) on GitLab

3. **Keep your branch updated with main**
   ```bash
   git checkout main
   git pull origin main
   git checkout <your-branch>
   git merge main
   ```

---

## 🔄 Merge Request Process

When your requirement is complete:

1. Go to GitLab: https://git-oop.polito.it/projects-2026/T135-weather-report
2. Click **"Merge Requests"** → **"New merge request"**
3. Source branch: `1-r1-network` (or your branch)
4. Target branch: `main`
5. Add title: `R1: Network Operations Implementation`
6. Request review from teammates
7. After approval → Merge

---

## 📋 Requirement Summary

| Req | What to Implement | Key Classes |
|-----|-------------------|-------------|
| **R1** | Network CRUD, Operators, NetworkReport | `Network.java`, `Operator.java`, `NetworkOperationsImpl.java` |
| **R2** | Gateway CRUD, Parameters, GatewayReport | `Gateway.java`, `Parameter.java`, `GatewayOperationsImpl.java` |
| **R3** | Sensor CRUD, Thresholds, SensorReport, Statistics | `Sensor.java`, `Threshold.java`, `SensorOperationsImpl.java` |
| **R4** | Connect/Disconnect entities, Refactoring | `TopologyOperationsImpl.java` (after R1-R3 merge) |

---

## 🔧 Shared Files (Coordinate with team!)

These files are used by ALL requirements:
- `CRUDRepository.java`
- `persistence.xml`
- `OperationsFactory.java`
- `DataImportingService.java`

**⚡ Tip:** Discuss changes to shared files in the group before modifying!

---

## 📅 Timeline

1. **Phase 1:** Each person works on R1/R2/R3 independently
2. **Phase 2:** Create Merge Requests → Code Review → Merge to `main`
3. **Phase 3:** Create `4-r4-topology` branch from updated `main`
4. **Phase 4:** Everyone collaborates on R4 (integration & refactoring)

---

## ❓ Questions?

- Read the full requirements in `README.md`
- Check tests in `src/test/java/com/weather/report/test/base/`

Good luck team! 💪
