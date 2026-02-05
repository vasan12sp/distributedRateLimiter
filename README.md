

# 🚦 Distributed Rate Limiter as a Service

A **Distributed Rate Limiting Service** built using **Spring Boot and Redis** that can be used as a central service to protect APIs from abuse, overload, and malicious traffic. The system implements a **Redis-based Sliding Window algorithm**, is designed to work in distributed environments, and follows a **fail-open strategy** for high availability.

---

## 🎯 Problem Statement

Modern applications expose multiple APIs such as:

* Login API
* Payment API
* Search API
* Public developer APIs

These APIs are vulnerable to:

* Brute-force attacks
* Excessive traffic
* Automated bot requests
* Distributed Denial of Service (DoS) attempts

A naive rate-limiting approach (like keeping counters in application memory) fails when multiple servers are involved, because each server maintains its own count.

---

## ✅ What is a Rate Limiter?

A **rate limiter** controls how many requests a user can make within a given time window.

Examples of rules:

* 100 requests per minute per user
* 10 login attempts per IP per minute
* 1000 requests per API key per hour

If the limit is exceeded, the system returns:

* HTTP **429 Too Many Requests**

---

## ☁️ What is “Rate Limiter as a Service”?

Instead of every application implementing its own rate-limiting logic, this project provides a **centralized rate-limiting service** that other applications can call before processing requests.

Applications simply send a request to this service asking:

> “Is this request allowed or should it be blocked?”

---

## 🏗️ High-Level System Flow

User → Application → Rate Limiter Service → Redis → Allow or Block Decision

Steps:

1. User sends a request to an application
2. The application calls the Rate Limiter Service
3. The Rate Limiter checks Redis for recent requests
4. If under limit → request is allowed
5. If over limit → request is rejected with 429

---

## 🌍 Why is this Distributed?

In real-world systems, applications run on multiple servers behind a load balancer.

If each server keeps its own request counter, the rate limit can be bypassed.

To solve this, all servers share a **centralized Redis store**, making the rate limiter truly distributed.

---

## ⏱️ Sliding Window Algorithm

This project uses a **Sliding Window Rate Limiting Algorithm** implemented with Redis Sorted Sets.

For each request:

1. Remove old timestamps outside the time window
2. Count remaining requests
3. If count is below limit → allow and store new timestamp
4. Otherwise → block the request

Advantages:

* More accurate than fixed windows
* Avoids boundary issues
* Smooth request handling

---

## 🛑 What happens if Redis goes down?

The system follows a **Fail-Open Strategy**:

* If Redis is unavailable, requests are allowed instead of blocked.

Reason:

* It is better to risk some abuse than to block legitimate users due to a temporary failure.

---

## 🎯 Who can use this?

This system is useful for:

### Startups and SaaS companies

Who want API protection without building their own rate limiter.

### Public API providers

Who need per-API-key limits and usage tracking.

### Enterprises

To protect internal microservices and prevent cascading failures.

### Indie developers

Who need plug-and-play API protection.

---

## 🔌 Integration Options

### HTTP API
```
Headers: POST /api/check
Content-Type: application/json
X-API-Key: abc123                // api key generated from website
```

```
POST /check-limit
{
  "identifier" : "user123",     // could be user ID, IP address, API key
  "endpoint": "/login",
  "method" : "GET"              // or  POST, PUT, DELETE
}
```

---

## 📊 Monitoring & Analytics

The system can provide:

* Total allowed vs blocked requests
* Most abused endpoints
* Top offending IP addresses
* Real-time traffic graphs

---

## ⭐ Key Features (USP)

* Accurate Sliding Window algorithm
* Works in distributed systems
* Redis-based for high performance
* Fail-open for reliability
* Easy integration via HTTP or SDK

---

## 🛠️ Tech Stack

* Backend: Spring Boot, Java 17
* Cache: Redis
* Database: PostgreSQL
* Monitoring: Prometheus & Grafana

---

## 🚀 How to Run

### Prerequisites

* Java 17
* Redis
* PostgreSQL
* Maven

### Run the project

```
git clone https://github.com/vasan12sp/rate-limiter.git
cd rate-limiter
mvn clean install
mvn spring-boot:run
```

---

## 📡 Example API Response

### Allowed

```
{
  "allowed": true,
  "remaining": 95
}
```

### Blocked

```
{
  "allowed": false,
  "error": "Rate limit exceeded",
  "retryAfter": 45
}
```

---

## 🔮 Future Enhancements

* Kubernetes deployment
* Multi-region Redis replication
* Token Bucket algorithm support
* AI-based anomaly detection

---

## 📄 License

MIT License

---

