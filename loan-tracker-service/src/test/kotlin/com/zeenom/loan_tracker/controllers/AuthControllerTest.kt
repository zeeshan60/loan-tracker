package com.zeenom.loan_tracker.controllers

import com.zeenom.loan_tracker.services.FirebaseService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerTest(@LocalServerPort private val port: Int) {

    private val webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

    @MockitoBean
    private lateinit var firebaseService: FirebaseService

    @Test
    fun `given verified id token generates jwt token with expiry successfully`() {
        // Given
        val idToken = "verified-id-token"
        Mockito.`when`(firebaseService.verifyIdToken(idToken)).thenReturn("jwt-token")

        // When
        val response = webTestClient.post()
            .uri("/login")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $idToken")
            .exchange()

        // Then
        response.expectStatus().isOk
            .expectBody()
            .jsonPath("$.token").value { it: String? -> Assertions.assertThat(it).isNotEmpty() }
    }
}

