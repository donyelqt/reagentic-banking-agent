package dev.reagentic.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class BackendClients {

    @Bean
    public RestClient accountClient(@Value("${ACCOUNT_SERVICE_URL:http://localhost:8082}") String url) {
        return RestClient.builder().baseUrl(url).build();
    }

    @Bean
    public RestClient ledgerClient(@Value("${LEDGER_SERVICE_URL:http://localhost:8084}") String url) {
        return RestClient.builder().baseUrl(url).build();
    }

    @Bean
    public RestClient paymentClient(@Value("${PAYMENT_SERVICE_URL:http://localhost:8083}") String url) {
        return RestClient.builder().baseUrl(url).build();
    }
}
