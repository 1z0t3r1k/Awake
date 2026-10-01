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

## Features

- **Friends** — search, friend requests, and shared availability.
- **Availability** — Available, Text only, or Do not disturb.
- **Sleep estimates** — screen activity, unlocks, heartbeats, and sleep signals.
- **Schedules** — sleeping hours interpreted in each person's time zone.

When signals are stale or insufficient, the app shows **Unknown**.

## Backend

The backend in `src/` is organized by domain, with controllers, services, repositories, and DTOs. Sleep inference uses
explicit rules and signal freshness; a scheduler refreshes stored states.

| Technology                         | Implementation                                                                               |
|:-----------------------------------|:---------------------------------------------------------------------------------------------|
| Java 21 · Spring Boot              | REST API, request validation, centralized error handling.                                    |
| Spring Security                    | RSA-signed JWTs, BCrypt passwords, rotating refresh tokens stored as SHA-256 hashes.         |
| PostgreSQL · JPA · JDBC            | Entity persistence, SQL projections, batch inserts, event deduplication with `ON CONFLICT`.  |
| Flyway · Docker Compose            | Versioned migrations and a local PostgreSQL environment.                                     |
| Spring transactions & events       | Per-user database locks serialize state calculation; wake notifications run after commit.    |
| Firebase Admin SDK                 | Push delivery and handling of invalid device registrations.                                  |
| JUnit 5 · Mockito · Testcontainers | Unit tests and PostgreSQL integration tests for concurrent inference and invalid timestamps. |

## Local development

Requires JDK 21, Docker, and Firebase server credentials.

<details>
<summary>Environment and credentials</summary>

- Set `DB_URL` to `jdbc:postgresql://localhost:5432/am_i_awake`, with `DB_USERNAME=postgres` and `DB_PASSWORD=postgres`
  for the local database.
- Place RSA keys at `secrets/private.pem` (PKCS#8) and `secrets/public.pem` (X.509). Custom paths use `JWT_PRIVATE_KEY`
  and `JWT_PUBLIC_KEY` with a `file:` prefix.
- Set `GOOGLE_APPLICATION_CREDENTIALS` to the path of your Firebase service account JSON.

</details>

From the repository root:

```sh
docker compose up -d
./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd spring-boot:run`.

Open `android/` in Android Studio (Android 10+). The emulator uses `http://10.0.2.2:8080/`; a real phone needs a
reachable backend address. Android push requires its own `android/app/google-services.json`.

## Authorship

- **Backend** — written by me: API, authentication, persistence, and application logic.
- **Android** — developed by ChatGPT Codex, not by me. Kotlin, Jetpack Compose, Retrofit, a persistent event queue, and
  WorkManager sync.

---

<div align="center">

[Sleep inference rules](docs/inference-v1.md)

</div>
