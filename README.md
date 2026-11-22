# Orquestador de Pagos

Microservicio en **Java 17** y **Spring Boot 4.1.2** para manejar órdenes, iniciar cobros contra una pasarela externa, recibir *webhooks* firmados de forma asíncrona y reconciliar pagos que se quedaron pendientes o con *timeout*.

Está pensado para que un reintento del cliente, una caída en mitad de una petición o un *webhook* que llega tarde o desordenado no provoquen cobros duplicados ni inconsistencias en el estado de la orden.

## Cómo funciona

* **Órdenes (`CREATED` -> `AWAITING_PAYMENT` -> `PAID`):** Se crea la orden primero y, al iniciar un pago, se bloquea con `SELECT ... FOR UPDATE` dentro de transacciones cortas.
* **Intentos de pago (`INITIATED` -> `REDIRECT_REQUIRED` / `REJECTED` / `UNKNOWN_TIMEOUT` -> `APPROVED` / `REJECTED`):** Cada solicitud exige el header `Idempotency-Key`. Si ya existe un intento con esa clave, se devuelve su respuesta anterior sin volver a llamar a la pasarela.
* **Llamadas externas fuera de transacción:** Las peticiones a la pasarela se hacen con `RestClient` y *timeouts* estrictos fuera de la transacción de base de datos, evitando mantener conexiones abiertas esperando red.
* **Webhooks + SQS:** Se valida `X-Signature` (`HMAC-SHA256`) y `X-Timestamp` (ventana de 5 minutos para prevenir *replay attacks*), se registra el evento por `providerEventId` de forma idempotente, se publica en **AWS SQS** y se responde `200 OK` rápido. Luego un consumidor procesa el evento, valida la máquina de estados (marcando `IGNORED_OUT_OF_ORDER` si llega fuera de orden), guarda en `payment_audit_logs` y borra el mensaje de SQS al terminar.
* **Reconciliación (`@Scheduled`):** Cada 2 minutos revisa intentos en estados transitorios que ya tienen cierta antigüedad, consulta el estado en la pasarela y aplica la misma regla de transición sin pasar por SQS.

## Tecnologías

* Java 17, Spring Boot 4.1.2, Spring Data JPA, `RestClient`
* PostgreSQL 16 con migraciones con Flyway
* AWS SQS (SDK v2)
* WireMock (con *mappings* y *__files* en el repositorio)
* Docker y Docker Compose

## Endpoints

* `POST /api/v1/orders` — Crea la orden en estado `CREATED` y registra el evento inicial en auditoría.
* `POST /api/v1/orders/{orderId}/payments` — Inicia un intento de pago (requiere `Idempotency-Key`).
* `GET /api/v1/orders/{orderId}` — Consulta la orden y sus intentos (útil tras volver de la pasarela o para *polling*).
* `POST /api/v1/webhooks/payments/{provider}` — Verifica firma y marca de tiempo, guarda el evento, publica en SQS y responde `200 OK`.

## Puesta en marcha y despliegue

### 1. Clonar y preparar el proyecto
Clona el repositorio y verifica que las carpetas `wiremock/mappings` y `wiremock/__files` estén en la raíz del proyecto:

```bash
git clone <url-del-repositorio>
cd <carpeta-del-proyecto>
```

*(Si despliegas en Linux y creas carpetas manualmente antes de levantar contenedores, asegúrate de que tu usuario tenga permisos sobre ellas para evitar conflictos con `root`).*

### 2. Ejecutar en local con Docker Compose + IDE (recomendado para debug)
Levanta PostgreSQL y WireMock en segundo plano:

```bash
docker compose up -d postgres-db wiremock-gateway
docker compose ps
```

Luego arranca la aplicación desde tu IDE en modo debug apuntando a `localhost:5432` para PostgreSQL y `http://localhost:8089` para WireMock, y asegúrate de tener `@EnableScheduling` habilitado para los procesos programados.

### 3. Desplegar todo el stack en local con Docker Compose
Construye y levanta PostgreSQL, WireMock y el microservicio juntos:

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f paymentgateway
```

Para actualizar el despliegue tras nuevos cambios en el código o en los mocks de WireMock:

```bash
git pull
docker compose up -d --build
docker compose restart wiremock-gateway
```

Para detener los servicios (conservando el volumen `postgres_data`):

```bash
docker compose down
```

### 4. Despliegue en servidor / entorno remoto
En un servidor Linux con Docker y Docker Compose instalados:

```bash
git clone <url-del-repositorio>
cd <carpeta-del-proyecto>
```

Crea un archivo `.env` (sin subirlo al repositorio) con las variables de conexión a PostgreSQL, pasarela y AWS SQS, y levanta el servicio:

```bash
docker compose --env-file .env up -d --build paymentgateway
docker compose logs -f --tail=100 paymentgateway
```

Si prefieres compilar el artefacto y ejecutarlo directamente como `.jar` en el servidor (con Maven o Gradle):

```bash
# Con Maven
./mvnw clean package -DskipTests
java -jar target/*.jar

# O con Gradle
./gradlew clean bootJar -x test
java -jar build/libs/*.jar
```