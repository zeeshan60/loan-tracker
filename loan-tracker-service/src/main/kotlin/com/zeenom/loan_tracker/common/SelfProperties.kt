package com.zeenom.loan_tracker.common

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "self")
class SelfProperties {
    lateinit var apiUrl: String
}