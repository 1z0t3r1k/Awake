<div align="center">

# Awake?

![Java](https://img.shields.io/badge/Java-backend-E76F00?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-API-6DB33F?style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-database-4169E1?style=flat-square)

**Awake?** is a social availability app that helps friends understand whether it is a good time to call, send a message, or wait until later.

Online presence alone is ambiguous: a person may be awake but busy, available for messages but not calls, or asleep in another time zone. Awake? is intended to make that context explicit.

</div>

## Project status

The repository currently focuses on the Java backend. Authentication and session management are implemented; the product features and Android client are the next part of the project.

### Implemented

- User registration and authentication
- Short-lived JWT access tokens
- Opaque refresh tokens generated with `SecureRandom`
- Refresh tokens stored as hashes rather than raw values
- Refresh token rotation using one database record per session
- Logout through refresh token revocation
- Request validation and authentication error responses
- PostgreSQL persistence with Flyway migrations

### Product scope

| Feature                 | Purpose                                           |
| :---------------------- | :------------------------------------------------ |
| **Availability status** | Show whether calls or messages are welcome        |
| **Friends**             | Share availability with a defined circle of users |
| **Sleep schedule**      | Reflect expected sleeping hours automatically     |
| **Time-zone awareness** | Present availability in the correct local context |
| **Android client**      | Provide a native Kotlin interface for the service |

The planned availability states are **Available**, **Text only**, **Do not disturb**, and **Sleeping**.

## Backend architecture

The diagram shows how authentication, request processing, and persistence connect inside the backend.

```mermaid
flowchart TB
    Client[API client]

    subgraph Application[Spring Boot application]
        Security[Spring Security filter chain]
        JwtFilter[JWT authentication filter]
        Jackson[Jackson and Bean Validation]
        Controller[REST controllers]
        Service[Application services]
        Repository[Spring Data JPA repositories]

        Security --> JwtFilter
        JwtFilter --> Jackson
        Jackson --> Controller
        Controller --> Service
        Service --> Repository
    end

    Hibernate[Hibernate]
    Database[(PostgreSQL)]
    Flyway[Flyway]

    Client -->|HTTP and JSON| Security
    Repository --> Hibernate
    Hibernate --> Database
    Flyway -->|versioned migrations| Database
    Controller -->|HTTP response| Client
```

## Authentication flow

Access and refresh tokens have separate responsibilities. Access tokens are short-lived JWTs used for API authorization. Refresh tokens are opaque values whose hashes are persisted, allowing sessions to be rotated and explicitly revoked.

```mermaid
flowchart LR
    Login[Login] --> Auth[Authentication service]
    Auth --> Access[JWT access token]
    Auth --> RawRefresh[Raw refresh token]

    Access -->|Bearer token| JwtFilter[JWT filter]
    JwtFilter -->|valid| Request[Authenticated request]

    RawRefresh -->|hash| Stored[(Refresh token record)]
    RawRefresh -->|refresh request| Validate{Active and not expired?}
    Stored --> Validate

    Validate -->|yes| Rotate[Generate token and replace stored hash]
    Rotate --> NewAccess[New access token]
    Rotate --> NewRefresh[New refresh token]
    NewRefresh -->|hash| Stored

    Validate -->|no| Unauthorized[401 Unauthorized]
    Logout[Logout] -->|set revoked timestamp| Stored
```

After rotation, the previous refresh token no longer matches the stored hash and cannot be used again. Logout marks the session record as revoked.

<div align="center">

## Tech stack

| Area                    | Technologies                                              |
| :---------------------- | :-------------------------------------------------------- |
| **Backend**             | Java, Spring Boot                                         |
| **Authentication**      | Spring Security, JWT access tokens, opaque refresh tokens |
| **Persistence**         | PostgreSQL, Spring Data JPA, Hibernate                    |
| **Database migrations** | Flyway                                                    |
| **API boundary**        | Jackson, Bean Validation                                  |
| **Mobile client**       | Kotlin, Android _(planned)_                               |

</div>
