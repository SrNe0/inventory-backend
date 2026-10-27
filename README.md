# inventory-backend

[![CI](https://github.com/SrNe0/inventory-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/SrNe0/inventory-backend/actions/workflows/ci.yml)
![Java 25](https://img.shields.io/badge/Java-25-orange)
![Spring Boot 4.1](https://img.shields.io/badge/Spring%20Boot-4.1-6db33f)
![PostgreSQL 18](https://img.shields.io/badge/PostgreSQL-18-336791)

API REST de control de inventario. Es solo backend: el frontend es otro proyecto que consumirá esta API.

## Problema

Llevar el inventario a mano o en hojas de cálculo suele romperse cuando varias personas consultan y actualizan existencias al mismo tiempo: se pierde el rastro de quién modificó el stock, cuándo ocurrió cada entrada o salida, qué productos están por debajo del stock mínimo para reponer y por qué una diferencia de conteo físico no cuadra con el saldo registrado.

Inicié este repositorio en 2022 con un CRUD básico de categorías en Spring Boot 2.7 y MySQL. En 2026 decidí retomarlo y reconstruirlo desde cero sobre Java 25, Spring Boot 4.1 y PostgreSQL 18 para reflejar cómo diseño y entrego un servicio backend hoy. Ya tengo migraciones versionadas con Flyway y pruebas de integración contra una base de datos real; el resto (control de concurrencia en movimientos de stock, errores estandarizados con `ProblemDetail` (RFC 7807) y seguridad stateless con JWT) lo construyo por etapas, como se ve en el [roadmap](#roadmap).

El alcance se centra en la API como producto (categorías, productos con filtro de stock bajo y movimientos de inventario auditables e inmutables), dejando fuera de este repositorio el cliente frontend y módulos accesorios como gestión de proveedores o exportación de archivos.

## Demo

Todavía no está desplegada. La URL de la API, Swagger y las credenciales de demo llegan con la versión `v1.0.0`.

## Stack

| Pieza | Versión |
|---|---|
| Java | 25 (LTS) |
| Spring Boot | 4.1.2 |
| PostgreSQL | 18 |
| Flyway | gestionado por Spring Boot |
| Pruebas | JUnit, Testcontainers |
| Build | Maven 3.9.16 (vía `mvnw`) |
| CI | GitHub Actions |

Las versiones las gestiona el BOM de Spring Boot siempre que es posible.

## Arquitectura

Organizo el código **por funcionalidad** (*package-by-feature*), no por capa técnica. Esta es la estructura que sigo, y se irá llenando por etapas:

```
src/main/java/io/github/srne0/inventory/
├── category/    # entidad, repositorio, servicio, controlador, dto/
├── product/
├── movement/
├── user/
├── auth/        # login y emisión de JWT
└── common/      # errores (ProblemDetail), configuración, paginación
```

Hoy solo existe la clase `InventoryApplication`. El modelo de datos se agrega junto con cada entidad.

## Cómo correrlo

### Con Docker y Maven

Necesitas JDK 25 y Docker.

1. Copia las variables de ejemplo y cambia `DB_PASSWORD`:

   ```bash
   cp .env.example .env        # en Windows (cmd): copy .env.example .env
   ```

2. Levanta PostgreSQL:

   ```bash
   docker compose up -d
   docker compose ps           # inventory-postgres debe quedar "healthy"
   ```

3. Arranca la API. Spring Boot **no lee `.env`**, solo Docker Compose lo lee, así que exporto las variables antes de ejecutar:

   ```bash
   set -a && . ./.env && set +a
   ./mvnw spring-boot:run
   ```

   Si no usas una terminal tipo bash, define `DB_URL`, `DB_USER` y `DB_PASSWORD` en el entorno del proceso (por ejemplo, en la configuración de ejecución de tu IDE).

Si el puerto `5432` está ocupado, cambia `DB_PORT` y el puerto dentro de `DB_URL` (deben coincidir). Para cambiar el puerto de la API, define `SERVER_PORT`.

Para apagar la base de datos: `docker compose down` (con `-v` también borra los datos).

### Con Dev Container o GitHub Codespaces

Abre el repositorio en VS Code con la extensión Dev Containers y elige **Reopen in Container**, o crea un Codespace. El entorno trae Java 25 (Temurin) y docker-in-docker; crea `.env` si no existe y levanta PostgreSQL solo. Las variables `DB_*` ya vienen definidas en el contenedor.

### Variables de entorno

| Variable | Para qué sirve | Ejemplo |
|---|---|---|
| `DB_NAME` | Nombre de la base (Docker Compose) | `inventory` |
| `DB_USER` | Usuario de la base | `inventory` |
| `DB_PASSWORD` | Contraseña de la base (obligatoria) | `cambia-esta-clave` |
| `DB_PORT` | Puerto publicado por Docker Compose | `5432` |
| `DB_URL` | URL JDBC que usa la API | `jdbc:postgresql://localhost:5432/inventory` |

`.env` no se versiona. Ningún secreto vive en el repositorio.

## Pruebas

```bash
./mvnw verify
```

Necesita Docker en marcha: la prueba de contexto levanta un PostgreSQL 18 real con Testcontainers y aplica las migraciones de Flyway. No necesita variables de entorno. La misma verificación corre en CI en cada pull request.

## Decisiones técnicas

- **Flyway con `ddl-auto: validate` en lugar de `update`**: delego todos los cambios de esquema a scripts SQL versionados en Flyway y dejo a Hibernate únicamente validando que las entidades coincidan al arrancar. Si falta una migración o una columna no cuadra, la aplicación falla de inmediato antes de recibir tráfico.
- **Testcontainers con PostgreSQL 18 real en lugar de H2**: ejecuto las pruebas de integración contra el mismo motor de base de datos que corre en producción usando `@ServiceConnection`. Así evito falsos positivos por diferencias de dialecto SQL, tipos de datos o restricciones (`CHECK`) y el CI no necesita secretos de base de datos.
- **Configuración por variables de entorno sin contraseñas por defecto (*fail-fast*)**: ni `application.yaml` ni `compose.yaml` incluyen contraseñas por defecto. Si falta `DB_PASSWORD`, tanto Docker Compose como Spring Boot abortan el arranque de forma explícita en vez de iniciar con credenciales inseguras.
- **`open-in-view: false`**: desactivo *Open Session in View* para que la sesión de JPA y la conexión a la base de datos se cierren al terminar la transacción en la capa de servicio, evitando retener conexiones durante la serialización JSON. Una carga perezosa fuera de la transacción falla de forma explícita (`LazyInitializationException`) en vez de ejecutar consultas SQL ocultas.
- **Dev Container con Temurin 25, `docker-in-docker` y `remoteEnv`**: uso la imagen base de Debian con la feature de Java configurada en Temurin (`jdkDistro: tem`) para igualar el JDK del CI, e inyecto las variables de ejemplo mediante `remoteEnv` en lugar de `spring.config.import: optional:file:.env` para no acoplar la configuración de producción a archivos locales.
- **Rama `main` protegida y flujo por PR**: todo cambio entra por pull request con el job `build` (`./mvnw -B verify`) en verde y la rama actualizada respecto a `main`.

## Roadmap

| Etapa | Versión | Contenido | Estado |
|---|---|---|---|
| 0. Base limpia | `v0.1.0` | Java 25 + Spring Boot 4.1, PostgreSQL + Flyway, CI, Dev Container, documentación | ✅ |
| 1. Categorías y cimientos de la API | `v0.2.0` | CRUD, DTOs, validación, `ProblemDetail`, paginación, OpenAPI | ⏳ |
| 2. Productos | `v0.3.0` | SKU único, baja lógica, filtros, bloqueo optimista | ⏳ |
| 3. Seguridad | `v0.4.0` | Usuarios, JWT, roles `ADMIN`/`OPERATOR`, CORS | ⏳ |
| 4. Movimientos de inventario | `v0.5.0` | Entradas, salidas y ajustes inmutables, concurrencia | ⏳ |
| 5. Contenedores y CI/CD | `v0.6.0` | Docker, Actuator, imagen en GHCR, Caddy | ⏳ |
| 6. Despliegue y demo | `v1.0.0` | Despliegue público, perfil demo, README final | ⏳ |

✅ terminada · 🚧 en curso · ⏳ pendiente

## Licencia

[MIT](LICENSE)
