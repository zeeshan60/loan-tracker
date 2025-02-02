package com.zeenom.loan_tracker.controllers

import com.zeenom.loan_tracker.dtos.JWTTokenResponse
import com.zeenom.loan_tracker.services.AuthService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(val authService: AuthService) {

    val logger = LoggerFactory.getLogger(AuthController::class.java)

    @PostMapping("/login")
    suspend fun login(@RequestHeader("Authorization") authHeader: String?): ResponseEntity<JWTTokenResponse> {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw IllegalArgumentException("Invalid Authorization header")
        }
        val firebaseIdToken = authHeader.substring(7)
        return try {
            val decodedToken = authService.generateJwtUsingIdToken(firebaseIdToken)
            ResponseEntity.ok(JWTTokenResponse(token = decodedToken))
        } catch (e: Exception) {
            logger.error("Error generating JWT token", e)
            throw IllegalArgumentException("Invalid Firebase ID token")
        }
    }
}

