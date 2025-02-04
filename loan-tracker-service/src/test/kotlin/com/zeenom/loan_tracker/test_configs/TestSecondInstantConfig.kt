package com.zeenom.loan_tracker.test_configs

import com.zeenom.loan_tracker.common.SecondInstant
import com.zeenom.loan_tracker.common.looseNanonSeconds
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import java.time.Instant

@TestConfiguration
class TestSecondInstantConfig {

    @Bean
    fun secondInstant(): SecondInstant {
        val secondInstant = Mockito.mock(SecondInstant::class.java)
        Mockito.doReturn(Instant.now().looseNanonSeconds()).whenever(secondInstant).now()
        return secondInstant
    }
}