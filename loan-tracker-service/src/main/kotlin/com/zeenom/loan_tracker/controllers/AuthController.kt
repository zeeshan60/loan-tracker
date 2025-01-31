package com.zeenom.loan_tracker.controllers

import com.zeenom.loan_tracker.dtos.JWTTokenResponse
import com.zeenom.loan_tracker.services.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/auth")
class AuthController(val firebaseService: AuthService) {

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
                Mono.error(IllegalArgumentException("Invalid Firebase ID token"))
            }
    }
}
