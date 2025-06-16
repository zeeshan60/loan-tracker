package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.transactions.AmountDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

class FriendsControllerAdapterTest {

    @Test
    fun `given balances takes main currency balance as main balance when main currency is available`() {
        // Arrange
        val friendDto = FriendDto(
            friendId = UUID.randomUUID(),
            email = null,
            phoneNumber = null,
            photoUrl = null,
            name = "John",
            mainCurrency = Currency.getInstance("USD"),
            balances = AllTimeBalanceDto(
                AmountDto(
                    amount = 200.0.toBigDecimal(),
                    currency = Currency.getInstance("USD"),
                    isOwed = false
                ), listOf(
                    OtherBalanceDto(
                        AmountDto(
                            amount = 100.0.toBigDecimal(),
                            currency = Currency.getInstance("SGD"),
                            isOwed = true
                        ), AmountDto(
                            amount = 100.0.toBigDecimal(),
                            currency = Currency.getInstance("SGD"),
                            isOwed = true
                        )
                    ),
                    OtherBalanceDto(
                        AmountDto(
                            amount = 200.0.toBigDecimal(),
                            currency = Currency.getInstance("USD"),
                            isOwed = false
                        ), AmountDto(
                            amount = 200.0.toBigDecimal(),
                            currency = Currency.getInstance("USD"),
                            isOwed = false
                        )
                    )
                )
            ),
            transactionUpdatedAt = null
        )

        // Act
        val adapter = FriendsControllerAdapter()
        val result = adapter.fromDtoToPaginatedResponse(
            FriendsWithAllTimeBalancesDto(
                listOf(friendDto),
                balance = AllTimeBalanceDto(
                    main = null,
                    other = emptyList()
                )
            )
        ).data.friends[0]

        // Assert
        assertThat(result.mainBalance?.amount).isEqualTo(200.0.toBigDecimal())
        assertThat(result.mainBalance?.currency).isEqualTo("USD")
        assertThat(result.mainBalance?.isOwed).isFalse()
        assertThat(result.otherBalances.size).isEqualTo(2)
        assertThat(result.otherBalances[0].amount.amount).isEqualTo(100.0.toBigDecimal())
        assertThat(result.otherBalances[0].amount.currency).isEqualTo("SGD")
        assertThat(result.otherBalances[0].amount.isOwed).isTrue()
    }

    @Test
    fun `given no balances returns mainBalance as null and other balances as empty`() {
        // Arrange
        val friendDto = FriendDto(
            friendId = UUID.randomUUID(),
            email = null,
            phoneNumber = null,
            photoUrl = null,
            name = "John",
            mainCurrency = Currency.getInstance("USD"),
            balances = AllTimeBalanceDto(null, emptyList()),
            transactionUpdatedAt = null
        )

        // Act
        val adapter = FriendsControllerAdapter()
        val result = adapter.fromDtoToPaginatedResponse(
            FriendsWithAllTimeBalancesDto(
                listOf(friendDto),
                balance = AllTimeBalanceDto(
                    main = null,
                    other = emptyList()
                )
            )
        ).data.friends[0]

        // Assert
        assertThat(result.mainBalance).isNull()
        assertThat(result.otherBalances).isEmpty()
    }
}