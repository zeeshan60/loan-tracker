package com.zeenom.loan_tracker.security

import com.zeenom.loan_tracker.common.exceptions.UnauthorizedException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(val authService: AuthService) {

    @PostMapping("/login")
    suspend fun login(@RequestBody loginRequest: LoginRequest): ResponseEntity<JWTTokenResponse> {
        if (!loginRequest.idToken.startsWith("Bearer ")) {
            throw UnauthorizedException("Invalid id token. Expecting a Bearer token")
        }
        val idToken = loginRequest.idToken.substring(7)
        val decodedToken = authService.generateJwtUsingIdToken(idToken)
        return ResponseEntity.ok(JWTTokenResponse(token = decodedToken))
    }
}

data class LoginRequest(val idToken: String)