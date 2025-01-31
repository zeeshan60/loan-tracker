package com.zeenom.loan_tracker.controllers

import com.zeenom.loan_tracker.services.FirebaseService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.*

@RestController
@RequestMapping("/api/auth")
class AuthController(val firebaseService: FirebaseService) {

    private val secretKey = "your-secret-key" // Store this securely

    @PostMapping("/login")
    fun login(@RequestHeader("Authorization") authHeader: String?): Mono<ResponseEntity<Map<String, String>>> {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.badRequest().body(mapOf("error" to "Invalid token")))
        }

        val firebaseIdToken = authHeader.substring(7)
        return firebaseService.generateJwtUsingIdToken(firebaseIdToken)
            .map { decodedToken ->
                val customJwt = generateJwt(decodedToken)
                ResponseEntity.ok(mapOf("token" to customJwt))
            }
            .onErrorResume {
                Mono.just(ResponseEntity.badRequest().body(mapOf("error" to "Invalid Firebase token")))
            }
    }
}



