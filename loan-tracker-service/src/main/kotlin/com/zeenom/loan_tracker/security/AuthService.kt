package com.zeenom.loan_tracker.security

import com.zeenom.loan_tracker.events.CommandCreateUser
import com.zeenom.loan_tracker.events.EventDto
import com.zeenom.loan_tracker.events.EventType
import com.zeenom.loan_tracker.firebase.FirebaseService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.spec.SecretKeySpec

@Service
class AuthService(
    private val firebaseService: FirebaseService,
    private val authProperties: AuthProperties,
    private val commandCreateUser: CommandCreateUser
) {
    val logger = LoggerFactory.getLogger(AuthService::class.java)
    suspend fun generateJwtUsingIdToken(idToken: String): String {
        return generateJwt(uidByVerifyingIdToken(idToken))
    }

    suspend fun uidByVerifyingIdToken(idToken: String): String {
        val user = firebaseService.userByVerifyingIdToken(idToken)

        commandCreateUser.execute(
            EventDto(
                event = EventType.LOGIN,
                payload = user,
                userId = user.uid
            )
        )
        logger.info("User logged in: {}", user)
        return user.uid
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
