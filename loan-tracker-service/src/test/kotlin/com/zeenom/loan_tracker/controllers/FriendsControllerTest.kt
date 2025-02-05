package com.zeenom.loan_tracker.controllers

import com.zeenom.loan_tracker.friends.CreateFriendDto
import com.zeenom.loan_tracker.friends.CreateFriendRequest
import com.zeenom.loan_tracker.friends.FriendsDao
import com.zeenom.loan_tracker.security.AuthService
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
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
    private lateinit var friendsDao: FriendsDao

    @Test
    fun `friends endpoint returns friends`(): Unit = runBlocking {
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
    fun `addFriend creates friend successfully`(): Unit = runBlocking {
        Mockito.doReturn(Unit).`when`(friendsDao).saveFriend(any(), any())
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
        Mockito.verify(friendsDao).saveFriend(captor.first.capture(), captor.second.capture())
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
}

