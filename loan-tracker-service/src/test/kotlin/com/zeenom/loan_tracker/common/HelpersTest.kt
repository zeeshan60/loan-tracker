package com.zeenom.loan_tracker.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
}