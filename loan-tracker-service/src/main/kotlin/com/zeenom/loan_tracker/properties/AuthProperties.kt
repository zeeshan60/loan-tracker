package com.zeenom.loan_tracker.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "auth")
class AuthProperties {
    lateinit var secretKey: String
    var jwtExpiryDays: Int = 0
}