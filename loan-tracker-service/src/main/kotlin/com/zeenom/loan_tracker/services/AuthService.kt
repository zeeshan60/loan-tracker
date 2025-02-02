package com.zeenom.loan_tracker.services

import com.zeenom.loan_tracker.properties.AuthProperties
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

    fun verifyIdToken(idToken: String): String {
        return firebaseService.verifyIdToken(idToken)
    }

    private fun generateJwt(uid: String): String {
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
