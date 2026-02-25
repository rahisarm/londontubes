package com.ig.group.tfl.status.controller;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;

@GrpcAdvice
@Slf4j
public class GrpcExceptionAdvice {

    @GrpcExceptionHandler(RequestNotPermitted.class)
    public Status handleRateLimiter(RequestNotPermitted e) {
        log.warn("gRPC Rate limit exceeded: {}", e.getMessage());
        return Status.RESOURCE_EXHAUSTED
                .withDescription("Too Many Requests. You have exceeded the API rate limit of 100 requests per minute.");
    }

    @GrpcExceptionHandler(CallNotPermittedException.class)
    public Status handleCircuitBreaker(CallNotPermittedException e) {
        log.error("gRPC Circuit Breaker is OPEN. Halting requests.");
        return Status.UNAVAILABLE
                .withDescription("Service Unavailable. The upstream TfL API is temporarily down.");
    }

    @GrpcExceptionHandler(Exception.class)
    public Status handleGenericException(Exception e) {
        log.error("gRPC Unhandled Exception: ", e);
        return Status.INTERNAL.withDescription(e.getMessage());
    }
}
