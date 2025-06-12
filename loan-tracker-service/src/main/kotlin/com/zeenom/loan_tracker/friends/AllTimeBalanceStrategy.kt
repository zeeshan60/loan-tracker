package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.transactions.AmountDto
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

@Service
class AllTimeBalanceStrategy {
    fun calculateAllTimeBalance(
        amounts: List<AmountDto>,
        currencyRateMap: Map<String, BigDecimal>,
        baseCurrency: String,
    ): AllTimeBalanceDto {
        val sortedByDescendingEntries =
            amounts
                .groupBy { it.currency }.entries.sortedByDescending { it.value.size }
        val other = sortedByDescendingEntries.associate { it.key to it.value }

        val otherAmounts = other.map {
            val total = it.value.sumOf { amount -> if (amount.isOwed) amount.amount else -amount.amount }
            AmountDto(total.abs(), it.key, total >= 0.toBigDecimal())
        }

        val otherBalances = otherAmounts.map {
            if (it.currency.currencyCode != baseCurrency) {
                val total = convertCurrency(it.amount, it.currency.currencyCode, baseCurrency, currencyRateMap)
                OtherBalanceDto(
                    amount = it,
                    convertedAmount = AmountDto(total, Currency.getInstance(baseCurrency), it.isOwed)
                )
            } else {
                OtherBalanceDto(amount = it, convertedAmount = it)
            }
        }

        val main = if (otherAmounts.isEmpty()) null else otherAmounts.map {
            if (it.currency.currencyCode != baseCurrency) {
                val total = convertCurrency(it.amount, it.currency.currencyCode, baseCurrency, currencyRateMap)
                AmountDto(total, Currency.getInstance(baseCurrency), it.isOwed)
            } else {
                it
            }
        }.sumOf { amount -> if (amount.isOwed) amount.amount else -amount.amount }
            .let { AmountDto(it.abs(), Currency.getInstance(baseCurrency), it >= 0.toBigDecimal()) }
        return AllTimeBalanceDto(
            main = main,
            other = otherBalances
        )
    }

    fun convertCurrency(
        amount: BigDecimal,
        currentCurrency: String,
        targetCurrency: String,
        currencyRateMap: Map<String, BigDecimal>,
    ): BigDecimal {
        val rate = currencyRateMap[targetCurrency]
            ?: throw IllegalArgumentException("Currency rate for $targetCurrency not found")
        val currentRate = currencyRateMap[currentCurrency]
            ?: throw IllegalArgumentException("Currency rate for $currentCurrency not found")
        return amount.multiply(rate).divide(currentRate, 10, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros().toPlainString().toBigDecimal()
    }
}