package com.ig.group.tfl.status.client;

import com.ig.group.tfl.status.dto.TflLineDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class TflApiClient {

    private final WebClient webClient;
    private final String appId;
    private final String appKey;

    public TflApiClient(WebClient webClient,
            @Value("${tfl.api.app-id}") String appId,
            @Value("${tfl.api.app-key}") String appKey) {
        this.webClient = webClient;
        this.appId = appId;
        this.appKey = appKey;
    }

    private WebClient.RequestHeadersUriSpec<?> authenticatedRequest(WebClient webClient) {
        if (appId != null && !appId.isBlank() && appKey != null && !appKey.isBlank()) {
            return (WebClient.RequestHeadersUriSpec<?>) webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("app_id", appId)
                            .queryParam("app_key", appKey)
                            .build());
        }
        return webClient.get();
    }

    /**
     * Requirement 1: Get Status of a Given Tube Line
     */
    public Flux<TflLineDto> getLineStatus(String lineId) {
        String uri = String.format("/Line/%s/Status", lineId);
        log.info("Fetching Line Status from TfL API: {}", uri);

        return authenticatedRequest(webClient)
                .uri(uriBuilder -> uriBuilder.path(uri).build())
                .retrieve()
                .bodyToFlux(TflLineDto.class)
                .doOnError(e -> log.error("Error fetching line status for {}: {}", lineId, e.getMessage()));
    }

    /**
     * Requirement 2: Future Status with Date Range
     */
    public Flux<TflLineDto> getLineStatusWithDateRange(String lineId, String startDate, String endDate) {
        String uri = String.format("/Line/%s/Status/%s/to/%s", lineId, startDate, endDate);
        log.info("Fetching Future Line Status from TfL API: {}", uri);

        return authenticatedRequest(webClient)
                .uri(uriBuilder -> uriBuilder.path(uri).build())
                .retrieve()
                .bodyToFlux(TflLineDto.class)
                .doOnError(e -> log.error("Error fetching future status for {} from {} to {}: {}", lineId, startDate,
                        endDate, e.getMessage()));
    }

    /**
     * Requirement 3: All disruptions (we will filter unplanned ones in the service
     * layer)
     * For tube only.
     */
    public Flux<TflLineDto> getAllTubeLineStatuses() {
        String uri = "/Line/Mode/tube/Status";
        log.info("Fetching All Tube Line Statuses from TfL API: {}", uri);

        return authenticatedRequest(webClient)
                .uri(uriBuilder -> uriBuilder.path(uri).build())
                .retrieve()
                .bodyToFlux(TflLineDto.class)
                .doOnError(e -> log.error("Error fetching all tube line statuses: {}", e.getMessage()));
    }

}

@Configuration
class WebClientConfig {
    @Bean
    public WebClient tflWebClient(@Value("${tfl.api.base-url}") String baseUrl,
            io.micrometer.observation.ObservationRegistry observationRegistry) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .observationRegistry(observationRegistry)
                .build();
    }
}
