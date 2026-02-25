package com.ig.group.tfl.status.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TflValidityPeriodDto {
    private String fromDate;
    private String toDate;
    private Boolean isNow;
}
