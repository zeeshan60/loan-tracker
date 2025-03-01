package com.zeenom.loan_tracker.controllers

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.zeenom.loan_tracker.common.Paginated
import com.zeenom.loan_tracker.common.exceptions.NotFoundException
import com.zeenom.loan_tracker.events.CommandDao
import com.zeenom.loan_tracker.events.CommandPayloadDto
import com.zeenom.loan_tracker.friends.*
import com.zeenom.loan_tracker.security.AuthService
import com.zeenom.loan_tracker.transactions.AmountDto
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever
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
    @Autowired @MockitoBean private val friendService: FriendService,
    @Autowired @MockitoBean private val commandDao: CommandDao,
    @Autowired private val objectMapper: ObjectMapper,
) {
    private val webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

    @Test
    fun `friends endpoint returns friends`(): Unit = runBlocking {
        val userid = "userid"
        val friendsDto = FriendsWithAllTimeBalancesDto(
            friends = listOf(
                FriendDto(
                    name = "John Doe",
                    email = "sample@gmail.com",
                    phoneNumber = "+923001234567",
                    balances = AllTimeBalanceDto(
                        AmountDto(1000.0.toBigDecimal(), Currency.getInstance("USD"), true),
                        listOf(AmountDto(1000.0.toBigDecimal(), Currency.getInstance("USD"), true))
                    ),
                    mainCurrency = Currency.getInstance("USD"),
                    photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2",
                    friendId = UUID.randomUUID()
                )
            ),
            balance = AllTimeBalanceDto(
                main = null,
                other = emptyList()
            )
        )
        Mockito.doReturn(
            friendsDto
        ).`when`(friendService).findAllByUserId(userid)
        val result = webTestClient.get()
            .uri("/api/v1/friends")
            .header("Authorization", "Bearer ${authService.generateJwt(userid)}")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody!!.let {
                objectMapper.readValue(
                    it,
                    object : TypeReference<Paginated<FriendsResponse>>() {})
            }

        assertThat(result.data.friends).hasSize(1)
        assertThat(result.data.friends[0].name).isEqualTo("John Doe")
        assertThat(result.data.friends[0].mainBalance?.amount).isEqualTo(1000.0.toBigDecimal())
        assertThat(result.data.friends[0].mainBalance?.isOwed).isTrue()
        assertThat(result.data.friends[0].photoUrl).isEqualTo("https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2")
        assertThat(result.data.friends[0].friendId).isNotNull()
    }

    @Test
    fun `addFriend creates friend successfully`(): Unit = runBlocking {
        Mockito.doReturn(Unit).`when`(friendService).createFriend(any(), any())
        whenever(
            friendService.findByUserIdFriendId(
                userId = "sample uid",
                friendEmail = "john@gmail.com",
                friendPhone = "+923001234567"
            )
        ).thenReturn(
            FriendDto(
                friendId = UUID.randomUUID(),
                email = "john@gmail.com",
                phoneNumber = "+923001234567",
                photoUrl = "some photo",
                name = "John Doe",
                mainCurrency = null,
                balances = AllTimeBalanceDto(
                    main = null,
                    other = emptyList()
                )
            )
        )
        Mockito.doReturn(Unit).`when`(commandDao).addCommand<CommandPayloadDto>(any())
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
            .jsonPath("$.name").isEqualTo("John Doe")

        val captor = argumentCaptor<String, CreateFriendDto>()
        Mockito.verify(friendService).createFriend(captor.first.capture(), captor.second.capture())
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
            .header("Origin", "https://any-origin.com")
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
            .header("Origin", "https://example.com") // Simulate a request from a frontend
            .header("Access-Control-Request-Method", "POST") // Simulate preflight for POST
            .exchange()

        response.expectStatus().isOk // Ensure preflight is not blocked
        response.expectHeader().valueEquals("Access-Control-Allow-Origin", "*")
        response.expectHeader().valueEquals("Access-Control-Allow-Methods", "POST")
    }

    @Test
    fun `given service throws notfoudexception resolves to 404 with exception message`() = runBlocking {
        Mockito.doThrow(NotFoundException("Friend not found")).`when`(friendService).findAllByUserId(any())
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

