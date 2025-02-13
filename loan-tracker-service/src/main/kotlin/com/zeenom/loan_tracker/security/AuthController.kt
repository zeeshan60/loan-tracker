package com.zeenom.loan_tracker.security

import com.google.firebase.auth.FirebaseAuthException
import com.zeenom.loan_tracker.common.exceptions.UnauthorizedException
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(val authService: AuthService) {

    val logger = LoggerFactory.getLogger(AuthController::class.java)

    @PostMapping("/login")
    suspend fun login(@RequestBody loginRequest: LoginRequest): ResponseEntity<JWTTokenResponse> {
        if (!loginRequest.idToken.startsWith("Bearer ")) {
            throw UnauthorizedException("Invalid Authorization header")
        }
        val idToken = loginRequest.idToken.substring(7)
        return try {
            val decodedToken = authService.generateJwtUsingIdToken(idToken)
            ResponseEntity.ok(JWTTokenResponse(token = decodedToken))
        } catch (e: FirebaseAuthException) {
            logger.error("Error generating JWT token", e)
            throw UnauthorizedException("Invalid Firebase ID token", e)
        }
    }
}

data class LoginRequest(val idToken: String)