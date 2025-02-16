package com.zeenom.loan_tracker.transactions

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeenom.loan_tracker.common.JacksonConfig
import com.zeenom.loan_tracker.common.SecondInstant
import com.zeenom.loan_tracker.common.looseNanonSeconds
import com.zeenom.loan_tracker.common.r2dbc.toJson
import com.zeenom.loan_tracker.friends.TestPostgresConfig
import com.zeenom.loan_tracker.users.UserEntity
import com.zeenom.loan_tracker.users.UserRepository
import io.swagger.v3.core.util.Json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.context.annotation.Import

@DataR2dbcTest
@Import(JacksonConfig::class)
@Disabled
class TransactionDaoTest(
    @Autowired private val transactionRepository: TransactionRepository,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val objectMapper: ObjectMapper,
) : TestPostgresConfig() {
    private val secondInstant = SecondInstant()
    val transactionDao = TransactionDao(transactionRepository)

    @Test
    fun `saves simple transaction object and reads it back successfully`(): Unit = runBlocking {
        val user = UserEntity(
            email = "test@gmail.com",
            uid = "someuid",
            phoneNumber = "1234567890",
            displayName = "Test User",
            photoUrl = "https://test.com",
            emailVerified = true,
            createdAt = secondInstant.now().looseNanonSeconds(),
            updatedAt = secondInstant.now().looseNanonSeconds(),
            lastLoginAt = null
        )

        val createdUser = userRepository.save(user)

        assertThat(createdUser.id).isNotNull

        val transaction = Transaction(
            amount = 100.0.toBigDecimal(),
            description = "Test Transaction",
            currency = "USD",
            date = secondInstant.now().looseNanonSeconds(),
            type = TransactionType.CREDIT,
            userId = createdUser.id!!,
            friendId = null,
            friendEmail = null,
            friendPhone = "1234567891",
            transactionTrail = TransactionTrailsDto(
                trail = emptyList()
            ).toJson(
                objectMapper = objectMapper
            ),
            deletedAt = null
        )

        transactionRepository.save(transaction)

        val savedTransaction = transactionRepository.findAll().first()
        assertThat(savedTransaction).isNotNull
        JSONAssert.assertEquals(
            Json.pretty(transaction.copy(id = savedTransaction.id)),
            Json.pretty(savedTransaction),
            true
        )

    }
}