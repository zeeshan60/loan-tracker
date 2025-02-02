package com.zeenom.loan_tracker.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FriendsControllerTest(@LocalServerPort private val port: Int) {

    private val webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

    @Test
    fun `friends endpoint returns friends`() {
        webTestClient.get()
            .uri("/api/v1/friends")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data.friends").isNotEmpty
            .jsonPath("$.next").value { value: String? ->
                assertThat(value).isNull()
            }
    }

    @Test
    fun `test cors configuration`() {
        val response = webTestClient.options()
            .uri("/api/v1/friends")
            .header("Origin", "http://any-origin.com")
            .header("Access-Control-Request-Method", "GET")
            .exchange()

        response.expectHeader().valueEquals("Access-Control-Allow-Origin", "*")
        response.expectHeader().valueEquals("Access-Control-Allow-Methods", "GET")
        response.expectHeader().exists("Access-Control-Max-Age")
    }
}

