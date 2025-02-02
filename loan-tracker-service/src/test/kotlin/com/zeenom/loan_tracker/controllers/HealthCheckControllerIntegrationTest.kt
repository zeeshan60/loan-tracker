package com.zeenom.loan_tracker.controllers

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthCheckControllerIntegrationTest(@LocalServerPort private val port: Int) {

    private val webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

    @Test
    fun `health check endpoint returns alive message`() {
        webTestClient.get()
            .uri("/health")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.message").value { it: String? -> Assertions.assertThat(it).isEqualTo("I'm alive!") }
    }

    @Test
    fun `test cors configuration`() {
        val response = webTestClient.options()
            .uri("/health")
            .header("Origin", "http://any-origin.com")
            .header("Access-Control-Request-Method", "GET")
            .exchange()

        response.expectHeader().valueEquals("Access-Control-Allow-Origin", "*")
        response.expectHeader().valueEquals("Access-Control-Allow-Methods", "GET")
        response.expectHeader().exists("Access-Control-Max-Age")
    }
}