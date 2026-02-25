# SRE: Service Level Objectives (SLOs) and Indicators (SLIs)

This document formalizes the reliability targets for the London Underground Tube Status service. 

## 1. Service Level Indicators (SLIs)

SLIs are the quantifiable metrics measuring the reliability characteristics relevant to the "User Experience" (in this case, other internal trading services). 

1. **Availability (Success Rate):** The proportion of requests (both gRPC and REST) that return a successful, non-server-error response code over the total number of valid requests. 
   - *Formula:* `(Total Requests - (5xx Errors)) / (Total Requests - 4xx User Errors) * 100`
   - *Justification:* If a trader requests "northern," and we return a `400 Bad Request` or `429 Too Many Requests`, the service operated exactly as designed. If we return `503 Circuit Breaker Open` or `500 Server Error`, our availability took a hit.

2. **Latency (P99 Response Time):** The time it takes for 99% of requests to be fulfilled (measured at the API Gateway or Load Balancer).
   - *Justification:* A trading floor algorithm expects near-instantaneous responses. Averages (mean) hide slow outliers. Measuring the P99 ensures even the slowest 1% of requests are bound by a rigid timeframe.

3. **Freshness (Cache Staleness):** The time elapsed since the Redis cache was last updated successfully *against* the TTL. 
   - *Justification:* In a high-throughput caching system, availability can read 100% (because Redis is fast and up), but if the upstream TfL API has been disconnected for 1 hour, traders are trading on dangerously stale information.

## 2. Service Level Objectives (SLOs)

These targets dictate when we halt feature work and begin immediate reliability remediation (Error Budgets). Time window: **Rolling 30-Day Window**.

1. **Availability:** `99.9% of all valid requests will complete successfully.`
   - *Why 99.9%?* This equates to roughly 43 minutes of allowed downtime per month. Aiming for 99.999% (Arbritrary 5 Nines, a known Red Flag in the spec) is impossible when we are heavily reliant on a 3rd party public API (TfL), which itself likely doesn't offer a 5-Nine SLA. We can use Circuit Breakers and Fallbacks, but eventually, if TfL goes down for 3 hours, we will fail our queries.

2. **Latency:** `99% of requests will complete in < 50ms.`
   - *Why 50ms?* Since we are using Spring WebFlux + Reactor interacting with a local Redis cache cluster, reading data requires no blocking I/O and no external network routing. If >1% of requests exceed 50ms, it indicates deep systemic flaws like GC Pauses, Redis CPU exhaustion, or thread-pool starvation.

3. **Freshness:** `Data returned to the client should never be older than 5 minutes, 99.9% of the time.`
   - *Why 5 minutes?* A TTL of 60 seconds is typical. If 4 retries (including jitter spans) continue to fail, we allow cache staleness to expand gracefully up to 5 minutes before we declare the data invalid/stale for algorithmic trading.

## 3. Alerting Strategy

Alerting should *never* trigger on single machine failures or localized spikes, otherwise it causes "Alert Fatigue" for the on-call SRE. Alerts should only fire when business SLIs are in danger of failing their SLOs.

### Methodology: Multi-Window Error Budget Burn Rate
We use Google SRE's *Multiple Burn Rate Alerts* methodology. 

An alert is sent when the service consumes error budgets too quickly (e.g. attempting to exhaust the 30-Day budget in 1 hour).

1. **PAGE (Critical - Wakes SRE Up):**
   - *Condition:* (14.4x Burn Rate) The 99.9% Availability SLO or 50ms Latency SLO is burning at a rate that would consume the entire monthly 0.1% error budget in **1 Hour**.
   - *Why?* This indicates a complete systematic outage. Example: Redis goes down entirely and the Fallback Circuit Breaker opens, resulting in 100% `503 Service Unavailable`.
   
2. **TICKET (High Severity - Handled during Business Hours):**
   - *Condition:* (6x Burn Rate) The SLO is burning at a rate that would consume the monthly budget in **3 Days**.
   - *Why?* The service works for most users, but ~5% of requests are failing or exceeding 50ms (perhaps one subset of nodes is misconfigured or JVM Garbage Collection is pausing frequently). It's not a total outage, but the SLO is currently failing.
   - *Freshness Ticket:* If the TfL API goes offline and we rely on stale cache data for > 5 minutes, raising a ticket prevents algorithmic disaster without waking someone up at 3 AM to fix a public 3rd party API they can't control.
