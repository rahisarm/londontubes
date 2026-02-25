package com.ig.group.tfl.status.controller;

import com.ig.group.tfl.status.grpc.*;
import com.ig.group.tfl.status.service.TflStatusService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import reactor.core.publisher.Mono;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class TubeStatusGrpcService
        extends com.ig.group.tfl.status.grpc.TubeStatusServiceGrpc.TubeStatusServiceImplBase {

    private final TflStatusService statusService;

    // Requirement 1
    @Override
    @RateLimiter(name = "clientIpLimiter")
    public void getLineStatus(LineStatusRequest request, StreamObserver<LineStatusResponse> responseObserver) {
        log.info("gRPC: Received request for Line Status: {}", request.getLineId());
        statusService.getLineStatus(request.getLineId())
                .subscribe(
                        response -> {
                            responseObserver.onNext(response);
                            responseObserver.onCompleted();
                        },
                        error -> responseObserver.onError(
                                io.grpc.Status.INTERNAL.withDescription(error.getMessage()).asRuntimeException()));
    }

    // Requirement 2
    @Override
    @RateLimiter(name = "clientIpLimiter")
    public void getFutureLineStatus(FutureLineStatusRequest request,
            StreamObserver<FutureLineStatusResponse> responseObserver) {
        log.info("gRPC: Received request for Future Line Status: {}", request.getLineId());
        Mono<FutureLineStatusResponse> responseMono;

        if (request.hasDateRange()) {
            responseMono = statusService.getFutureLineStatus(request.getLineId(), request.getDateRange().getStartDate(),
                    request.getDateRange().getEndDate());
        } else {
            // "If no date range is provided, return current status" - Requirement 2
            log.info("No date range provided, fetching current status.");
            responseMono = statusService.getLineStatus(request.getLineId())
                    .map(statusRes -> FutureLineStatusResponse.newBuilder().setLine(statusRes.getLine()).build());
        }

        responseMono.subscribe(
                response -> {
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                },
                error -> responseObserver
                        .onError(io.grpc.Status.INTERNAL.withDescription(error.getMessage()).asRuntimeException()));
    }

    // Requirement 3
    @Override
    @RateLimiter(name = "clientIpLimiter")
    public void getUnplannedDisruptions(EmptyRequest request,
            StreamObserver<UnplannedDisruptionsResponse> responseObserver) {
        log.info("gRPC: Received request for all Unplanned Disruptions");
        statusService.getUnplannedDisruptions()
                .subscribe(
                        response -> {
                            responseObserver.onNext(response);
                            responseObserver.onCompleted();
                        },
                        error -> responseObserver.onError(
                                io.grpc.Status.INTERNAL.withDescription(error.getMessage()).asRuntimeException()));
    }
}
