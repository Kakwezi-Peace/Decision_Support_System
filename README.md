# RwandAir Decision Support System

A Decision Support System (DSS) for cost-optimal aircraft delay recovery at RwandAir's Operations Control Centre (OCC). When a flight is delayed, the system generates every feasible recovery option — absorb the delay, cancel the flight, swap the aircraft, substitute crew, or reroute passengers — costs each one out, ranks them, and lets a dispatcher make an informed, evidence-based decision in seconds instead of minutes.

Built as a BSc final-year research project (Design Science Research methodology), the system pairs a Mixed-Integer Linear Programming (MILP) solver with a Reinforcement Learning (RL) agent that keeps improving from real-world outcomes.

## How it works

1. A delay is reported against a flight (category, minutes, cause).
2. The backend gathers current operational state — spare aircraft, available crew, connecting passengers — and sends it to the optimizer.
3. The optimizer runs two engines in parallel:
   - **MILP** (PuLP/CBC) enumerates every recovery option, costs it out (fuel, crew overtime, passenger compensation, ATC slot penalties, MRO), and formally solves for the minimum-cost feasible choice.
   - **RL** (a domain-seeded Q-learning agent) recommends an option based on a learned policy, for comparison.
4. All options come back ranked by cost, each tagged with its source (MILP/RL/manual), feasibility, and heuristic characterisation fields (passenger impact score, crew duty compliance, regulatory feasibility).
5. A dispatcher reviews the ranked list, optionally adds a manual override option (to capture tacit knowledge the model doesn't have), and selects one.
6. Once the recovery has actually played out, the dispatcher records the real cost. This closes the feedback loop: the RL agent's policy is updated from the real outcome, and the discrepancy is tracked for reporting.

## Architecture

```
frontend/    React 18 + TypeScript + Vite       — the OCC dashboard
backend/     Spring Boot 3 (Java 21)            — REST API, RBAC, persistence, orchestration
optimizer/   FastAPI (Python)                   — MILP + RL cost-optimization engine
```

The Spring Boot backend is the source of truth: it owns the Postgres database, enforces role-based access control, and calls the Python optimizer over HTTP for every recovery-options request. The optimizer is stateless except for its in-memory RL Q-table.

## Features

- **JWT authentication** with 6 roles mapped to real OCC staff categories (Admin, Operations Controller, Crew Scheduler, Maintenance Controller, Commercial Services, Senior Management), each scoped to the data domain they own.
- **Full CRUD** for Aircraft, Crew, Flights, Delay Events, Passengers, and Users, with pagination and recent-first ordering throughout.
- **Hybrid MILP + RL recovery engine** producing a ranked, costed list of options per delay event, with a manual override mechanism for controller judgment calls.
- **Feedback and learning loop** — record the actual cost of a recovery and the RL policy updates from it.
- **Recovery Analytics** — delay frequency and cost breakdowns by cause, decision-speed KPIs, and a DSS-vs-manual-baseline cost comparison.
- **Recovery History** — a searchable, filterable record of every past decision, with side-by-side scenario comparison.
- **Option characterisation** — passenger impact score, crew duty compliance, and regulatory feasibility on every option (documented heuristics — see `optimizer/option_scoring.py`).

## Tech stack

| Layer | Stack |
|---|---|
| Frontend | React 18, TypeScript, Vite, React Router |
| Backend | Spring Boot 3.3, Java 21, Spring Security (JWT), Spring Data JPA, PostgreSQL |
| Optimizer | Python, FastAPI, PuLP (CBC solver), NumPy |
| Tests | JUnit 5, Spring Boot Test, H2 (in-memory) |

## Running locally

**Prerequisites:** Java 21, Maven, Node.js, Python 3.11+, PostgreSQL.

### 1. Database

Create a PostgreSQL database and user matching your config (see step 2), e.g.:

```sql
CREATE DATABASE dss_rwandair;
CREATE USER dss_app WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE dss_rwandair TO dss_app;
```

### 2. Backend

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
# edit application.properties with your real DB password and a JWT secret
mvn spring-boot:run
```

Runs on `http://localhost:8080`. Table schema is created/updated automatically (`ddl-auto=update`), and a default admin account is seeded on first run from the credentials in `application.properties`.

### 3. Optimizer

```bash
cd optimizer
python -m venv .venv
.venv/Scripts/activate   # or source .venv/bin/activate on macOS/Linux
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

Runs on `http://localhost:8000`.

### 4. Frontend

```bash
cd frontend
npm install
npm run dev
```

Runs on `http://localhost:5173`.

## Running tests

```bash
mvn test
```

## Project structure

```
src/main/java/rw/ac/dss/
  controller/    REST endpoints
  service/       business logic and orchestration (RecoveryService is the core)
  model/         JPA entities
  repository/    Spring Data repositories
  dto/           request/response DTOs (dto/optimizer/ mirrors the Python API contract)
  security/      JWT filter, user details, token service
  config/        security config, CORS, admin seeding

optimizer/
  main.py              FastAPI app (/optimize, /feedback, /health)
  optimizer.py         MILP option generation and solving
  rl_agent.py          Q-learning agent
  option_scoring.py    passenger impact / crew duty / regulatory heuristics
  models.py            Pydantic schemas

frontend/src/
  pages/         one component per route
  components/    shared CRUD managers (Aircraft, Crew, Flight, Passenger)
  auth/          auth context and role-based permission checks
  api/           typed HTTP client
```
