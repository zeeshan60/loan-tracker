package com.zeenom.loan_tracker.transactions

import io.swagger.v3.core.util.Json
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class CurrencyClientTest {
    @Autowired
    private lateinit var currencyClient: CurrencyClient

    @ParameterizedTest
    @CsvSource(
        "1",
        "2"
    )
    fun `fetches currencies successfully`(): Unit = runBlocking {
        val response = currencyClient.fetchCurrencies()
        Json.prettyPrint(response)

        assertThat(response.rates).isNotEmpty
    }
}