package com.zeenom.loan_tracker.integration

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.zeenom.loan_tracker.common.Paginated
import com.zeenom.loan_tracker.firebase.FirebaseService
import com.zeenom.loan_tracker.friends.FriendRequest
import com.zeenom.loan_tracker.friends.FriendResponse
import com.zeenom.loan_tracker.friends.FriendsResponse
import com.zeenom.loan_tracker.friends.TestPostgresConfig
import com.zeenom.loan_tracker.security.JWTTokenResponse
import com.zeenom.loan_tracker.security.LoginRequest
import com.zeenom.loan_tracker.users.UserDto
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.mockito.Mockito.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BaseIntegration : TestPostgresConfig() {

    @Autowired
    @MockitoSpyBean
    private lateinit var firebaseService: FirebaseService

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var webTestClient: WebTestClient

    fun loginUser(userDto: UserDto): JWTTokenResponse {
        val idToken =
            "eyJhbGciOiJSUzI1NiIsImtpZCI6ImE0MzRmMzFkN2Y3NWRiN2QyZjQ0YjgxZDg1MjMwZWQxN2ZlNTk3MzciLCJ0eXAiOiJKV1QifQ.eyJuYW1lIjoiWmVlc2hhbiBUdWZhaWwiLCJwaWN0dXJlIjoiaHR0cHM6Ly9saDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2EvQUNnOG9jS3hlSEhxNENsNU9RZjdDSENISHA4Ym1ObEswbmFGbzJHa282UTJPS0xDRjRkNjVHbHc9czk2LWMiLCJpc3MiOiJodHRwczovL3NlY3VyZXRva2VuLmdvb2dsZS5jb20vbG9hbi10cmFja2VyLTliMjVkIiwiYXVkIjoibG9hbi10cmFja2VyLTliMjVkIiwiYXV0aF90aW1lIjoxNzM4NTEyNzA3LCJ1c2VyX2lkIjoiQ01XTDB0YXBaR1NET0kzVGJ6UUVNOHZibFRsMiIsInN1YiI6IkNNV0wwdGFwWkdTRE9JM1RielFFTTh2YmxUbDIiLCJpYXQiOjE3Mzg1MTI3MDcsImV4cCI6MTczODUxNjMwNywiZW1haWwiOiJ6ZWVzaGFudHVmYWlsODZAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImZpcmViYXNlIjp7ImlkZW50aXRpZXMiOnsiZ29vZ2xlLmNvbSI6WyIxMTE4MzA4MjI1MDU4Mjc5Mzk3OTQiXSwiZW1haWwiOlsiemVlc2hhbnR1ZmFpbDg2QGdtYWlsLmNvbSJdfSwic2lnbl9pbl9wcm92aWRlciI6Imdvb2dsZS5jb20ifX0.T5p39Aja_tqrZ3Xlcl7AD0M9Lm9kkUY4ACNqr2a1eBfWfxp22Yu8BUkO3NiiSHAhx7-CgXRc8hs6KQRX3D5h8L_rwm3g5b7hVGkHy-YnvL0beOghhshJpp-WdYLP6xZ7gTB8ENwM8aC5U3kYuvc4VblwzdOC0jxkkvwNDDTmlhUJmVoua2VmEBjcuxEP0sILhHy0NWZjimf_DeeNDS7O6hI9uo5rnOfTPdfUaT5EagRyh0CNcP-FuxLsu6qFeMRqEXXIjYR8HpA3MnfcIen-_h-UTHWNxFv3SLIjkhkpRFP9oh7WGBIKJNfu6TExZlJ0A6aZSwF_lfxoGJdISnRLkQ"
        @Suppress("RunBlocking")
        (runBlocking {
            doReturn(
                userDto
            ).whenever(firebaseService).userByVerifyingIdToken(idToken)
        })

        val responseToken = webTestClient.post()
            .uri("/login")
            .header("Content-Type", "application/json")
            .bodyValue(LoginRequest("Bearer $idToken"))
            .exchange()
            .expectStatus().isOk
            .expectBody(JWTTokenResponse::class.java)
            .returnResult().responseBody

        Assertions.assertThat(responseToken).isNotNull
        return responseToken!!
    }

    fun addFriend(token: String, name: String = "John Doe"): FriendResponse {
        return webTestClient.post()
            .uri("/api/v1/friends/add")
            .header("Authorization", "Bearer $token")
            .bodyValue(
                FriendRequest(
                    name = name,
                    email = "${name.replace(" ", "_")}@gmail.com",
                    phoneNumber = "+923001234568",
                )
            )
            .exchange()
            .expectStatus().isOk
            .expectBody(FriendResponse::class.java).returnResult().responseBody!!
    }

    fun queryFriend(token: String): Paginated<FriendsResponse> {
        return webTestClient.get()
            .uri("/api/v1/friends")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody!!.let {
                objectMapper.readValue(
                    it,
                    object : TypeReference<Paginated<FriendsResponse>>() {})
            }
    }
}