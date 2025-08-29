package com.zeenom.loan_tracker.integration

import com.fasterxml.jackson.core.type.TypeReference
import com.zeenom.loan_tracker.friends.FriendEventRepository
import com.zeenom.loan_tracker.friends.FriendModelRepository
import com.zeenom.loan_tracker.transactions.SplitType
import com.zeenom.loan_tracker.transactions.TransactionCreateRequest
import com.zeenom.loan_tracker.transactions.TransactionEventRepository
import com.zeenom.loan_tracker.transactions.TransactionResponse
import com.zeenom.loan_tracker.transactions.TransactionsResponse
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventRepository
import com.zeenom.loan_tracker.users.UserModelRepository
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

    @Autowired
    private lateinit var userModelRepository: UserModelRepository

    @Autowired
    private lateinit var friendModelRepository: FriendModelRepository

    private lateinit var zeeToken: String

    private lateinit var transaction: TransactionResponse

    private var zeeDto = UserDto(
        uid = null,
        userFBId = "123",
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
        userModelRepository.deleteAll()
        friendModelRepository.deleteAll()
        userEventRepository.deleteAll()
        friendEventRepository.deleteAll()
        transactionEventRepository.deleteAll()
        zeeToken = loginUser(
            userDto = zeeDto
        ).token
        zeeDto = userModelRepository.findByUidAndDeletedIsFalse(zeeDto.userFBId)!!.let {
            UserDto(
                uid = it.streamId,
                userFBId = it.uid,
                email = it.email,
                phoneNumber = it.phoneNumber,
                displayName = it.displayName,
                photoUrl = it.photoUrl,
                currency = it.currency,
                emailVerified = it.emailVerified
            )
        }
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
                    transactionDate = Instant.parse("2025-02-26T00:00:00Z"),
                    groupId = null,
                    groupAmountSplit = null
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