package com.zeenom.loan_tracker.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "auth")
class AuthProperties {
    lateinit var firebaseSecretJson: String
    lateinit var secretKey: String
    var jwtExpiryDays: Int = 0
}
