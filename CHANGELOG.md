# Changelog

Todos los cambios relevantes de este proyecto se documentan en este archivo.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto usa [Versionado Semántico](https://semver.org/lang/es/).

## [Unreleased]

## [0.2.0] - 2026-11-07

### Added
- CRUD de categorías en `/api/v1/categories` (`POST`, `GET`, `PUT`, `DELETE`) con DTOs, validación y recorte de espacios.
- Migración `V2__categorias.sql`: tabla `category` con nombre único sin distinguir mayúsculas.
- Errores estandarizados con `ProblemDetail` (RFC 9457): `400` con errores por campo, `404` y `409`.
- Listado de categorías paginado y ordenable (`PagedModel`; tamaño 20 por defecto y 100 como máximo; orden validado con lista blanca).
- Documentación OpenAPI y Swagger UI con springdoc (`/v3/api-docs`, `/swagger-ui.html`).
- Pruebas en tres capas: unitarias (Mockito), de slice (`@WebMvcTest`, `@DataJpaTest`) y de integración con PostgreSQL real, además de una prueba de concurrencia y otra del contrato OpenAPI.

### Changed
- El orden del listado añade `id` como último criterio para que la paginación no repita ni salte filas cuando hay empates.

## [0.1.0] - 2026-10-27

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

[Unreleased]: https://github.com/SrNe0/inventory-backend/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/SrNe0/inventory-backend/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/SrNe0/inventory-backend/releases/tag/v0.1.0
