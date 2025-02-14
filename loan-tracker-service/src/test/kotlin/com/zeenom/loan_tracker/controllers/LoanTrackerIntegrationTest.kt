package com.zeenom.loan_tracker.controllers

import com.zeenom.loan_tracker.security.JWTTokenResponse
import com.zeenom.loan_tracker.security.LoginRequest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Disabled
class LoanTrackerIntegrationTest(@LocalServerPort private val port: Int) {

    private val webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

    private val token =
        "eyJhbGciOiJSUzI1NiIsImtpZCI6ImE0MzRmMzFkN2Y3NWRiN2QyZjQ0YjgxZDg1MjMwZWQxN2ZlNTk3MzciLCJ0eXAiOiJKV1QifQ.eyJuYW1lIjoiWmVlc2hhbiBUdWZhaWwiLCJwaWN0dXJlIjoiaHR0cHM6Ly9saDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2EvQUNnOG9jS3hlSEhxNENsNU9RZjdDSENISHA4Ym1ObEswbmFGbzJHa282UTJPS0xDRjRkNjVHbHc9czk2LWMiLCJpc3MiOiJodHRwczovL3NlY3VyZXRva2VuLmdvb2dsZS5jb20vbG9hbi10cmFja2VyLTliMjVkIiwiYXVkIjoibG9hbi10cmFja2VyLTliMjVkIiwiYXV0aF90aW1lIjoxNzM4NjE5NjY4LCJ1c2VyX2lkIjoiQ01XTDB0YXBaR1NET0kzVGJ6UUVNOHZibFRsMiIsInN1YiI6IkNNV0wwdGFwWkdTRE9JM1RielFFTTh2YmxUbDIiLCJpYXQiOjE3Mzg2MTk2NjgsImV4cCI6MTczODYyMzI2OCwiZW1haWwiOiJ6ZWVzaGFudHVmYWlsODZAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImZpcmViYXNlIjp7ImlkZW50aXRpZXMiOnsiZ29vZ2xlLmNvbSI6WyIxMTE4MzA4MjI1MDU4Mjc5Mzk3OTQiXSwiZW1haWwiOlsiemVlc2hhbnR1ZmFpbDg2QGdtYWlsLmNvbSJdfSwic2lnbl9pbl9wcm92aWRlciI6Imdvb2dsZS5jb20ifX0.uOuFCiH2tRHaBvL32GsRM9e17Ocr47QyijhmLmleWgiZ8y2xtI68g2IlOwcrx895ZWaP_6H8P13VnnHRDSXUcTYrH08TrH2LeSjzfRtDR9ZhQjgt8qpYRMQQaeJSXrVWHQUhpHHZwOUlmGhoSoJIaeIX0z2yBTs4pcoPdnsfL_57FiQGZE5ypwxOMlxTtyQjla-EeJ-1hR-AClHWi_2fV32PNwmkyR8wUUofrAmWG0pnPpfXI5LWoX6oOHx_uCrN-aoSA7r8sa_YcTRB_fypGkR8sYN5QLUy9S1vaUtAQnYlcQgfJyU_HZuwdrWqDCluL8K4OvcpGk2q_uZ-3TlpPA"

    @Test
    fun `given verified id token generates jwt token with expiry successfully`() {

        // When
        val response = webTestClient.post()
            .uri("/login")
            .header("Content-Type", "application/json")
            .bodyValue(LoginRequest("Bearer $token"))
            .exchange()

        // Then
        response.expectStatus().isOk
            .expectBody()
            .jsonPath("$.token").value { it: String? -> Assertions.assertThat(it).isNotEmpty() }


    }

    @Test
    fun `test getfriends with token`() {

        // When
        webTestClient.post()
            .uri("/login")
            .header("Content-Type", "application/json")
            .bodyValue(LoginRequest("Bearer $token"))
            .exchange().expectStatus().isOk
            .expectBody(JWTTokenResponse::class.java)
            .returnResult().responseBody?.let {
                webTestClient.get()
                    .uri("/api/v1/friends")
                    .header("Authorization", "Bearer ${it.token}")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$.data.friends").exists()
            }
    }
}