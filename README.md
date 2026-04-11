# Spring PetClinic 

A demo project accompanying a talk on **Spec-Driven Development with the AI Unified Process (AIUP)**.

It revisits the classic Spring PetClinic sample, but built from the ground up using specifications first — use cases, an
entity model, and UI flows — and then letting AI assistants implement the code against those specs.

## AI Unified Process

Spec-Driven Development (SDD) flips the usual "prompt and pray" workflow on its head. Instead of asking an AI to produce
code from a one-line request, you invest upfront in a precise, machine-readable specification of what the system should
do. The AI then works *against* that spec — generating code, tests, and documentation that can be verified against a
stable source of truth.

The [**AI Unified Process (AIUP)**](https://unifiedprocess.ai/) is a lightweight adaptation of the Unified Process for
AI-assisted development. It keeps the artifacts that matter — use cases, domain models, architectural decisions — and
drops the ceremony that doesn't. The result is a workflow where humans stay in charge of *intent* and AI handles the
mechanical translation to code.

This repository is the running example used in the talk.

## Stack

- **Java 25**
- **Spring Boot 4.0**
- **Vaadin 25** — UI
- **jOOQ** — type-safe SQL
- **Flyway** — database migrations
- **PostgreSQL** (via Testcontainers for tests)

## Specs

The specifications that drive the implementation live in [`docs/`](docs/):

- [`docs/entity_model.md`](docs/entity_model.md) — the domain model
- [`docs/use_cases.puml`](docs/use_cases.puml) — PlantUML use case diagram
- [`docs/use_cases/`](docs/use_cases) — individual use case specifications

## Running locally

```bash
./mvnw spring-boot:test-run
```

A PostgreSQL instance is required. Tests use Testcontainers and need Docker running:

```bash
./mvnw test
```

## Structure

```
docs/        — specifications (the source of truth)
src/main/   — implementation derived from the specs
src/test/   — tests verifying the implementation against the specs
```
