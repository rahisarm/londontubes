package com.ig.group.tfl.status.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TflLineDto {
    private String id;
    private String name;
    private String modeName;
    private List<TflLineStatusDto> lineStatuses;
}
