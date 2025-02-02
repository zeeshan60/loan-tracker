package com.zeenom.loan_tracker.controllers

import com.zeenom.loan_tracker.services.AuthService
import com.zeenom.loan_tracker.services.FirebaseService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FriendsControllerTest(@LocalServerPort private val port: Int) {

    @Autowired
    private lateinit var authService: AuthService
    private val webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

    @MockitoBean
    private lateinit var firebaseService: FirebaseService

    @Test
    fun `friends endpoint returns friends`() {
        Mockito.doReturn("uid").`when`(firebaseService).verifyIdToken("verified-id-token")
        webTestClient.get()
            .uri("/api/v1/friends")
            .header("Authorization", "Bearer ${authService.generateJwt("verified-id-token")}")
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
            .header("Authorization", "Bearer ${authService.generateJwt("verified-id-token")}")
            .header("Origin", "http://any-origin.com")
            .header("Access-Control-Request-Method", "GET")
            .exchange()

        response.expectHeader().valueEquals("Access-Control-Allow-Origin", "*")
        response.expectHeader().valueEquals("Access-Control-Allow-Methods", "GET")
        response.expectHeader().exists("Access-Control-Max-Age")
    }
}

