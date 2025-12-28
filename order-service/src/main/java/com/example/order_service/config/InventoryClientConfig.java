package com.example.order_service.config;

import com.example.order_service.client.InventoryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class InventoryClientConfig {

    @Bean
    public WebClient inventoryWebClient() {
        String inventoryUrl = System.getenv("INVENTORY_URL");
        if (inventoryUrl == null || inventoryUrl.isEmpty()) {
            inventoryUrl = "http://localhost:8082"; // Fallback to localhost for development
        }
        return WebClient.builder()
                .baseUrl(inventoryUrl) // Inventory service URL from environment or localhost fallback
                .build();
    }

    @Bean
    public InventoryClient inventoryClient(WebClient inventoryWebClient) {
        HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory.builder()
                .exchangeAdapter(WebClientAdapter.create(inventoryWebClient))
                .build();
        return httpServiceProxyFactory.createClient(InventoryClient.class);
    }
}
