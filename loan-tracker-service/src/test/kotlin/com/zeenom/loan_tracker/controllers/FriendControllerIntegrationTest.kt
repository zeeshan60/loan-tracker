package com.zeenom.loan_tracker.controllers

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.zeenom.loan_tracker.common.MessageResponse
import com.zeenom.loan_tracker.common.Paginated
import com.zeenom.loan_tracker.firebase.FirebaseService
import com.zeenom.loan_tracker.friends.*
import com.zeenom.loan_tracker.prettyAndPrint
import com.zeenom.loan_tracker.security.JWTTokenResponse
import com.zeenom.loan_tracker.security.LoginRequest
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventRepository
import com.zeenom.loan_tracker.users.UserRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FriendControllerIntegrationTest(
    @LocalServerPort private val port: Int,
    @Autowired @MockitoSpyBean private val firebaseService: FirebaseService,
) : TestPostgresConfig() {
    @Autowired
    private lateinit var friendRepository: FriendRepository

    @Autowired
    private lateinit var newFriendEventRepository: NewFriendEventRepository

    @Autowired
    private lateinit var userEventRepository: UserEventRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper
    private val webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()


    private lateinit var zeeToken: String
    private var zeeDto = UserDto(
        uid = "123",
        email = "zee@gmail.com",
        phoneNumber = "+923001234567",
        displayName = "Zeeshan Tufail",
        photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2",
        emailVerified = true
    )

    private lateinit var johnToken: String
    private var johnDto = UserDto(
        uid = "124",
        email = "john@gmail.com",
        phoneNumber = "+923001234568",
        displayName = "John Doe",
        photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl3",
        emailVerified = true
    )

    @BeforeAll
    fun beforeAll(): Unit = runBlocking {
        userEventRepository.deleteAll()
        friendRepository.deleteAll()
        zeeToken = loginUser(
            userDto = zeeDto
        ).token
    }

    @Test
    @Order(1)
    fun `user zee adds a friend john successfully`() {

        val responseMessage = addFriend(token = zeeToken)
        assertThat(responseMessage).isEqualTo(
            MessageResponse(
                message = "Friend added successfully"
            )
        )
    }

    @Test
    @Order(2)
    fun `user zee query friends receives john as friend successfully`() {
        val friendResponse = queryFriend(
            token = zeeToken
        )
        JSONAssert.assertEquals(
            """
                {
                  "data" : {
                    "friends" : [ {
                      "photoUrl" : null,
                      "name" : "John Doe",
                      "loanAmount" : null
                    } ]
                  },
                  "next" : null
                }
            """.trimIndent(),
            friendResponse.prettyAndPrint(objectMapper),
            true
        )
    }

    @Order(4)
    @Test
    fun `john logs in successfully`() {
        johnToken = loginUser(userDto = johnDto).token
    }

    @Order(5)
    @Test
    fun `john logs in and find zee as his friend`() {
        val response = queryFriend(token = johnToken)
        JSONAssert.assertEquals(
            """
                {
                  "data" : {
                    "friends" : [ {
                      "photoUrl" : "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2",
                      "name" : "Zeeshan Tufail",
                      "loanAmount" : null
                    } ]
                  },
                  "next" : null
                }
            """.trimIndent(),
            response.prettyAndPrint(objectMapper),
            true
        )
    }

    @Order(5)
    @Test
    fun `zee query friends again and john  is linked successfully`() {
        val response = queryFriend(zeeToken)
        //Check that photoUrl is now available for friend
        JSONAssert.assertEquals(
            """
                {
                  "data" : {
                    "friends" : [ {
                      "photoUrl" : "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl3",
                      "name" : "John Doe",
                      "loanAmount" : null
                    } ]
                  },
                  "next" : null
                }
            """.trimIndent(),
            response.prettyAndPrint(objectMapper),
            true
        )
    }

    private fun queryFriend(token: String): Paginated<FriendsResponse> {
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

    private fun addFriend(token: String): MessageResponse {
        return webTestClient.post()
            .uri("/api/v1/friends/add")
            .header("Authorization", "Bearer $token")
            .bodyValue(
                CreateFriendRequest(
                    name = "John Doe",
                    email = "john@gmail.com",
                    phoneNumber = "+923001234568",
                )
            )
            .exchange()
            .expectStatus().isOk
            .expectBody(MessageResponse::class.java).returnResult().responseBody!!
    }

    private fun loginUser(userDto: UserDto): JWTTokenResponse {
        val idToken =
            "eyJhbGciOiJSUzI1NiIsImtpZCI6ImE0MzRmMzFkN2Y3NWRiN2QyZjQ0YjgxZDg1MjMwZWQxN2ZlNTk3MzciLCJ0eXAiOiJKV1QifQ.eyJuYW1lIjoiWmVlc2hhbiBUdWZhaWwiLCJwaWN0dXJlIjoiaHR0cHM6Ly9saDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2EvQUNnOG9jS3hlSEhxNENsNU9RZjdDSENISHA4Ym1ObEswbmFGbzJHa282UTJPS0xDRjRkNjVHbHc9czk2LWMiLCJpc3MiOiJodHRwczovL3NlY3VyZXRva2VuLmdvb2dsZS5jb20vbG9hbi10cmFja2VyLTliMjVkIiwiYXVkIjoibG9hbi10cmFja2VyLTliMjVkIiwiYXV0aF90aW1lIjoxNzM4NTEyNzA3LCJ1c2VyX2lkIjoiQ01XTDB0YXBaR1NET0kzVGJ6UUVNOHZibFRsMiIsInN1YiI6IkNNV0wwdGFwWkdTRE9JM1RielFFTTh2YmxUbDIiLCJpYXQiOjE3Mzg1MTI3MDcsImV4cCI6MTczODUxNjMwNywiZW1haWwiOiJ6ZWVzaGFudHVmYWlsODZAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImZpcmViYXNlIjp7ImlkZW50aXRpZXMiOnsiZ29vZ2xlLmNvbSI6WyIxMTE4MzA4MjI1MDU4Mjc5Mzk3OTQiXSwiZW1haWwiOlsiemVlc2hhbnR1ZmFpbDg2QGdtYWlsLmNvbSJdfSwic2lnbl9pbl9wcm92aWRlciI6Imdvb2dsZS5jb20ifX0.T5p39Aja_tqrZ3Xlcl7AD0M9Lm9kkUY4ACNqr2a1eBfWfxp22Yu8BUkO3NiiSHAhx7-CgXRc8hs6KQRX3D5h8L_rwm3g5b7hVGkHy-YnvL0beOghhshJpp-WdYLP6xZ7gTB8ENwM8aC5U3kYuvc4VblwzdOC0jxkkvwNDDTmlhUJmVoua2VmEBjcuxEP0sILhHy0NWZjimf_DeeNDS7O6hI9uo5rnOfTPdfUaT5EagRyh0CNcP-FuxLsu6qFeMRqEXXIjYR8HpA3MnfcIen-_h-UTHWNxFv3SLIjkhkpRFP9oh7WGBIKJNfu6TExZlJ0A6aZSwF_lfxoGJdISnRLkQ"
        runBlocking {
            Mockito.doReturn(
                userDto
            ).whenever(firebaseService).userByVerifyingIdToken(idToken)
        }

        val responseToken = webTestClient.post()
            .uri("/login")
            .header("Content-Type", "application/json")
            .bodyValue(LoginRequest("Bearer $idToken"))
            .exchange()
            .expectStatus().isOk
            .expectBody(JWTTokenResponse::class.java)
            .returnResult().responseBody

        assertThat(responseToken).isNotNull
        return responseToken!!
    }

}