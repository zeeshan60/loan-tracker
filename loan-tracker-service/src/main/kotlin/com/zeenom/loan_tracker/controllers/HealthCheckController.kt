package com.zeenom.loan_tracker.controllers

import com.zeenom.loan_tracker.dtos.MessageResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthCheckController {

    @GetMapping("/health")
    suspend fun healthCheck(): MessageResponse {
        return MessageResponse(message = "I'm alive!")
    }
}

