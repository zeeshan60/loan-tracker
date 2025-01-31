package com.zeenom.loan_tracker.services

import com.google.firebase.auth.FirebaseAuth
import com.zeenom.loan_tracker.properties.AuthProperties
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.*
import javax.crypto.spec.SecretKeySpec

data class JWTToken(val token: String)

@Service
class FirebaseService(private val firebaseAuth: FirebaseAuth, private val authProperties: AuthProperties) {
    fun generateJwtUsingIdToken(idToken: String): Mono<JWTToken> {
        return Mono.fromCallable {
            firebaseAuth.verifyIdToken(idToken)
        }.subscribeOn(Schedulers.boundedElastic()).map { decodedToken ->
            decodedToken.uid
        }
            .map { decodedToken ->
                JWTToken(token = generateJwt(decodedToken))
            }
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

