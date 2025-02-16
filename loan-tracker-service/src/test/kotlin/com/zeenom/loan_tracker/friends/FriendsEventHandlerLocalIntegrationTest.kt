package com.zeenom.loan_tracker.friends

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeenom.loan_tracker.common.AmountDto
import com.zeenom.loan_tracker.common.r2dbc.toJson
import com.zeenom.loan_tracker.users.UserEntity
import com.zeenom.loan_tracker.users.UserRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.*

@SpringBootTest
@ActiveProfiles("local")
@Disabled
class FriendsEventHandlerLocalIntegrationTest(
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val friendRepository: FriendRepository,
    @Autowired private val userRepository: UserRepository,
) {

    @Test
    fun `add bunch of test friends`(): Unit = runBlocking {
        cleanup()
        val result = UserEntity(
            uid = "QlI1B6ili1SsB9ovzKharlWFvHr1",
            email = "nomantufail100@gmail.com",
            phoneNumber = null,
            displayName = "Noman Tufail",
            photoUrl = "https://lh3.googleusercontent.com/a/ACg8ocLl28eLnZCPrAzvJyKlz_UrvsCxYQu8yDSc-RQTP87SAQ1DCD9L=s96-c",
            emailVerified = true,
            createdAt = Instant.parse("2025-02-12T06:17:29.000Z"),
            updatedAt = Instant.parse("2025-02-12T17:58:25.000Z"),
            lastLoginAt = Instant.parse("2025-02-12T17:58:25.000Z")
        )
        val nomi = userRepository.save(result)

        val friendTemplate = UserFriendEntity(
            userId = nomi.id!!,
            friendId = null,
            friendEmail = "zeeshantufail86@gmail.com",
            friendPhoneNumber = "+923001234567",
            friendDisplayName = "Zeeshan Tufail",
            friendTotalAmountsDto = objectMapper.writeValueAsString(
                FriendTotalAmountsDto(
                    amountsPerCurrency = listOf(
                        AmountDto(
                            amount = 100.0.toBigDecimal(),
                            isOwed = true,
                            currency = Currency.getInstance("USD")
                        )
                    )
                )
            ).toJson(objectMapper),
            createdAt = Instant.parse("2025-02-13T01:04:14.000Z"),
            updatedAt = Instant.parse("2025-02-13T01:04:14.000Z")
        )

        (1..50).map {
            friendRepository.save(
                friendTemplate.copy(
                    friendEmail = "friend$it@gmail.com",
                    friendDisplayName = "Friend $it",
                    friendPhoneNumber = "+92300123456$it",
                    friendTotalAmountsDto = FriendTotalAmountsDto(
                        amountsPerCurrency = listOf(
                            AmountDto(
                                amount = (100.0 + it).toBigDecimal(),
                                currency = Currency.getInstance("USD"),
                                isOwed = it % 2 == 0,
                            )
                        )
                    ).toJson(objectMapper)
                )
            )
        }

    }

    private suspend fun cleanup() {
        userRepository.deleteAll()
        friendRepository.deleteAll()
    }
}