package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.currencyRateMap
import com.zeenom.loan_tracker.transactions.AmountDto
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
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

        val balance = allTimeBalanceStrategy.calculateAllTimeBalance(amounts, currencyRateMap, "USD")

        assertThat(balance.main).isNotNull
        assertThat(balance.main!!.amount).isEqualTo(100.0.toBigDecimal())
        assertThat(balance.main!!.currency).isEqualTo(Currency.getInstance("USD"))
        assertThat(balance.main!!.isOwed).isTrue()

        assertThat(balance.other[0].amount).isEqualTo(100.0.toBigDecimal())
        assertThat(balance.other[0].currency).isEqualTo(Currency.getInstance("USD"))
        assertThat(balance.other[0].isOwed).isTrue()
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

        val balance = allTimeBalanceStrategy.calculateAllTimeBalance(amounts, currencyRateMap, "USD")

        assertThat(balance.main).isNotNull
        assertThat(balance.main!!.amount).isEqualTo(100.38.toBigDecimal())
        assertThat(balance.main!!.currency).isEqualTo(Currency.getInstance("USD"))
        assertThat(balance.main!!.isOwed).isTrue()

        assertThat(balance.other).hasSize(2)

        assertThat(balance.other[0].amount).isEqualTo(100.0.toBigDecimal())
        assertThat(balance.other[0].currency).isEqualTo(Currency.getInstance("USD"))
        assertThat(balance.other[0].isOwed).isTrue()
        assertThat(balance.other[1].amount).isEqualTo(100.0.toBigDecimal())
        assertThat(balance.other[1].currency).isEqualTo(Currency.getInstance("PKR"))
        assertThat(balance.other[1].isOwed).isTrue()
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

        val balance = allTimeBalanceStrategy.calculateAllTimeBalance(amounts, currencyRateMap, "PKR")

        assertThat(balance.main).isNotNull
        assertThat(balance.main!!.amount).isEqualTo(26300.0.toBigDecimal())
        assertThat(balance.main!!.currency).isEqualTo(Currency.getInstance("PKR"))
        assertThat(balance.main!!.isOwed).isTrue()

        assertThat(balance.other).hasSize(2)

        assertThat(balance.other[0].amount).isEqualTo(300.0.toBigDecimal())
        assertThat(balance.other[0].currency).isEqualTo(Currency.getInstance("PKR"))
        assertThat(balance.other[0].isOwed).isTrue()

        assertThat(balance.other[1].amount).isEqualTo(100.0.toBigDecimal())
        assertThat(balance.other[1].currency).isEqualTo(Currency.getInstance("USD"))
        assertThat(balance.other[1].isOwed).isTrue()
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

        val balance = allTimeBalanceStrategy.calculateAllTimeBalance(amounts, currencyRateMap, "USD")

        assertThat(balance.main).isNotNull
        assertThat(balance.main!!.amount).isEqualTo(199.23.toBigDecimal())
        assertThat(balance.main!!.currency).isEqualTo(Currency.getInstance("USD"))
        assertThat(balance.main!!.isOwed).isFalse()

        assertThat(balance.other).hasSize(2)
        assertThat(balance.other[0].amount).isEqualTo(200.0.toBigDecimal())
        assertThat(balance.other[0].currency).isEqualTo(Currency.getInstance("USD"))
        assertThat(balance.other[0].isOwed).isFalse()
        assertThat(balance.other[1].amount).isEqualTo(200.0.toBigDecimal())
        assertThat(balance.other[1].currency).isEqualTo(Currency.getInstance("PKR"))
        assertThat(balance.other[1].isOwed).isTrue()
    }
}