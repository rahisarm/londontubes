package com.ig.group.tfl.status;

import org.springframework.cache.annotation.EnableCaching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;

@SpringBootApplication
@org.springframework.cache.annotation.EnableCaching
public class TflStatusServiceApplication {

	public static void main(String[] args) {
		reactor.core.publisher.Hooks.enableAutomaticContextPropagation();
		SpringApplication.run(TflStatusServiceApplication.class, args);
	}

	@Bean
	OtlpHttpSpanExporter otlpHttpSpanExporter(@Value("${management.otlp.tracing.endpoint}") String url) {
		return OtlpHttpSpanExporter.builder()
				.setEndpoint(url)
				.build();
	}

}
