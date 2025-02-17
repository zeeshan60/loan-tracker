package com.zeenom.loan_tracker.controllers

import com.zeenom.loan_tracker.transactions.AmountDto
import com.zeenom.loan_tracker.common.exceptions.NotFoundException
import com.zeenom.loan_tracker.events.CommandDao
import com.zeenom.loan_tracker.events.CommandPayloadDto
import com.zeenom.loan_tracker.friends.*
import com.zeenom.loan_tracker.security.AuthService
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FriendsControllerTest(
    @LocalServerPort private val port: Int,
    @Autowired private val authService: AuthService,
    @Autowired @MockitoBean private val friendsEventHandler: FriendsEventHandler,
    @Autowired@MockitoBean private val commandDao: CommandDao
) {
    private val webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

    @Test
    fun `friends endpoint returns friends`(): Unit = runBlocking {
        val userid = "userid"
        val friendsDto = FriendsDto(
            friends = listOf(
                FriendDto(
                    name = "John Doe",
                    email = "sample@gmail.com",
                    phoneNumber = "+923001234567",
                    loanAmount = AmountDto(1000.0.toBigDecimal(), Currency.getInstance("USD"), true),
                    photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2"
                )
            )
        )
        Mockito.doReturn(
            friendsDto
        ).`when`(friendsEventHandler).findAllByUserId(userid)
        val result = webTestClient.get()
            .uri("/api/v1/friends")
            .header("Authorization", "Bearer ${authService.generateJwt(userid)}")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody

        JSONAssert.assertEquals(
            result,
            """
                {
                  "data" : {
                    "friends" : [ {
                      "photoUrl" : "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2",
                      "name" : "John Doe",
                      "loanAmount" : {
                        "amount" : 1000.0,
                        "isOwed" : true
                      }
                    } ]
                  },
                  "next" : null
                }
            """.trimIndent(),
            true
        )
    }

    @Test
    fun `addFriend creates friend successfully`(): Unit = runBlocking {
        Mockito.doReturn(Unit).`when`(friendsEventHandler).saveFriend(any(), any())
        Mockito.doReturn(Unit).`when`(commandDao).saveEvent<CommandPayloadDto>(any())
        webTestClient.post()
            .uri("/api/v1/friends/add")
            .header("Authorization", "Bearer ${authService.generateJwt("sample uid")}")
            .bodyValue(
                CreateFriendRequest(
                    name = "John Doe",
                    email = "john@gmail.com",
                    phoneNumber = "+923001234567",
                )
            )
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.message").isEqualTo("Friend added successfully")

        val captor = argumentCaptor<String, CreateFriendDto>()
        Mockito.verify(friendsEventHandler).saveFriend(captor.first.capture(), captor.second.capture())
        assertThat(captor.first.firstValue).isEqualTo("sample uid")
        assertThat(captor.second.firstValue).isEqualTo(
            CreateFriendDto(
                name = "John Doe",
                email = "john@gmail.com",
                phoneNumber = "+923001234567"
            )
        )
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

    @Test
    fun `test CORS preflight request is allowed`() {
        val response = webTestClient.options()
            .uri("/api/v1/friends") // Change this to a real endpoint
            .header("Origin", "http://example.com") // Simulate a request from a frontend
            .header("Access-Control-Request-Method", "POST") // Simulate preflight for POST
            .exchange()

        response.expectStatus().isOk // Ensure preflight is not blocked
        response.expectHeader().valueEquals("Access-Control-Allow-Origin", "*")
        response.expectHeader().valueEquals("Access-Control-Allow-Methods", "POST")
    }

    @Test
    fun `given service throws notfoudexception resolves to 404 with exception message`() = runBlocking {
        Mockito.doThrow(NotFoundException("Friend not found")).`when`(friendsEventHandler).findAllByUserId(any())
        val result = webTestClient.get()
            .uri("/api/v1/friends")
            .header("Authorization", "Bearer ${authService.generateJwt("sample uid")}")
            .exchange()
            .expectStatus().isNotFound
            .expectBody(String::class.java)
            .returnResult().responseBody

        JSONAssert.assertEquals(
            result,
            """
                {
                  "error" : {
                    "message" : "Friend not found"
                  }
                }
            """.trimIndent(),
            true
        )
    }
}

