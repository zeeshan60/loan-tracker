package com.zeenom.loan_tracker.integration

import com.fasterxml.jackson.core.type.TypeReference
import com.zeenom.loan_tracker.friends.FriendEventRepository
import com.zeenom.loan_tracker.transactions.SplitType
import com.zeenom.loan_tracker.transactions.TransactionCreateRequest
import com.zeenom.loan_tracker.transactions.TransactionEventRepository
import com.zeenom.loan_tracker.transactions.TransactionResponse
import com.zeenom.loan_tracker.transactions.TransactionsResponse
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.util.UUID

class TransactionsDeleteAllBugTest: BaseIntegration() {

    @Autowired
    private lateinit var friendEventRepository: FriendEventRepository

    @Autowired
    private lateinit var userEventRepository: UserEventRepository

    @Autowired
    private lateinit var transactionEventRepository: TransactionEventRepository

    private lateinit var zeeToken: String

    private lateinit var transaction: TransactionResponse

    private var zeeDto = UserDto(
        uid = "123",
        email = "zee@gmail.com",
        phoneNumber = "+923001234567",
        displayName = "Zeeshan Tufail",
        photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2",
        currency = null,
        emailVerified = true
    )
    private lateinit var johnFriendId: UUID

    @BeforeAll
    fun setupBeforeAll(): Unit = runBlocking {
        userEventRepository.deleteAll()
        friendEventRepository.deleteAll()
        transactionEventRepository.deleteAll()
        zeeToken = loginUser(
            userDto = zeeDto
        ).token
        addFriend(zeeToken, "john")
        johnFriendId = queryFriend(
            token = zeeToken
        ).data.friends.first().friendId
    }
    @Order(1)
    @Test
    fun `add a transaction`() {

        webTestClient.post()
            .uri("/api/v1/transactions/add")
            .header("Authorization", "Bearer $zeeToken")
            .bodyValue(
                TransactionCreateRequest(
                    amount = 100.0.toBigDecimal(),
                    currency = "USD",
                    type = SplitType.YouPaidSplitEqually,
                    recipientId = johnFriendId,
                    description = "Sample transaction",
                    transactionDate = Instant.parse("2025-02-26T00:00:00Z")
                )
            )
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.description").isEqualTo("Sample transaction")
        val result = webTestClient.get()
            .uri("/api/v1/transactions/friend/byMonth?friendId=$johnFriendId&timeZone=Asia/Singapore")
            .header("Authorization", "Bearer $zeeToken")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody!!.let {
                objectMapper.readValue(
                    it,
                    object : TypeReference<TransactionsResponse>() {})
            }
        transaction = result.perMonth[0].transactions[0]
    }

    @Order(2)
    @Test
    fun `delete transaction as zee`() {
        webTestClient.delete()
            .uri("/api/v1/transactions/delete/transactionId/${transaction.transactionId}")
            .header("Authorization", "Bearer $zeeToken")
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.deleted").isEqualTo(true)

        val result = webTestClient.get()
            .uri("/api/v1/transactions/friend/byMonth?friendId=$johnFriendId&timeZone=Asia/Singapore")
            .header("Authorization", "Bearer $zeeToken")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody!!.let {
                objectMapper.readValue(
                    it,
                    object : TypeReference<TransactionsResponse>() {})
            }

        Assertions.assertThat(result.perMonth).hasSize(0)
    }
}