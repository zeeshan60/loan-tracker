package com.zeenom.loan_tracker.controllers

import com.zeenom.loan_tracker.dtos.JWTTokenResponse
import com.zeenom.loan_tracker.services.AuthService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class AuthController(val firebaseService: AuthService) {

    val logger = LoggerFactory.getLogger(AuthController::class.java)

    @PostMapping("/login")
    fun login(@RequestHeader("Authorization") authHeader: String?): Mono<ResponseEntity<JWTTokenResponse>> {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.error(IllegalArgumentException("Invalid Authorization header"))
        }
        val firebaseIdToken = authHeader.substring(7)
        return firebaseService.generateJwtUsingIdToken(firebaseIdToken)
            .map { decodedToken ->
                ResponseEntity.ok(JWTTokenResponse(token = decodedToken))
            }
            .onErrorResume {
                logger.error("Error generating JWT token", it)
                Mono.error(IllegalArgumentException("Invalid Firebase ID token"))
            }
    }
}
