package com.ig.group.tfl.status.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TflLineStatusDto {
    private Integer statusSeverity;
    private String statusSeverityDescription;
    private String reason;
    private List<TflValidityPeriodDto> validityPeriods;
}
