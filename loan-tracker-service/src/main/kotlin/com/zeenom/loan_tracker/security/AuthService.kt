package com.zeenom.loan_tracker.security

import com.zeenom.loan_tracker.events.LoginUserCommand
import com.zeenom.loan_tracker.events.CommandDto
import com.zeenom.loan_tracker.events.CommandType
import com.zeenom.loan_tracker.firebase.FirebaseService
import com.zeenom.loan_tracker.users.UserService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.spec.SecretKeySpec

@Service
class AuthService(
    private val firebaseService: FirebaseService,
    private val authProperties: AuthProperties,
    private val loginUserCommand: LoginUserCommand,
    private val userService: UserService
) {
    val logger: Logger = LoggerFactory.getLogger(AuthService::class.java)
    suspend fun generateJwtUsingIdToken(idToken: String): String {
        return generateJwt(uidByVerifyingIdToken(idToken))
    }

    suspend fun uidByVerifyingIdToken(idToken: String): String {
        val user = firebaseService.userByVerifyingIdToken(idToken)

        loginUserCommand.execute(
            CommandDto(
                commandType = CommandType.LOGIN,
                payload = user,
                userId = user.uid
            )
        )
        //Do not use the user object from Firebase, it doesnt necessarily have the same uid
        val existing = userService.findByUserEmailOrPhoneNumber(
            email = user.email,
            phoneNumber = user.phoneNumber
        ) ?: run {
            logger.error("User not found in the database: {}", user)
            throw IllegalStateException("User not found")
        }
        logger.info("User logged in: {}", existing)
        return existing.uid
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
