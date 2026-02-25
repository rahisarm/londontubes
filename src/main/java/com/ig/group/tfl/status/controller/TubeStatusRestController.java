package com.ig.group.tfl.status.controller;

import com.ig.group.tfl.status.grpc.FutureLineStatusResponse;
import com.ig.group.tfl.status.grpc.LineStatusResponse;
import com.ig.group.tfl.status.grpc.UnplannedDisruptionsResponse;
import com.ig.group.tfl.status.service.TflStatusService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/line")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tube Status API", description = "Endpoints for checking TfL Tube Line Statuses (Non-technical / Browser accessible)")
public class TubeStatusRestController {

    private final TflStatusService statusService;

    @Operation(summary = "Get Status of a Given Tube Line", description = "Requirement 1: Query the current status for a specific line (e.g., 'central'). Returns disruption details if applicable.")
    @GetMapping(value = "/{id}/status", headers = "Accept-Version=v1")
    @RateLimiter(name = "clientIpLimiter")
    public Mono<LineStatusResponse> getLineStatus(@PathVariable("id") String lineId,
            @RequestHeader(value = HttpHeaders.ACCEPT) String accept) {
        log.info("REST: Received request for Line Status: {}", lineId);
        return statusService.getLineStatus(lineId);
    }

    @Operation(summary = "Get Future Status with Date Range", description = "Requirement 2: Query future status spanning a specific start and end date. Formats should be YYYY-MM-DD.")
    @GetMapping(value = "/{id}/status/{startDate}/to/{endDate}", headers = "Accept-Version=v1")
    @RateLimiter(name = "clientIpLimiter")
    public Mono<FutureLineStatusResponse> getFutureStatus(@PathVariable("id") String lineId,
            @PathVariable("startDate") String startDate,
            @PathVariable("endDate") String endDate) {
        log.info("REST: Received request for Future Line Status: {} from {} to {}", lineId, startDate, endDate);
        return statusService.getFutureLineStatus(lineId, startDate, endDate);
    }

    @Operation(summary = "Get All Unplanned Disruptions", description = "Requirement 3: Queries all tube lines and filters out strictly planned disruptions, displaying only those experiencing unforeseen issues.")
    @GetMapping(value = "/disruptions/unplanned", headers = "Accept-Version=v1")
    @RateLimiter(name = "clientIpLimiter")
    public Mono<UnplannedDisruptionsResponse> getUnplannedDisruptions() {
        log.info("REST: Received request for Unplanned Disruptions");
        return statusService.getUnplannedDisruptions();
    }
}
