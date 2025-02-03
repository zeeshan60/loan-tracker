package com.zeenom.loan_tracker.common

import org.springframework.stereotype.Component
import java.time.Instant

@Component
class SecondInstant {
    fun now(): Instant {
        return Instant.now().looseNanonSeconds()
    }
}