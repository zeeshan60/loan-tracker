package com.zeenom.loan_tracker.integration

import com.fasterxml.jackson.core.type.TypeReference
import com.zeenom.loan_tracker.friends.FriendEventRepository
import com.zeenom.loan_tracker.friends.FriendModelRepository
import com.zeenom.loan_tracker.friends.FriendResponse
import com.zeenom.loan_tracker.friends.UpdateFriendRequest
import com.zeenom.loan_tracker.transactions.SplitType
import com.zeenom.loan_tracker.transactions.TransactionCreateRequest
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

class FriendUpdateTransactionSyncIntegrationTest() : BaseIntegration() {

    @Autowired
    private lateinit var friendEventRepository: FriendEventRepository

    @Autowired
    private lateinit var userEventRepository: UserEventRepository

    @Autowired
    private lateinit var userModelRepository: UserModelRepository

    @Autowired
    private lateinit var friendModelRepository: FriendModelRepository

    private lateinit var zeeToken: String
    private var zeeDto = UserDto(
        uid = UUID.randomUUID(),
        userFBId = "123",
        email = "zee@gmail.com",
        phoneNumber = "+923001234567",
        displayName = "Zeeshan Tufail",
        photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2",
        currency = null,
        emailVerified = true
    )

    private lateinit var johnToken: String
    private var johnDto = UserDto(
        uid = UUID.randomUUID(),
        userFBId = "124",
        email = "john@gmail.com",
        phoneNumber = "+923001234569",
        displayName = "John Doe",
        photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl3",
        currency = null,
        emailVerified = true
    )

    private lateinit var johnFriendId: UUID
    @BeforeAll
    fun beforeAll(): Unit = runBlocking {
        userModelRepository.deleteAll()
        friendModelRepository.deleteAll()
        userEventRepository.deleteAll()
        friendEventRepository.deleteAll()
        zeeToken = loginUser(
            userDto = zeeDto
        ).token
        johnToken = loginUser(
            userDto = johnDto
        ).token

        addFriend(zeeToken, "JohnWrong")
        johnFriendId = queryFriend(
            token = zeeToken
        ).data.friends.first().friendId
        addTransaction(zeeToken, johnFriendId)
    }

    @Order(1)
    @Test
    fun `updating john correct phone sync zee transactions to john`() {
        queryFriend(
            token = johnToken
        ).data.friends.also {
            Assertions.assertThat(it).isEmpty()
        }
        queryTransactions(zeeToken, johnFriendId)!!.also {
            Assertions.assertThat(it.perMonth).hasSize(1)
        }
        webTestClient.put()
            .uri("/api/v1/friends/${johnFriendId}")
            .header("Authorization", "Bearer $zeeToken")
            .bodyValue(
                UpdateFriendRequest(
                    email = null,
                    phoneNumber = johnDto.phoneNumber,
                    name = null,
                )
            )
            .exchange()
            .expectStatus().isOk
            .expectBody(FriendResponse::class.java)
            .returnResult().responseBody!!
        val zeeFriendId = queryFriend(
            token = johnToken
        ).data.friends.first().friendId
        Assertions.assertThat(zeeFriendId).isNotNull
        val result = queryTransactions(johnToken, zeeFriendId)!!
        Assertions.assertThat(result.perMonth).hasSize(1)
        Assertions.assertThat(result.perMonth[0].transactions[0].friend.name).isEqualTo(zeeDto.displayName)
        Assertions.assertThat(result.perMonth[0].transactions[0].amount.amount).isEqualTo(50.0.toBigDecimal())
        Assertions.assertThat(result.perMonth[0].transactions[0].amount.currency).isEqualTo("USD")
        Assertions.assertThat(result.perMonth[0].transactions[0].amount.isOwed).isFalse
        Assertions.assertThat(result.perMonth[0].transactions[0].totalAmount).isEqualTo(100.0.toBigDecimal())
        Assertions.assertThat(result.perMonth[0].transactions[0].transactionId).isNotNull()
        Assertions.assertThat(result.perMonth[0].transactions[0].splitType).isEqualTo(SplitType.TheyPaidSplitEqually)
        Assertions.assertThat(result.perMonth[0].transactions[0].description).isEqualTo("Sample transaction")
    }

    fun addTransaction(token: String, friendId: UUID) {
        webTestClient.post()
            .uri("/api/v1/transactions/add")
            .header("Authorization", "Bearer $token")
            .bodyValue(
                TransactionCreateRequest(
                    amount = 100.0.toBigDecimal(),
                    currency = "USD",
                    type = SplitType.YouPaidSplitEqually,
                    recipientId = friendId,
                    description = "Sample transaction",
                    transactionDate = Instant.parse("2025-02-26T00:00:00Z"),
                    groupId = null,
                    groupAmountSplit = null,
                )
            )
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.description").isEqualTo("Sample transaction")
    }

    private fun queryTransactions(token: String, friendId: UUID): TransactionsResponse? = webTestClient.get()
        .uri("/api/v1/transactions/friend/byMonth?friendId=$friendId&timeZone=Asia/Singapore")
        .header("Authorization", "Bearer $token")
        .exchange()
        .expectStatus().isOk
        .expectBody(String::class.java)
        .returnResult().responseBody!!.let {
            objectMapper.readValue(
                it,
                object : TypeReference<TransactionsResponse>() {})
        }

}