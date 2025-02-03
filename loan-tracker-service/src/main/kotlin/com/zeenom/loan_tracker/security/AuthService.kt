package com.zeenom.loan_tracker.security

import com.zeenom.loan_tracker.firebase.FirebaseService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.spec.SecretKeySpec

@Service
class AuthService(private val firebaseService: FirebaseService, private val authProperties: AuthProperties) {
    suspend fun generateJwtUsingIdToken(idToken: String): String {
        return generateJwt(verifyIdToken(idToken))
    }

    suspend fun verifyIdToken(idToken: String): String {
        firebaseService.verifyIdToken(idToken)
        return "Token is valid"
    }

    fun generateJwt(uid: String): String {
        val expirationTime = Calendar.getInstance().apply {
            add(Calendar.DATE, authProperties.jwtExpiryDays)
        }.time
        return Jwts.builder()
            .setSubject(uid)
            .setIssuedAt(Date())
            .setExpiration(expirationTime)
            .signWith(
                SecretKeySpec(authProperties.secretKey.toByteArray(), SignatureAlgorithm.HS256.jcaName),
                SignatureAlgorithm.HS256
            )
            .compact()
    }
}
