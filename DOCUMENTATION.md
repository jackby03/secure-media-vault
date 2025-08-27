Plataforma de Gestión y Procesamiento de Archivos a Gran Escala

1. Descripción General

La plataforma permitirá a los usuarios subir, almacenar, procesar y buscar archivos de gran tamaño (hasta 5GB) de forma segura y escalable. El sistema deberá estar diseñado bajo una arquitectura reactiva y resiliente, integrando múltiples tecnologías para cubrir almacenamiento, procesamiento asíncrono, seguridad, observabilidad y despliegue automatizado.

2. Requerimientos Funcionales
   2.1. Autenticación y Autorización

Implementar JWT con refresh tokens para sesiones seguras.

Soporte para RBAC (Role-Based Access Control) con roles:

Admin: gestión completa de usuarios, archivos y sistema.

Editor: subir, procesar y compartir archivos.

Viewer: acceso restringido a descargas y visualización.

Signed URLs para descargas temporales y seguras.

Validaciones de entrada siguiendo las guías OWASP (inyecciones, XSS, CSRF, brute force).

2.2. Gestión de Archivos

API reactiva para:

Subida de archivos grandes (hasta 5GB) mediante resumable uploads y chunked transfer.

Descarga de archivos con soporte de reanudación.

Los archivos deberán almacenarse en MinIO (S3-compatible).

Metadatos de archivos registrados en base de datos:

ID único, nombre, tamaño, tipo, usuario propietario, estado (pendiente, procesando, listo, fallido), hash, etiquetas, timestamps.

2.3. Procesamiento Asíncrono

Cada archivo subido genera un evento en RabbitMQ.

Un worker reactivo procesa el archivo (ejemplo: conversión con FFmpeg, análisis de metadatos, compresión).

Actualización de estado en base de datos al finalizar o fallar.

2.4. Búsqueda

PostgreSQL Full Text Search sobre nombre, descripción y etiquetas.

Posibilidad de integrar Elasticsearch para búsquedas más avanzadas.

2.5. Cache y Rendimiento

Redis como caché de:

Metadatos de archivos más accedidos.

Sesiones temporales de uploads resumibles.

Control de rate limiting por usuario.

2.6. Observabilidad

Logs estructurados en formato JSON.

Prometheus para recolectar métricas expuestas vía /actuator/prometheus.

OpenTelemetry para trazas distribuidas (API + workers).

2.7. Resiliencia

Uso de Resilience4j en interacciones críticas:

Llamadas a MinIO (almacenamiento).

RabbitMQ (procesamiento).

Redis (caché).

Configuración de retries, timeouts, bulkheads y circuit breakers.

2.8. CI/CD y DevOps

Pipeline en GitHub Actions con pasos de:

Build y análisis estático.

Ejecución de tests unitarios y de integración.

Construcción de imágenes Docker.

Despliegue en entorno con Docker Compose (servicios: app, PostgreSQL, Redis, RabbitMQ, MinIO, Prometheus, Grafana).

Configuración de entornos de desarrollo, staging y producción.

2.9. Testing y Calidad

80%+ de cobertura de tests con:

JUnit 5 (unit testing).

MockK (mocks).

Reactor Test para flujos reactivos.

Tests de integración para API, base de datos, Redis y RabbitMQ.

3. Requerimientos No Funcionales

Escalabilidad: arquitectura basada en microservicios y componentes distribuidos.

Disponibilidad: tolerancia a fallos mediante resiliencia y redundancia de servicios.

Seguridad: cumplimiento de OWASP Top 10, cifrado en tránsito (HTTPS/TLS).

Rendimiento: tiempos de respuesta < 200ms en consultas de metadatos; soporte para concurrencia alta en uploads.

Mantenibilidad: código modular, documentado y con arquitectura clara.

Portabilidad: despliegue con Docker Compose y soporte para migrar a Kubernetes.

4. Tecnologías a Utilizar

Lenguaje y Framework: Kotlin + Spring Boot 3 + WebFlux.

Base de datos: PostgreSQL 15 + R2DBC.

Cache: Redis.

Mensajería: RabbitMQ.

Almacenamiento: MinIO (S3-compatible).

Procesamiento opcional: FFmpeg.

Búsqueda avanzada (opcional): Elasticsearch.

Resiliencia: Resilience4j.

Testing: JUnit 5, MockK, Reactor Test.

CI/CD: GitHub Actions + Docker Compose.

Observabilidad: Prometheus, Grafana, OpenTelemetry, logs en JSON.

5. Alcance del MVP

Subida y descarga de archivos grandes.

Persistencia en MinIO y PostgreSQL.

Procesamiento básico asíncrono con RabbitMQ.

JWT + RBAC funcionales.

Redis para cache de metadatos.

Logs estructurados + métricas básicas.

6. Extensiones Futuras

Integración con Elasticsearch.

Procesamiento multimedia con FFmpeg.

Signed URLs con políticas más avanzadas (ej. expiración dinámica).

Dashboard en Grafana con métricas de uso y estado de procesamiento.

Migración a Kubernetes para orquestación avanzada.