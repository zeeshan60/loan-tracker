package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.transactions.AmountDto
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

class AllTimeBalanceStrategyTest {
    @Test
    fun `balance is positive  when only one positive amount available`() {
        val allTimeBalanceStrategy = AllTimeBalanceStrategy()

        val amounts = listOf(
            AmountDto(
                amount = 100.0.toBigDecimal(),
                currency = Currency.getInstance("USD"),
                isOwed = true
            )
        )

        val balance = allTimeBalanceStrategy.calculateAllTimeBalance(amounts)

        Assertions.assertThat(balance.main).isNotNull
        Assertions.assertThat(balance.main!!.amount).isEqualTo(100.0.toBigDecimal())
        Assertions.assertThat(balance.main!!.currency).isEqualTo(Currency.getInstance("USD"))
        Assertions.assertThat(balance.main!!.isOwed).isTrue()

        Assertions.assertThat(balance.other).isEmpty()
    }

    @Test
    fun `with equal different currencies takes first one as main`() {
        val amountDto1 = AmountDto(
            amount = 100.0.toBigDecimal(),
            currency = Currency.getInstance("USD"),
            isOwed = true
        )

        val amountDto2 = AmountDto(
            amount = 100.0.toBigDecimal(),
            currency = Currency.getInstance("PKR"),
            isOwed = true
        )

        val allTimeBalanceStrategy = AllTimeBalanceStrategy()

        val amounts = listOf(amountDto1, amountDto2)

        val balance = allTimeBalanceStrategy.calculateAllTimeBalance(amounts)

        Assertions.assertThat(balance.main).isNotNull
        Assertions.assertThat(balance.main!!.amount).isEqualTo(100.0.toBigDecimal())
        Assertions.assertThat(balance.main!!.currency).isEqualTo(Currency.getInstance("USD"))
        Assertions.assertThat(balance.main!!.isOwed).isTrue()

        Assertions.assertThat(balance.other).hasSize(1)
        Assertions.assertThat(balance.other.first().amount).isEqualTo(100.0.toBigDecimal())
        Assertions.assertThat(balance.other.first().currency).isEqualTo(Currency.getInstance("PKR"))
        Assertions.assertThat(balance.other.first().isOwed).isTrue()
    }

    @Test
    fun `with PKR more than USD takes PKR as main currency`() {
        val amountDto1 = AmountDto(
            amount = 100.0.toBigDecimal(),
            currency = Currency.getInstance("USD"),
            isOwed = true
        )

        val amountDto2 = AmountDto(
            amount = 200.0.toBigDecimal(),
            currency = Currency.getInstance("PKR"),
            isOwed = true
        )

        val amountDto3 = AmountDto(
            amount = 100.0.toBigDecimal(),
            currency = Currency.getInstance("PKR"),
            isOwed = true
        )

        val allTimeBalanceStrategy = AllTimeBalanceStrategy()

        val amounts = listOf(amountDto1, amountDto2, amountDto3)

        val balance = allTimeBalanceStrategy.calculateAllTimeBalance(amounts)

        Assertions.assertThat(balance.main).isNotNull
        Assertions.assertThat(balance.main!!.amount).isEqualTo(300.0.toBigDecimal())
        Assertions.assertThat(balance.main!!.currency).isEqualTo(Currency.getInstance("PKR"))
        Assertions.assertThat(balance.main!!.isOwed).isTrue()

        Assertions.assertThat(balance.other).isNotEmpty
        Assertions.assertThat(balance.other.first().amount).isEqualTo(100.0.toBigDecimal())
        Assertions.assertThat(balance.other.first().currency).isEqualTo(Currency.getInstance("USD"))
        Assertions.assertThat(balance.other.first().isOwed).isTrue()
    }

    @Test
    fun `with negative balance reverts isowed`() {
        val amountDto1 = AmountDto(
            amount = 100.0.toBigDecimal(),
            currency = Currency.getInstance("USD"),
            isOwed = true
        )

        val amountDto2 = AmountDto(
            amount = 200.0.toBigDecimal(),
            currency = Currency.getInstance("PKR"),
            isOwed = true
        )

        val amountDto3 = AmountDto(
            amount = 300.0.toBigDecimal(),
            currency = Currency.getInstance("USD"),
            isOwed = false
        )

        val allTimeBalanceStrategy = AllTimeBalanceStrategy()

        val amounts = listOf(amountDto1, amountDto2, amountDto3)

        val balance = allTimeBalanceStrategy.calculateAllTimeBalance(amounts)

        Assertions.assertThat(balance.main).isNotNull
        Assertions.assertThat(balance.main!!.amount).isEqualTo(200.0.toBigDecimal())
        Assertions.assertThat(balance.main!!.currency).isEqualTo(Currency.getInstance("USD"))
        Assertions.assertThat(balance.main!!.isOwed).isFalse()

        Assertions.assertThat(balance.other).isNotEmpty
        Assertions.assertThat(balance.other.first().amount).isEqualTo(200.0.toBigDecimal())
        Assertions.assertThat(balance.other.first().currency).isEqualTo(Currency.getInstance("PKR"))
        Assertions.assertThat(balance.other.first().isOwed).isTrue()
    }
}