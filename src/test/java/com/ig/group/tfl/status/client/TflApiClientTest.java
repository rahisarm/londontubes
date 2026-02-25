package com.ig.group.tfl.status.client;

import com.ig.group.tfl.status.dto.TflLineDto;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TflApiClientTest {

    private MockWebServer mockWebServer;
    private TflApiClient tflApiClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        // Testing without appId and appKey configuration logic path for simplicity
        tflApiClient = new TflApiClient(webClient, "", "");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void getLineStatus_ReturnsFluxOfTflLineDto() {
        // Arrange
        String mockResponseJson = "[{\"id\":\"central\",\"name\":\"Central\",\"modeName\":\"tube\",\"lineStatuses\":[{\"statusSeverity\":10,\"statusSeverityDescription\":\"Good Service\"}]}]";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(mockResponseJson));

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
        // Arrange
        String mockResponseJson = "[{\"id\":\"victoria\",\"name\":\"Victoria\",\"modeName\":\"tube\",\"lineStatuses\":[{\"statusSeverity\":1,\"statusSeverityDescription\":\"Closed\"}]}]";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(mockResponseJson));

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
        // Arrange
        String mockResponseJson = "[{\"id\":\"central\",\"name\":\"Central\"}, {\"id\":\"bakerloo\",\"name\":\"Bakerloo\"}]";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(mockResponseJson));

        // Act & Assert
        StepVerifier.create(tflApiClient.getAllTubeLineStatuses())
                .assertNext(dto -> assertEquals("central", dto.getId()))
                .assertNext(dto -> assertEquals("bakerloo", dto.getId()))
                .verifyComplete();
    }
}
