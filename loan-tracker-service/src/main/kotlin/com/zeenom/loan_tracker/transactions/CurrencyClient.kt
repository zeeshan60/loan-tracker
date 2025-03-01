package com.zeenom.loan_tracker.transactions

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

@Component
@ConfigurationProperties(prefix = "currency.client")
class CurrencyClientProperties {
    lateinit var url: String
    lateinit var base: String
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class CurrencyResponse(val rates: Map<String, BigDecimal>)

interface ICurrencyClient {
    suspend fun fetchCurrencies(): CurrencyResponse
}

@Service
@Profile("!test")
class CurrencyClient(private val webClient: WebClient, private val currencyClientProperties: CurrencyClientProperties) :
    ICurrencyClient {
    private val logger = LoggerFactory.getLogger(CurrencyClient::class.java)
    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.HOURS)
        .build<String, Deferred<CurrencyResponse>>()

    override suspend fun fetchCurrencies(): CurrencyResponse {
        return cache.get("currencies") {
            CoroutineScope(Dispatchers.IO).async {
                logger.info("Fetching currencies")
                webClient.get().uri("${currencyClientProperties.url}/${currencyClientProperties.base}")
                    .retrieve()
                    .bodyToMono(CurrencyResponse::class.java)
                    .awaitSingle()
            }
        }!!.await()
    }
}

@Service
@Profile("test")
class TestCurrencyClient : ICurrencyClient {
    override suspend fun fetchCurrencies(): CurrencyResponse {
        return CurrencyResponse(
            mapOf(
                "USD" to 1.0.toBigDecimal(),
                "PKR" to 260.0.toBigDecimal(),
                "SGD" to 1.3.toBigDecimal()
            )
        )
    }
}