# London Underground Tube Status Service

This is a resilient, high-throughput microservice that provides real-time information on the London Underground status using data from the Transport for London (TfL) API. 

It is designed following robust Site Reliability Engineering (SRE) principles to tolerate upstream failures and gracefully degrade when necessary.

## Architecture

This service was designed specifically to support up to **1 million Requests Per Second (RPS)**. As a result, standard blocking web architecture (Spring MVC + Tomcat) was replaced with **Spring WebFlux (Project Reactor + Netty)** and **gRPC** for maximum internal throughput.

### Dual Protocol Approach
- **gRPC (Port 9090)**: Designed for ultra-high throughput machine-to-machine internal communication (e.g., trading platforms). Protocol Buffers (`.proto`) serialize much faster and use less bandwidth than standard JSON.
- **REST via WebFlux (Port 8080)**: Exposed for standard developers, non-technical users, and browser-based interfaces (Swagger UI) using JSON over non-blocking HTTP (`Accept-Version: v1` Headers).

### Resilience Patterns Implemented
1. **Circuit Breaker**: Halts requests if 5 consecutive calls to the TfL API fail, opening for 30 seconds before testing recovery (`Half-Open`). Returns `503 Service Unavailable` or `UNAVAILABLE`.
2. **Retry Logic**: Exponential Backoff + Jitter for 5xx server errors and network Timeouts. Never retries user-caused 4xx errors.
3. **Rate Limiting**: Protects the local resources and API quota from abuse by throttling over 100 requests per minute per client IP. Returns `429 Too Many Requests` with a `Retry-After: 60` header.
4. **Caching**: Spring Cache with Reactive Redis. *This is critical for achieving 1M RPS without immediately being rate-limited by TfL.* A TTL of 60 seconds is used.

### Trade-offs
1. **Data Freshness vs Throughput / Resilience**: By adding a caching layer with a 60s TTL, we sacrifice real-time (to the millisecond) data precision for the ability to handle massive throughput while preventing TfL Rate Limits. A tube delay typically does not materially change in < 60 seconds.
2. **Complexity for Performance**: Moving from MVC to WebFlux / Reactor adds complexity to testing and debugging. Standard `try/catch` is replaced with `doOnError` operators, but it mathematically enables 1M RPS which MVC could never achieve on the same hardware.

## Running Locally

**Prerequisites:** 
- Java 17
- Maven
- Redis (Optional, defaults to localhost:6379, testcontainers used during build)

```bash
# 1. Start Redis (Requirement for caching layer to run locally)
docker run -d -p 6379:6379 redis

# 2. Build the application (compiles gRPC and runs tests)
./mvnw clean install

# 3. Start the application
./mvnw spring-boot:run
```

## Testing the API

*(Required: Ensure the application is running)*

### For Non-Technical Users (Interactive Browser)
Simply navigate to your local interactive documentation:
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### Using Built-in cURL (REST Version)
*Note: Header versioning must be passed*

**1. Requirement 1: Single Line Status**
```bash
curl -H "Accept-Version: v1" http://localhost:8080/api/line/central/status | python3 -m json.tool
```

**2. Requirement 2: Future Date Range**
```bash
curl -H "Accept-Version: v1" http://localhost:8080/api/line/central/status/2026-03-20/to/2026-03-22 | python3 -m json.tool
```

**3. Requirement 3: Unplanned Disruptions Only**
```bash
curl -H "Accept-Version: v1" http://localhost:8080/api/line/disruptions/unplanned | python3 -m json.tool
```

### Using gRPCurl (High-Throughput Protocol)
If you have `grpcurl` installed:
```bash
# Get Line Status
grpcurl -plaintext -d '{"line_id": "northern"}' localhost:9090 com.ig.group.tfl.status.TubeStatusService/GetLineStatus

# Get Unplanned Disruptions
grpcurl -plaintext localhost:9090 com.ig.group.tfl.status.TubeStatusService/GetUnplannedDisruptions
```

## Scaling to 1,000,000 RPS
Should we need to deploy this to handle genuine 1M RPS traffic:
1. Increase the Redis instance size/cluster configuration to handle hundreds of thousands of concurrent reads.
2. Introduce a CDN (Cloudflare/Cloudfront) in front of the API to absorb Edge requests.
3. Switch from `on-demand` TfL lookups to an isolated `polling` microservice that populates the Redis cache directly. The primary cluster would then *only* read from Redis and never hold open HTTP connections to TfL.
