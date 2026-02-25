package com.ig.group.tfl.status.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TflApiClientTest {

    private TflApiClient tflApiClient;

    @BeforeEach
    void setUp() {
        ExchangeFunction exchangeFunction = clientRequest -> {
            String url = clientRequest.url().toString();
            String responseBody = "";

            if (url.contains("/Line/central/Status")) {
                responseBody = "[{\"id\":\"central\",\"name\":\"Central\",\"modeName\":\"tube\",\"lineStatuses\":[{\"statusSeverity\":10,\"statusSeverityDescription\":\"Good Service\"}]}]";
            } else if (url.contains("/Line/victoria/Status/")) {
                responseBody = "[{\"id\":\"victoria\",\"name\":\"Victoria\",\"modeName\":\"tube\",\"lineStatuses\":[{\"statusSeverity\":1,\"statusSeverityDescription\":\"Closed\"}]}]";
            } else if (url.contains("/Line/Mode/tube/Status")) {
                responseBody = "[{\"id\":\"central\",\"name\":\"Central\"}, {\"id\":\"bakerloo\",\"name\":\"Bakerloo\"}]";
            }

            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(responseBody)
                    .build());
        };

        WebClient webClient = WebClient.builder()
                .baseUrl("http://mock")
                .exchangeFunction(exchangeFunction)
                .build();

        // Testing without appId and appKey configuration logic path for simplicity
        tflApiClient = new TflApiClient(webClient, "", "");
    }

    @Test
    void getLineStatus_ReturnsFluxOfTflLineDto() {
        // Act & Assert
        StepVerifier.create(tflApiClient.getLineStatus("central"))
                .assertNext(tflLineDto -> {
                    assertEquals("central", tflLineDto.getId());
                    assertEquals("tube", tflLineDto.getModeName());
                    assertNotNull(tflLineDto.getLineStatuses());
                    assertEquals(1, tflLineDto.getLineStatuses().size());
                    assertEquals("Good Service", tflLineDto.getLineStatuses().get(0).getStatusSeverityDescription());
                })
                .verifyComplete();
    }

    @Test
    void getLineStatusWithDateRange_ReturnsFluxOfTflLineDto() {
        // Act & Assert
        StepVerifier.create(tflApiClient.getLineStatusWithDateRange("victoria", "2023-10-25", "2023-10-26"))
                .assertNext(tflLineDto -> {
                    assertEquals("victoria", tflLineDto.getId());
                    assertEquals(1, tflLineDto.getLineStatuses().get(0).getStatusSeverity());
                })
                .verifyComplete();
    }

    @Test
    void getAllTubeLineStatuses_ReturnsFluxOfTflLineDto() {
        // Act & Assert
        StepVerifier.create(tflApiClient.getAllTubeLineStatuses())
                .assertNext(dto -> assertEquals("central", dto.getId()))
                .assertNext(dto -> assertEquals("bakerloo", dto.getId()))
                .verifyComplete();
    }
}
