package com.zeenom.loan_tracker.common

import com.zeenom.loan_tracker.transactions.SplitType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal
import java.time.Instant

class HelpersTest {

    @Test
    fun `start of month correctly sends start of month with timezone`() {
        // Arrange

        val instant = Instant.parse("2021-08-01T00:00:00+08:00")
        instant.toReadableDateFormat().also { println(it) }
        val expected = Instant.parse("2021-07-01T08:00:00+08:00")
        expected.toReadableDateFormat().also { println(it) }

        // Act
        val result = instant.startOfMonth("UTC")
        result.toReadableDateFormat().also { println(it) }
        // Assert
        assertThat(result).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        "5, 2.5",
        "10, 5",
        "5.0, 2.5",
        "5.5, 2.75",
    )
    fun `test that split types splits amount with decimal`(value: BigDecimal, expected: BigDecimal) {
        val amount = value
        val result = SplitType.YouPaidSplitEqually.apply(amount)
        assertThat(result).isEqualTo(expected)
    }
}