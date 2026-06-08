# Passwordy Backend

Spring Boot REST API for password management.

## Running

```bash
# Default profile: in-memory H2, no setup required
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080` (H2 console at `/h2-console`).

For the PostgreSQL `local` / `docker` profiles and the required secret configuration,
see [SETUP.md](../SETUP.md) in the project root.

## Building & Testing

```bash
./mvnw clean install
```

> Tests run on in-memory **H2** — no database or Docker required.

## Documentation

- **API reference:** [API.md](../API.md)
- **Design rationale:** [DECISIONS.md](../DECISIONS.md)
- **Per-class behavior and contracts:** documented inline as Javadoc on each class
  (controllers, services, security, encryption, etc.).
