package com.zeenom.loan_tracker.transactions

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WeebClientConfig {
    @Bean
    fun webClient(): WebClient {
        return WebClient.create()
    }
}