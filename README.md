<div align="center">

# Am I Awake?

**Before you call, check if they're awake.**

Am I Awake? gives friends a little context before reaching out:<br>
who's awake, who welcomes a call, and who would rather get a message.

![Java](https://img.shields.io/badge/Java-backend-E76F00?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-API-6DB33F?style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-database-4169E1?style=flat-square)
![Kotlin](https://img.shields.io/badge/Kotlin-Android-7F52FF?style=flat-square)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-UI-4285F4?style=flat-square)

</div>

A Java backend with an Android companion app: device signals become sleep estimates, while users choose their
availability independently. The backend includes transactional state calculation, friendship access checks, and
wake subscriptions, with container images deployed to a VPS through GitHub Actions and GHCR.

## Features

- **Friends** — search, send and accept requests, and view accepted friends' states.
- **Availability** — choose Available, Text only, or Do not disturb, independently of sleep estimates.
- **Sleep estimates** — combine screen activity, unlocks, heartbeats, and Google Sleep API classifications.
- **Schedules** — interpret sleep windows in each user's time zone.
- **Wake notifications** — subscribe to an accepted friend's next detected wake transition; Firebase delivers the push.

Missing, stale, or inconclusive signals produce **Unknown**. An estimate describes available evidence, not certainty
that someone is asleep or ready for a call.

## Architecture and how it works

The backend is a single Spring Boot application, organized by domain in `src/main/java/com/amiawake/amiawake`.
Controllers and DTOs define the API; services handle application rules and transactions; repositories handle persistence.
Domains cover authentication, users and availability, friendship, sleep schedules, device events, sleep classifications,
user state, device registrations, and wake subscriptions.

```text
Android client -> REST API -> PostgreSQL
                     |
             State calculation
          (telemetry + schedule)
                     |
           SLEEPING -> AWAKE
                     |
                AFTER_COMMIT -> Wake subscribers -> Firebase push
```

1. The client submits device events and sleep classifications. Device-event IDs support deduplication with
   PostgreSQL `ON CONFLICT`; batches use JDBC inserts.
2. New telemetry triggers state calculation. A scheduler also recalculates every 15 minutes. Rules consider signal
   freshness and the user's schedule; future-dated events are excluded from inference.
3. A per-user `FOR NO KEY UPDATE` lock serializes telemetry ingestion and state calculation, including the first
   calculation before a state row exists. This prevents concurrent recalculations from publishing the same wake transition.
4. A fresh `SLEEPING -> AWAKE` transition publishes an application event. Its `AFTER_COMMIT` listener consumes one-shot
   subscriptions in a separate transaction, then sends notifications. Unregistered Firebase installations are removed.

**Delivery boundary:** push happens after the state transaction commits. It is best effort: there is no durable outbox
or retry queue, and consuming a subscription does not guarantee push delivery.

See [sleep inference rules and limitations](docs/inference-v1.md) for thresholds and freshness windows. Confidence is
a rule-based score, not measured prediction accuracy. Nighttime estimates tolerate delayed heartbeats and missing
motion readings; Google classifications are smoothed, and short gaps can retain previous sleep. Android sends motion
and charging events when available. Recent phone use is separate from the user's chosen availability for calls.

## Backend stack

| Technology | Role |
|:-----------|:-----|
| Java 21 · Spring Boot | REST API, request validation, centralized error responses. |
| Spring Security · JWT | RSA-signed access tokens, BCrypt passwords, rotating refresh tokens stored as SHA-256 hashes. |
| PostgreSQL · JPA/Hibernate · JDBC | Entity persistence, query projections, batch ingestion, database locks, and deduplication. |
| Flyway | Versioned schema migrations; Hibernate validates the schema rather than creating it. |
| Firebase Admin SDK | Push delivery and invalid device-registration cleanup. |
| Docker · Compose · GitHub Actions · GHCR | Container builds, image publishing, and VPS deployment. |

## Local development

Requires **JDK 21**, **Docker with Compose**, and **Firebase service-account credentials**. The repository includes
the Maven wrapper. Run commands from the repository root.

<details>
<summary>Configure the database, JWT keys, and Firebase</summary>

Set environment variables in the shell that will run the backend:

| Variable | Local value |
|:---------|:------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/am_i_awake` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | `postgres` |
| `GOOGLE_APPLICATION_CREDENTIALS` | Absolute path to your Firebase service-account JSON. |

These database credentials match `compose.yaml` and are intended for local development.
The Maven command does not automatically load a Compose `.env` file.

Place an RSA private key in PKCS#8 PEM format at `secrets/private.pem` and its public key in X.509 PEM format at
`secrets/public.pem`. If OpenSSL is installed, generate a local pair with:

```sh
mkdir -p secrets
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out secrets/private.pem
openssl pkey -in secrets/private.pem -pubout -out secrets/public.pem
```

In PowerShell, create the directory with `New-Item -ItemType Directory -Force secrets`; the OpenSSL commands are the same.
To use other locations, set `JWT_PRIVATE_KEY` and `JWT_PUBLIC_KEY` to `file:` resource paths.
Keep private keys and service-account credentials out of version control.

`compose.yaml` also defines a containerized backend and references `FIREBASE_CREDENTIALS_PATH` for its credential mount.
Set that variable to the absolute path of your Firebase JSON before using the Compose commands below.

</details>

Start only PostgreSQL, then run the backend on the host:

```sh
docker compose up -d postgres
./mvnw spring-boot:run
```

On Windows, use `./mvnw.cmd spring-boot:run`. The local API listens on `http://localhost:8080`.

Alternatively, with JWT keys in `secrets/` and `FIREBASE_CREDENTIALS_PATH` configured, run both services in containers:

```sh
docker compose up -d --build
```

Choose one backend startup method to avoid a port conflict.

### Android companion

Open `android/` in Android Studio; the client supports Android 10+ and uses Kotlin, Jetpack Compose, Retrofit,
a persistent event queue, and WorkManager synchronization. Add your Firebase client configuration at
`android/app/google-services.json` for push integration.

The default API address is `http://10.0.2.2:8080/` for the emulator. For a physical device, set the `apiBaseUrl`
Gradle property to a reachable backend URL, including the trailing `/`.

## Tests

Backend tests in `src/test/java` use **JUnit 5**, **Mockito**, and **PostgreSQL/Testcontainers**. They include inference
rules, authentication and friendship flows, concurrent state creation, single wake-event publication under concurrent
recalculation, and filtering of invalid event timestamps.

```sh
./mvnw test
```

On Windows, use `./mvnw.cmd test`. The full suite needs Docker; Spring context tests also need the JWT keys and Firebase
credentials described above. Testcontainers provisions its own PostgreSQL instance.

**Current CI scope:** the workflow and Dockerfile build with `-DskipTests`. Tests exist in the repository, but automated
test execution is not currently a deployment gate.

## Deployment

The [GitHub Actions workflow](.github/workflows/workflow.yml) builds the Maven artifact and Docker image on pushes
and pull requests. Pushes to `main` publish a commit-SHA-tagged image to GHCR, then deploy it over SSH:

```text
push to main -> Maven build -> Docker build -> GHCR image tagged with commit SHA
                                                        |
                                                  SSH to VPS
                                                        |
                                     Compose pulls and updates the backend
```

[Production Compose](compose.prod.yaml) runs the backend and PostgreSQL with a persistent database volume.
JWT keys and Firebase credentials are mounted read-only. The deployment job updates `BACKEND_IMAGE` in
`/opt/am-i-awake/.env.prod` and recreates the backend service.

The existing setup requires GitHub secrets `VPS_HOST`, `VPS_USER`, and `VPS_SSH_KEY`; the VPS also needs GHCR pull access,
the Compose file, and `.env.prod` with `BACKEND_IMAGE`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`,
`JWT_SECRETS_PATH`, and `FIREBASE_CREDENTIALS_PATH`. The deployment image owner is currently hard-coded in the workflow.

This is a production-like deployment for a small project. The checked-in Compose configuration exposes port 8080
and includes no HTTPS reverse proxy or backend healthcheck; the workflow does not verify application readiness after
deployment.

## API and documentation

- [Sleep inference specification](docs/inference-v1.md) — rules, thresholds, and client limitations.
- [Authentication controller](src/main/java/com/amiawake/amiawake/auth/controller/AuthController.java) — login, refresh, logout.
- [User controller](src/main/java/com/amiawake/amiawake/user/controller/UserController.java) — registration, profile, availability, search.
- [Friendship controller](src/main/java/com/amiawake/amiawake/friendship/controller/FriendshipController.java) — requests, friendships, friend states.
- [Android API interface](android/app/src/main/java/com/amiawake/android/data/AmIAwakeApi.kt) — client endpoint declarations.

The API uses the `/api/v1` prefix. Protected endpoints require `Authorization: Bearer <access-token>`.
The repository currently has no generated OpenAPI/Swagger reference.

## Authorship

**Backend:** designed and implemented by me, including the API, authentication, data model and migrations,
sleep inference, transactional state updates, wake subscriptions, and container deployment pipeline.

**Android companion:** developed substantially with ChatGPT Codex assistance to provide a client for the backend
and exercise the end-to-end flow. My primary engineering contribution and ownership are the Java backend.
