# Distributed Rate Limiter

A reusable **Spring Boot distributed rate-limiting library** built with **Redis, Lua scripting, and Spring AOP**.

The library provides annotation-based rate limiting for Spring Boot applications and supports configurable rules and request-key resolution.

RateGuard — A reusable Spring Boot Redis-based rate-limiting library designed to protect APIs from excessive traffic using configurable rules and atomic Lua scripts.

## 🚀 Features

* ✅ Redis-based distributed rate limiting
* ✅ Annotation-based configuration
* ✅ Spring AOP integration
* ✅ Atomic rate-limit validation using Lua scripts
* ✅ Fixed-window rate-limiting strategy
* ✅ Configurable request limits and time windows
* ✅ Multiple key-resolution strategies

  * HTTP Header
  * Request Parameter
  * Path Variable
  * Request Body
  * Composite keys
* ✅ Thread-safe implementation
* ✅ Supports multiple application instances
* ✅ Centralized configuration
* ✅ Fail-closed behavior when Redis is unavailable
* ✅ Custom exception handling
* ✅ Unit and integration test support

---

# 🏗️ Architecture

```text
                    Client
                      |
                      v
              Spring Boot API
                      |
                      v
              @RateLimited
                      |
                      v
                RateLimitAspect
                      |
                      v
              Key Resolver
                      |
                      v
              Redis Rate Limiter
                      |
                      v
                 Lua Script
                      |
                      v
                    Redis
```

## Distributed Architecture

```text
                 ┌───────────────┐
                 │    Client     │
                 └───────┬───────┘
                         |
                         v
              ┌─────────────────────┐
              │ Load Balancer / ALB │
              └─────────┬───────────┘
                        |
              ┌─────────┴─────────┐
              |                   |
              v                   v
        ┌───────────┐       ┌───────────┐
        │ Instance 1│       │ Instance 2│
        │ Spring    │       │ Spring    │
        │ Boot      │       │ Boot      │
        └─────┬─────┘       └─────┬─────┘
              |                   |
              └─────────┬─────────┘
                        |
                        v
                ┌──────────────┐
                │    Redis     │
                │ Shared State │
                └──────────────┘
```

Because all application instances use the same Redis store, the rate limit is maintained across multiple instances.

---

# 💡 Why Distributed Rate Limiting?

A local in-memory rate limiter works only inside a single application instance.

For example:

```text
Limit = 10 requests / 2 minutes
```

If the application has three instances:

```text
Instance 1 → 10 requests
Instance 2 → 10 requests
Instance 3 → 10 requests
```

A local implementation could effectively allow:

```text
30 requests
```

instead of the intended:

```text
10 requests
```

A Redis-based implementation maintains the counter centrally:

```text
Instance 1 ─┐
Instance 2 ─┼──> Redis Counter
Instance 3 ─┘
```

Therefore, the limit is applied consistently across instances.

---

# 🔐 How Rate Limiting Works

For every incoming request:

1. `@RateLimited` identifies the API that requires rate limiting.
2. `RateLimitAspect` intercepts the request.
3. `RateLimitKeyResolver` extracts the configured key.
4. The library generates a Redis key.
5. A Lua script executes atomically in Redis.
6. Redis checks the current request count.
7. If the limit is not reached, the request is allowed.
8. If the limit is exceeded, the request is rejected.

Example:

```text
Request
   |
   v
Extract Mobile Number
   |
   v
rate_limit:mobile:9876543210
   |
   v
Redis Lua Script
   |
   +---- Allowed ----> Controller
   |
   +---- Exceeded ---> 429 Too Many Requests
```

---

# 🧩 Usage

## 1. Add the Dependency

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>distributed-rate-limiter</artifactId>
    <version>1.0.0</version>
</dependency>
```

> Update the dependency coordinates according to the published artifact.

---

# 2. Configure Redis

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

---

# 3. Configure Rate Limiting

```yaml
rate-limit:
  enabled: true
```

Example rule:

```yaml
rate-limit:
  rules:
    customer-auth:
      limit: 10
      window-seconds: 120
      key-name: customer-auth
      key-type:
        - location: HEADER
          name: X-API-KEY
```

This configuration allows:

```text
10 requests
within 120 seconds
per API key
```

---

# 4. Apply Rate Limiting

```java
@RateLimited(rule = "customer-auth")
@PostMapping("/customer/authenticate")
public ResponseEntity<String> authenticate(
        @RequestBody CustomerAuthRequest request) {

    return ResponseEntity.ok("Authenticated");
}
```

---

# 🔑 Supported Key Types

## Header

```yaml
key-type:
  - location: HEADER
    name: X-API-KEY
```

Example:

```text
X-API-KEY: abc123
```

Redis key:

```text
rate_limit:customer-auth:abc123
```

---

## Request Parameter

```yaml
key-type:
  - location: PARAM
    name: mobile
```

Request:

```text
/customer/authenticate?mobile=9876543210
```

---

## Path Variable

```yaml
key-type:
  - location: PATH
    name: customerId
```

Request:

```text
/customer/12345
```

---

## Request Body

Example request:

```json
{
  "mobile": "9876543210",
  "name": "Customer"
}
```

Configuration:

```yaml
key-type:
  - location: BODY
    name: mobile
```

---

# 🔗 Composite Keys

Multiple request attributes can be combined to create a unique rate-limit key.

Example:

```yaml
key-type:
  - location: HEADER
    name: X-API-KEY

  - location: HEADER
    name: X-MOBILE
```

Generated Redis key:

```text
rate_limit:customer-auth:api-key-value:mobile-value
```

This allows rate limiting based on a combination of request attributes.

---

# ⚡ Atomic Redis Operation

The rate limiter uses a **Lua script** to perform the rate-limit operation atomically.

Conceptually:

```text
GET current count

IF count >= limit
    reject request
ELSE
    increment count
    set expiration
    allow request
END
```

Without atomic execution, concurrent requests could cause race conditions:

```text
Request A → GET count = 9
Request B → GET count = 9

Request A → INCREMENT
Request B → INCREMENT

Unexpected result
```

With Lua scripting, Redis executes the complete operation atomically.

```text
Request A ─┐
Request B ─┼──> Redis Lua Script
Request C ─┘

        ↓

Atomic decision
```

---

# ⏱️ Fixed Window Strategy

The current implementation uses a **fixed-window** strategy.

Example:

```text
Limit: 10 requests
Window: 120 seconds
```

The request count is maintained for the configured window.

```text
0 sec                    120 sec
 |--------------------------|
       10 requests max
```

After the window expires, the counter is reset through Redis key expiration.

---

# 🛡️ Fail-Closed Behavior

If Redis becomes unavailable, the rate limiter can be configured to reject requests rather than bypassing the protection.

```text
Application
     |
     v
Redis unavailable
     |
     v
Rate limiter
     |
     v
Reject request
```

This prevents an infrastructure failure from unintentionally disabling rate limiting.

---

# ❌ Rate Limit Exceeded

When the configured limit is exceeded, the application returns:

```http
HTTP/1.1 429 Too Many Requests
```

Example response:

```json
{
  "status": 429,
  "message": "Rate limit exceeded"
}
```

---

# 🧪 Testing

The project includes tests covering important scenarios.

### Basic scenarios

* Request within configured limit
* Request exceeding configured limit
* Redis key expiration
* Multiple users / keys
* Different rate-limit rules

### Concurrency scenarios

* Multiple simultaneous requests
* Concurrent requests for the same key
* Requests for different keys
* Atomic Lua-script execution

### Failure scenarios

* Redis unavailable
* Invalid configuration
* Missing request key
* Unsupported key type

Example:

```text
Limit = 3 requests / 60 seconds

Request 1 → 200 OK
Request 2 → 200 OK
Request 3 → 200 OK
Request 4 → 429 Too Many Requests
```

---

# 📦 Project Structure

```text
distributed-rate-limiter
│
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com.example.ratelimiter
│   │   │       │
│   │   │       ├── annotation
│   │   │       ├── aspect
│   │   │       ├── config
│   │   │       ├── exception
│   │   │       ├── model
│   │   │       ├── resolver
│   │   │       └── service
│   │   │
│   │   └── resources
│   │       └── scripts
│   │           └── rate-limit.lua
│   │
│   └── test
│
├── pom.xml
└── README.md
```

---

# 🛠️ Technology Stack

| Technology        | Purpose                         |
| ----------------- | ------------------------------- |
| Java 21           | Application development         |
| Spring Boot       | Framework                       |
| Spring AOP        | Request interception            |
| Spring Data Redis | Redis integration               |
| Redis             | Distributed state               |
| Lua               | Atomic Redis operations         |
| Maven             | Build and dependency management |
| JUnit             | Testing                         |
| Mockito           | Mock-based testing              |
| Docker            | Local Redis environment         |

---

# 🐳 Run Redis Locally

Start Redis using Docker:

```bash
docker run -d \
  --name redis \
  -p 6379:6379 \
  redis
```

Verify Redis:

```bash
docker exec -it redis redis-cli ping
```

Expected:

```text
PONG
```

---

# 🔍 Example Scenario

Suppose an authentication API has the following rule:

```text
10 requests / 2 minutes / API key
```

Client:

```text
X-API-KEY: abc123
```

Redis key:

```text
rate_limit:customer-auth:abc123
```

Requests:

```text
Request 1  → Allowed
Request 2  → Allowed
Request 3  → Allowed
...
Request 10 → Allowed
Request 11 → 429 Too Many Requests
```

After the configured window expires:

```text
Request 12 → Allowed
```

---

# 📈 Scalability

The application can run multiple instances behind a load balancer:

```text
                 Load Balancer
                      |
          ┌───────────┼───────────┐
          ↓           ↓           ↓
       App-1       App-2       App-3
          \           |           /
           \          |          /
                  Redis
```

Because Redis maintains the shared state, rate limiting remains consistent regardless of which application instance receives the request.

---

# 🔮 Future Enhancements

The following features can be added in future versions:

* [ ] Sliding Window algorithm
* [ ] Token Bucket algorithm
* [ ] Leaky Bucket algorithm
* [ ] Dynamic configuration refresh
* [ ] Redis Cluster support
* [ ] Rate-limit response headers
* [ ] Distributed metrics
* [ ] Micrometer/Prometheus integration
* [ ] Resilience4j integration
* [ ] WebFlux support
* [ ] Custom key resolver SPI
* [ ] Per-user and per-IP rate limiting

---

# 📚 Design Concepts Demonstrated

This project demonstrates practical usage of:

* Spring AOP
* Custom annotations
* Strategy Pattern
* Registry Pattern
* Dependency Injection
* Redis
* Lua scripting
* Atomic operations
* Distributed systems
* Concurrency
* Thread safety
* Configuration-driven design
* Exception handling
* Clean architecture
* Reusable library design

---

# 🎯 Interview Discussion Points

This project can be used to discuss:

### Why Redis?

Redis provides centralized shared state that can be accessed by multiple application instances.

### Why Lua?

The rate-limit check and counter update need to be atomic. Lua allows Redis to execute the complete operation as one atomic server-side operation.

### Why not ConcurrentHashMap?

An in-memory map only maintains state inside one JVM. It does not provide a shared counter across multiple application instances.

### Why AOP?

AOP separates rate-limiting concerns from business logic.

Instead of writing rate-limit code inside every controller:

```java
if (!rateLimiter.allow(...)) {
    ...
}
```

we can simply use:

```java
@RateLimited(rule = "customer-auth")
```

### What happens if Redis fails?

The implementation follows a fail-closed approach so that rate limiting is not silently bypassed.

---

# 📄 License

This project is intended for learning, experimentation, and demonstration purposes.

---

# 👨‍💻 Author

**Ajay Sharma**

Java Backend Developer

Interested in:

* Java
* Spring Boot
* Microservices
* Redis
* AWS
* Distributed Systems
* System Design
