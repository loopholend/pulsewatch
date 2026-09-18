package com.pulsewatch.backend.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@EnableScheduling
@EnableAsync
public class AppConfig {

    /** 5 MB buffer — large enough for GitHub/Cloudflare HTML responses. */
    private static final int MAX_IN_MEMORY_SIZE = 5 * 1024 * 1024;

    /**
     * 32 KB response header buffer.
     * Netty default is 8192 bytes — too small for many modern sites
     * (Gemini, large CDNs, sites with big CSP/Set-Cookie/Link headers).
     * Symptom when too small: "HTTP header is larger than 8192 bytes."
     */
    private static final int MAX_HEADER_SIZE = 32 * 1024;

    /**
     * Hard network-level response timeout (Netty layer).
     * This fires at the TCP/TLS layer, independent of Reactor's reactive scheduler.
     * Prevents threads from hanging indefinitely on sites like LinkedIn that stall
     * during TLS handshake or redirect chains before Reactor can signal a timeout.
     * The per-monitor Reactor-level timeout in MonitorExecutionService provides
     * a finer-grained timeout respecting each monitor's configured timeoutMs.
     */
    private static final Duration NETWORK_RESPONSE_TIMEOUT = Duration.ofSeconds(30);

    @Bean
    public WebClient.Builder webClientBuilder() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(config -> config.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE))
                .build();

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(NETWORK_RESPONSE_TIMEOUT)
                .followRedirect(true)
                .httpResponseDecoder(spec -> spec.maxHeaderSize(MAX_HEADER_SIZE));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies);
    }
}
