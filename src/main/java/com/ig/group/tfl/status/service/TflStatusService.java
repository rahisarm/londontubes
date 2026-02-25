package com.ig.group.tfl.status.service;

import com.ig.group.tfl.status.client.TflApiClient;
import com.ig.group.tfl.status.dto.TflLineDto;
import com.ig.group.tfl.status.dto.TflLineStatusDto;
import com.ig.group.tfl.status.dto.TflValidityPeriodDto;
import com.ig.group.tfl.status.grpc.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TflStatusService {

    private final TflApiClient tflApiClient;

    private static final String CACHE_NAME_LINE_STATUS = "lineStatus";
    private static final String CACHE_NAME_FUTURE_STATUS = "futureStatus";
    private static final String CACHE_NAME_UNPLANNED = "unplannedDisruptions";

    @Cacheable(value = CACHE_NAME_LINE_STATUS, key = "#lineId")
    public Mono<LineStatusResponse> getLineStatus(String lineId) {
        return tflApiClient.getLineStatus(lineId)
                .collectList()
                .map(this::mapTflResponseToLineInfo)
                .map(lineInfo -> LineStatusResponse.newBuilder().setLine(lineInfo).build());
    }

    @Cacheable(value = CACHE_NAME_FUTURE_STATUS, key = "{#lineId, #startDate, #endDate}")
    public Mono<FutureLineStatusResponse> getFutureLineStatus(String lineId, String startDate, String endDate) {
        return tflApiClient.getLineStatusWithDateRange(lineId, startDate, endDate)
                .collectList()
                .map(this::mapTflResponseToLineInfo)
                .map(lineInfo -> FutureLineStatusResponse.newBuilder().setLine(lineInfo).build());
    }

    @Cacheable(value = CACHE_NAME_UNPLANNED)
    public Mono<UnplannedDisruptionsResponse> getUnplannedDisruptions() {
        return tflApiClient.getAllTubeLineStatuses()
                .filter(this::hasUnplannedDisruption)
                .map(this::mapSingleLineDtoToLineInfo)
                .collectList()
                .map(lines -> UnplannedDisruptionsResponse.newBuilder().addAllAffectedLines(lines).build());
    }

    /**
     * Requirement 3 logic: Exclude planned engineering works.
     * Often 'statusSeverity' < 10 but != 0 (special/planned closures).
     * In TfL API:
     * 10 = Good Service
     * 11 = Planned Closure
     * 12 = Part Closure
     * 13 = Planned Part Closure
     * 14 = Part Suspended (can be unplanned)
     * 6 = Severe Delays
     * 9 = Minor Delays
     * We look for statuses that aren't "Good Service" (10) and aren't strictly
     * "Planned"
     */
    private boolean hasUnplannedDisruption(TflLineDto dto) {
        if (dto.getLineStatuses() == null || dto.getLineStatuses().isEmpty()) {
            return false;
        }

        return dto.getLineStatuses().stream().anyMatch(status -> {
            int severity = status.getStatusSeverity();
            // Assuming 10 is Good Service, and 11, 12, 13 are variations of planned
            // closures.
            // SRE implementation note: Real TfL status parsing can be complex,
            // we filter out Good Service and typical planned strings.
            boolean isNotGoodService = severity != 10;
            boolean isNotPlanned = severity != 11 && severity != 13;

            String desc = status.getStatusSeverityDescription();
            if (desc != null && desc.toLowerCase().contains("planned")) {
                isNotPlanned = false;
            }

            return isNotGoodService && isNotPlanned;
        });
    }

    private LineInfo mapTflResponseToLineInfo(List<TflLineDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return LineInfo.newBuilder().setId("unknown").setName("Unknown").build();
        }
        // Often one line is returned, take the first
        return mapSingleLineDtoToLineInfo(dtos.get(0));
    }

    private LineInfo mapSingleLineDtoToLineInfo(TflLineDto dto) {
        LineInfo.Builder builder = LineInfo.newBuilder()
                .setId(dto.getId() != null ? dto.getId() : "")
                .setName(dto.getName() != null ? dto.getName() : "");

        if (dto.getLineStatuses() != null) {
            for (TflLineStatusDto statusDto : dto.getLineStatuses()) {
                StatusInfo.Builder statusBuilder = StatusInfo.newBuilder()
                        .setStatusSeverity(statusDto.getStatusSeverity() != null ? statusDto.getStatusSeverity() : 0)
                        .setStatusSeverityDescription(statusDto.getStatusSeverityDescription() != null
                                ? statusDto.getStatusSeverityDescription()
                                : "");

                if (statusDto.getReason() != null) {
                    statusBuilder.setReason(statusDto.getReason());
                }

                if (statusDto.getValidityPeriods() != null) {
                    for (TflValidityPeriodDto vpDto : statusDto.getValidityPeriods()) {
                        ValidityPeriod.Builder vpBuilder = ValidityPeriod.newBuilder()
                                .setFromDate(vpDto.getFromDate() != null ? vpDto.getFromDate() : "")
                                .setToDate(vpDto.getToDate() != null ? vpDto.getToDate() : "")
                                .setIsNow(vpDto.getIsNow() != null ? vpDto.getIsNow() : false);
                        statusBuilder.addValidityPeriods(vpBuilder.build());
                    }
                }
                builder.addStatuses(statusBuilder.build());
            }
        }
        return builder.build();
    }
}
