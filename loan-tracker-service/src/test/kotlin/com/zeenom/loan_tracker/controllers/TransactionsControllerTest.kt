package com.zeenom.loan_tracker.controllers

import com.fasterxml.jackson.core.type.TypeReference
import com.zeenom.loan_tracker.common.Paginated
import com.zeenom.loan_tracker.prettyAndPrint
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
    private lateinit var transactionQuery: TransactionQuery

    @Autowired
    private lateinit var authService: AuthService

    @Test
    fun `given transactions spanning over months return correct grouping`() {
        val recipientId = UUID.randomUUID()
        whenever(runBlocking {
            transactionQuery.execute(
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
                        recipientId = recipientId,
                        recipientName = "John",
                        transactionStreamId = UUID.randomUUID(),
                        updatedAt = Instant.parse("2021-01-01T00:00:00Z"),
                        history = emptyList(),
                        createdAt = Instant.now(),
                        createdBy = "123",
                        createdByName = "Zeeshan Tufail",
                        updatedBy = null,
                        updatedByName = null,
                        deleted = false,
                    ),
                    TransactionDto(
                        currency = Currency.getInstance("SGD"),
                        description = "transaction 2",
                        originalAmount = 100.0.toBigDecimal(),
                        splitType = SplitType.YouPaidSplitEqually,
                        recipientId = recipientId,
                        recipientName = "John",
                        transactionStreamId = UUID.randomUUID(),
                        updatedAt = Instant.parse("2021-01-02T00:00:00Z"),
                        history = emptyList(),
                        createdAt = Instant.now(),
                        createdBy = "123",
                        createdByName = "Zeeshan Tufail",
                        updatedBy = null,
                        updatedByName = null,
                        deleted = false,
                    ),

                    TransactionDto(
                        currency = Currency.getInstance("SGD"),
                        description = "transaction 1",
                        originalAmount = 50.0.toBigDecimal(),
                        splitType = SplitType.TheyOweYouAll,
                        recipientId = recipientId,
                        recipientName = "John",
                        transactionStreamId = UUID.randomUUID(),
                        updatedAt = Instant.parse("2021-02-03T00:00:00Z"),
                        history = emptyList(),
                        createdAt = Instant.now(),
                        createdBy = "123",
                        createdByName = "Zeeshan Tufail",
                        updatedBy = null,
                        updatedByName = null,
                        deleted = false,
                    ),

                    TransactionDto(
                        currency = Currency.getInstance("SGD"),
                        description = "transaction 2",
                        originalAmount = 300.0.toBigDecimal(),
                        splitType = SplitType.YouPaidSplitEqually,
                        recipientId = recipientId,
                        recipientName = "John",
                        transactionStreamId = UUID.randomUUID(),
                        updatedAt = Instant.parse("2021-02-01T00:00:00Z"),
                        history = emptyList(),
                        createdAt = Instant.now(),
                        createdBy = "123",
                        createdByName = "Zeeshan Tufail",
                        updatedBy = null,
                        updatedByName = null,
                        deleted = false,
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

        result.prettyAndPrint(objectMapper)
        Assertions.assertThat(result.perMonth).hasSize(2)
        Assertions.assertThat(result.perMonth[0].transactions).hasSize(2)
        Assertions.assertThat(result.perMonth[0].transactions[0].amountResponse.amount).isEqualTo(100.0.toBigDecimal())
        Assertions.assertThat(result.perMonth[0].transactions[0].amountResponse.currency).isEqualTo("SGD")
        Assertions.assertThat(result.perMonth[0].transactions[0].amountResponse.isOwed).isTrue()
        Assertions.assertThat(result.perMonth[0].transactions[0].totalAmount).isEqualTo(200.0.toBigDecimal())
        Assertions.assertThat(result.perMonth[0].transactions[0].transactionId).isNotNull()
        Assertions.assertThat(result.perMonth[0].transactions[0].splitType).isEqualTo(SplitType.YouPaidSplitEqually)
        Assertions.assertThat(result.perMonth[0].transactions[0].description).isEqualTo("transaction 1")
        Assertions.assertThat(result.perMonth[0].transactions[1].amountResponse.amount).isEqualTo(50.0.toBigDecimal())
        Assertions.assertThat(result.perMonth[0].transactions[1].amountResponse.currency).isEqualTo("SGD")
        Assertions.assertThat(result.perMonth[0].transactions[1].amountResponse.isOwed).isTrue()
        Assertions.assertThat(result.perMonth[0].transactions[1].totalAmount).isEqualTo(100.0.toBigDecimal())
        Assertions.assertThat(result.perMonth[0].transactions[1].transactionId).isNotNull()
        Assertions.assertThat(result.perMonth[0].transactions[1].splitType).isEqualTo(SplitType.YouPaidSplitEqually)
        Assertions.assertThat(result.perMonth[0].transactions[1].description).isEqualTo("transaction 2")
        Assertions.assertThat(result.perMonth[1].transactions).hasSize(2)
        Assertions.assertThat(result.perMonth[1].transactions[0].amountResponse.amount).isEqualTo(50.0.toBigDecimal())
        Assertions.assertThat(result.perMonth[1].transactions[0].amountResponse.currency).isEqualTo("SGD")
        Assertions.assertThat(result.perMonth[1].transactions[0].amountResponse.isOwed).isTrue()
        Assertions.assertThat(result.perMonth[1].transactions[0].totalAmount).isEqualTo(50.0.toBigDecimal())
        Assertions.assertThat(result.perMonth[1].transactions[0].transactionId).isNotNull()
        Assertions.assertThat(result.perMonth[1].transactions[0].splitType).isEqualTo(SplitType.TheyOweYouAll)
        Assertions.assertThat(result.perMonth[1].transactions[0].description).isEqualTo("transaction 1")
        Assertions.assertThat(result.perMonth[1].transactions[1].amountResponse.amount).isEqualTo(150.0.toBigDecimal())
        Assertions.assertThat(result.perMonth[1].transactions[1].amountResponse.currency).isEqualTo("SGD")
        Assertions.assertThat(result.perMonth[1].transactions[1].amountResponse.isOwed).isTrue()
        Assertions.assertThat(result.perMonth[1].transactions[1].totalAmount).isEqualTo(300.0.toBigDecimal())
        Assertions.assertThat(result.perMonth[1].transactions[1].transactionId).isNotNull()
        Assertions.assertThat(result.perMonth[1].transactions[1].splitType).isEqualTo(SplitType.YouPaidSplitEqually)
        Assertions.assertThat(result.perMonth[1].transactions[1].description).isEqualTo("transaction 2")
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
            .expectBody().jsonPath("$.error.message").isEqualTo("400 BAD_REQUEST \"Required query parameter 'friendId' is not present.\"")
    }
}