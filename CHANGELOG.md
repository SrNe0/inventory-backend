# Changelog

Todos los cambios relevantes de este proyecto se documentan en este archivo.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto usa [Versionado Semántico](https://semver.org/lang/es/).

## [Unreleased]

### Added
- Flyway para las migraciones del esquema, con la migración inicial `V1__esquema_inicial.sql`.
- PostgreSQL 18 como base de datos, con `compose.yaml` para desarrollo local y `.env.example` con las variables de entorno.
- Prueba de contexto con Testcontainers y PostgreSQL (`@ServiceConnection`).
- Workflow de integración continua con GitHub Actions (`build`: JDK 25, `./mvnw -B verify`).
- Dev Container con Java 25, PostgreSQL y docker-in-docker, compatible con VS Code y GitHub Codespaces.
- `README.md`, `CHANGELOG.md` y licencia MIT.

### Changed
- Migración a Java 25 y Spring Boot 4.1.2.
- El paquete base pasa de `com.company.inventory` a `io.github.srne0.inventory`.
- Maven Wrapper actualizado a Maven 3.9.16 en modo *only-script* (sin `maven-wrapper.jar` versionado).
- Fines de línea normalizados con `.gitattributes` y `.gitignore` actualizado.
- La configuración de la base de datos se lee solo desde variables de entorno.

### Removed
- Conector de MySQL.
- Ramas `develop` y `feature/category`.
