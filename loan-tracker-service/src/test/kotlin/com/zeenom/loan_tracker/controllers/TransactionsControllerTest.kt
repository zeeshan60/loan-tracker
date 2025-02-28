package com.zeenom.loan_tracker.controllers

import com.fasterxml.jackson.core.type.TypeReference
import com.zeenom.loan_tracker.common.Paginated
import com.zeenom.loan_tracker.friends.FriendSummaryDto
import com.zeenom.loan_tracker.security.AuthService
import com.zeenom.loan_tracker.transactions.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Instant
import java.util.*

class TransactionsControllerTest(@LocalServerPort private val port: Int) : BaseIntegration(port) {
    @MockitoBean
    private lateinit var createTransactionCommand: CreateTransactionCommand

    @MockitoBean
    private lateinit var updateTransactionCommand: UpdateTransactionCommand

    @MockitoBean
    private lateinit var transactionsQuery: TransactionsQuery

    @Autowired
    private lateinit var authService: AuthService

    @Test
    fun `given transactions spanning over months return correct grouping`() {
        val recipientId = UUID.randomUUID()
        whenever(runBlocking {
            transactionsQuery.execute(
                FriendTransactionQueryDto(
                    "123",
                    recipientId
                )
            )
        }).thenReturn(
            Paginated(
                listOf(
                    TransactionDto(
                        currency = Currency.getInstance("SGD"),
                        description = "transaction 1",
                        originalAmount = 200.0.toBigDecimal(),
                        splitType = SplitType.YouPaidSplitEqually,
                        friendSummaryDto = FriendSummaryDto(
                            recipientId,
                            "john@gmail.com",
                            "+923001234567",
                            "John",
                            "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2",
                        ),
                        transactionStreamId = UUID.randomUUID(),
                        updatedAt = Instant.parse("2021-01-01T00:00:00Z"),
                        history = emptyList(),
                        createdAt = Instant.now(),
                        createdBy = "123",
                        createdByName = "Zeeshan Tufail",
                        updatedBy = null,
                        updatedByName = null,
                        deleted = false,
                        transactionDate = Instant.parse("2021-01-01T00:00:00Z")
                    ),
                    TransactionDto(
                        currency = Currency.getInstance("SGD"),
                        description = "transaction 2",
                        originalAmount = 100.0.toBigDecimal(),
                        splitType = SplitType.YouPaidSplitEqually,
                        friendSummaryDto = FriendSummaryDto(
                            recipientId,
                            "john@gmail.com",
                            "+923001234567",
                            "John",
                            "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2",
                        ),
                        transactionStreamId = UUID.randomUUID(),
                        updatedAt = Instant.parse("2021-01-02T00:00:00Z"),
                        history = emptyList(),
                        createdAt = Instant.now(),
                        createdBy = "123",
                        createdByName = "Zeeshan Tufail",
                        updatedBy = null,
                        updatedByName = null,
                        deleted = false,
                        transactionDate = Instant.parse("2021-01-02T00:00:00Z")

                    ),

                    TransactionDto(
                        currency = Currency.getInstance("SGD"),
                        description = "transaction 1",
                        originalAmount = 50.0.toBigDecimal(),
                        splitType = SplitType.TheyOweYouAll,
                        friendSummaryDto = FriendSummaryDto(
                            recipientId,
                            "john@gmail.com",
                            "+923001234567",
                            "John",
                            "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2",
                        ),
                        transactionStreamId = UUID.randomUUID(),
                        updatedAt = Instant.parse("2021-02-03T00:00:00Z"),
                        history = emptyList(),
                        createdAt = Instant.now(),
                        createdBy = "123",
                        createdByName = "Zeeshan Tufail",
                        updatedBy = null,
                        updatedByName = null,
                        deleted = false,
                        transactionDate = Instant.parse("2021-02-03T00:00:00Z")
                    ),

                    TransactionDto(
                        currency = Currency.getInstance("SGD"),
                        description = "transaction 2",
                        originalAmount = 300.0.toBigDecimal(),
                        splitType = SplitType.YouPaidSplitEqually,
                        friendSummaryDto = FriendSummaryDto(
                            recipientId,
                            "john@gmail.com",
                            "+923001234567",
                            "John",
                            "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2",
                        ),
                        transactionStreamId = UUID.randomUUID(),
                        updatedAt = Instant.parse("2021-02-01T00:00:00Z"),
                        history = emptyList(),
                        createdAt = Instant.now(),
                        createdBy = "123",
                        createdByName = "Zeeshan Tufail",
                        updatedBy = null,
                        updatedByName = null,
                        deleted = false,
                        transactionDate = Instant.parse("2021-02-01T00:00:00Z")
                    )
                ),
                next = null
            )
        )

        val result = webTestClient.get()
            .uri("/api/v1/transactions/friend/byMonth?friendId=$recipientId&timeZone=Asia/Singapore")
            .header("Authorization", "Bearer ${authService.generateJwt("123")}")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody!!.let {
                objectMapper.readValue(
                    it,
                    object : TypeReference<TransactionsResponse>() {})
            }

        Assertions.assertThat(result.perMonth).hasSize(2)
        Assertions.assertThat(result.perMonth[0].transactions).hasSize(2)
        val month1Transaction1 = result.perMonth[0].transactions[0]
        Assertions.assertThat(month1Transaction1.amountResponse.amount).isEqualTo(50.0.toBigDecimal())
        Assertions.assertThat(month1Transaction1.amountResponse.currency).isEqualTo("SGD")
        Assertions.assertThat(month1Transaction1.amountResponse.isOwed).isTrue()
        Assertions.assertThat(month1Transaction1.totalAmount).isEqualTo(50.0.toBigDecimal())
        Assertions.assertThat(month1Transaction1.transactionId).isNotNull()
        Assertions.assertThat(month1Transaction1.splitType).isEqualTo(SplitType.TheyOweYouAll)
        Assertions.assertThat(month1Transaction1.description).isEqualTo("transaction 1")
        Assertions.assertThat(month1Transaction1.date).isEqualTo(Instant.parse("2021-02-03T00:00:00Z"))
        val month1Transaction2 = result.perMonth[0].transactions[1]
        Assertions.assertThat(month1Transaction2.amountResponse.amount).isEqualTo(150.0.toBigDecimal())
        Assertions.assertThat(month1Transaction2.amountResponse.currency).isEqualTo("SGD")
        Assertions.assertThat(month1Transaction2.amountResponse.isOwed).isTrue()
        Assertions.assertThat(month1Transaction2.totalAmount).isEqualTo(300.0.toBigDecimal())
        Assertions.assertThat(month1Transaction2.transactionId).isNotNull()
        Assertions.assertThat(month1Transaction2.splitType).isEqualTo(SplitType.YouPaidSplitEqually)
        Assertions.assertThat(month1Transaction2.description).isEqualTo("transaction 2")
        Assertions.assertThat(month1Transaction2.date).isEqualTo(Instant.parse("2021-02-01T00:00:00Z"))
        Assertions.assertThat(result.perMonth[1].transactions).hasSize(2)
        val month2Transaction1 = result.perMonth[1].transactions[0]
        Assertions.assertThat(month2Transaction1.amountResponse.amount).isEqualTo(50.0.toBigDecimal())
        Assertions.assertThat(month2Transaction1.amountResponse.currency).isEqualTo("SGD")
        Assertions.assertThat(month2Transaction1.amountResponse.isOwed).isTrue()
        Assertions.assertThat(month2Transaction1.totalAmount).isEqualTo(100.0.toBigDecimal())
        Assertions.assertThat(month2Transaction1.transactionId).isNotNull()
        Assertions.assertThat(month2Transaction1.splitType).isEqualTo(SplitType.YouPaidSplitEqually)
        Assertions.assertThat(month2Transaction1.description).isEqualTo("transaction 2")
        Assertions.assertThat(month2Transaction1.date).isEqualTo(Instant.parse("2021-01-02T00:00:00Z"))
        val month2Transaction2 = result.perMonth[1].transactions[1]
        Assertions.assertThat(month2Transaction2.amountResponse.amount).isEqualTo(100.0.toBigDecimal())
        Assertions.assertThat(month2Transaction2.amountResponse.currency).isEqualTo("SGD")
        Assertions.assertThat(month2Transaction2.amountResponse.isOwed).isTrue()
        Assertions.assertThat(month2Transaction2.totalAmount).isEqualTo(200.0.toBigDecimal())
        Assertions.assertThat(month2Transaction2.transactionId).isNotNull()
        Assertions.assertThat(month2Transaction2.splitType).isEqualTo(SplitType.YouPaidSplitEqually)
        Assertions.assertThat(month2Transaction2.description).isEqualTo("transaction 1")
        Assertions.assertThat(month2Transaction2.date).isEqualTo(Instant.parse("2021-01-01T00:00:00Z"))
    }

    @Test
    fun `invalid zone id throws bad request error`() {
        webTestClient.get()
            .uri("/api/v1/transactions/friend/byMonth?friendId=${UUID.randomUUID()}&timeZone=Asia/invalid")
            .header("Authorization", "Bearer ${authService.generateJwt("123")}")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody().jsonPath("$.error.message").isEqualTo("Invalid timezone")

    }

    @Test
    fun `send a requried query param as null throws bad request`() {
        webTestClient.get()
            .uri("/api/v1/transactions/friend/byMonth?timeZone=Asia/Singapore")
            .header("Authorization", "Bearer ${authService.generateJwt("123")}")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody().jsonPath("$.error.message")
            .isEqualTo("400 BAD_REQUEST \"Required query parameter 'friendId' is not present.\"")
    }
}