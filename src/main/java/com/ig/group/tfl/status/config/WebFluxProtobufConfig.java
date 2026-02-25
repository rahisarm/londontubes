package com.ig.group.tfl.status.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.protobuf.ProtobufDecoder;
import org.springframework.http.codec.protobuf.ProtobufEncoder;
import org.springframework.http.codec.protobuf.ProtobufJsonDecoder;
import org.springframework.http.codec.protobuf.ProtobufJsonEncoder;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
@EnableWebFlux
public class WebFluxProtobufConfig implements WebFluxConfigurer {

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().protobufDecoder(new ProtobufDecoder());
        configurer.defaultCodecs().protobufEncoder(new ProtobufEncoder());

        // This is specifically required for WebFlux to serialize Protobuf natively to
        // application/json
        configurer.customCodecs().register(new ProtobufJsonEncoder());
        configurer.customCodecs().register(new ProtobufJsonDecoder());
    }
}
