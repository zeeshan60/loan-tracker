package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.transactions.AmountDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

class FriendsControllerAdapterTest {
    @Test
    fun `given balances takes first balance as main balance when main currency is not available`() {
        // Arrange
        val friendDto = FriendDto(
            friendId = UUID.randomUUID(),
            email = null,
            phoneNumber = null,
            photoUrl = null,
            name = "John",
            mainCurrency = null,
            balances = AllTimeBalanceDto(
                null, listOf(
                    AmountDto(
                        amount = 100.0.toBigDecimal(),
                        currency = Currency.getInstance("SGD"),
                        isOwed = true
                    ),
                    AmountDto(
                        amount = 200.0.toBigDecimal(),
                        currency = Currency.getInstance("USD"),
                        isOwed = false
                    )
                )
            )
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
        assertThat(result.mainBalance?.amount).isEqualTo(100.0.toBigDecimal())
        assertThat(result.mainBalance?.currency).isEqualTo("SGD")
        assertThat(result.mainBalance?.isOwed).isTrue()
        assertThat(result.otherBalances.size).isEqualTo(1)
        assertThat(result.otherBalances[0].amount).isEqualTo(200.0.toBigDecimal())
        assertThat(result.otherBalances[0].currency).isEqualTo("USD")
        assertThat(result.otherBalances[0].isOwed).isFalse()
    }

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
            balances = AllTimeBalanceDto(null, listOf(
                AmountDto(
                    amount = 100.0.toBigDecimal(),
                    currency = Currency.getInstance("SGD"),
                    isOwed = true
                ),
                AmountDto(
                    amount = 200.0.toBigDecimal(),
                    currency = Currency.getInstance("USD"),
                    isOwed = false
                )
            ))
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
        assertThat(result.otherBalances.size).isEqualTo(1)
        assertThat(result.otherBalances[0].amount).isEqualTo(100.0.toBigDecimal())
        assertThat(result.otherBalances[0].currency).isEqualTo("SGD")
        assertThat(result.otherBalances[0].isOwed).isTrue()
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
            balances = AllTimeBalanceDto(null, emptyList())
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