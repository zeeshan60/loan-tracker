package com.zeenom.loan_tracker.firebase

import com.google.api.core.ApiFuture
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken
import com.zeenom.loan_tracker.events.CommandEventService
import com.zeenom.loan_tracker.events.EventDto
import com.zeenom.loan_tracker.events.EventSource
import com.zeenom.loan_tracker.events.EventType
import com.zeenom.loan_tracker.users.UserDto
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Service
class FirebaseService(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseAdapter: FirebaseAdapter,
    private val commandEventService: CommandEventService
) {

    val logger = LoggerFactory.getLogger(FirebaseService::class.java)
    suspend fun verifyIdToken(idToken: String) {
        logger.info("Verifying id token")
        val user = getUserByVerifyingToken(idToken)
        logger.info("Token verified: {}", user)
        commandEventService.execute(
            EventDto(
                event = EventType.LOGIN,
                payload = user,
                userId = user.uid,
                source = EventSource.INDIRECT,
                eventId = UUID.randomUUID().toString()
            )
        )
        logger.info("User logged in: {}", user)
    }

    suspend fun getUserByVerifyingToken(idToken: String): UserDto {
        val firebaseToken = firebaseAuth.verifyIdTokenAsync(idToken).await()
        val user = firebaseAdapter.tokenToUser(firebaseToken)
        return user
    }
}

suspend fun <T> ApiFuture<T>.await(): T {
    return this.toMono().awaitSingle()
}

fun <T> ApiFuture<T>.toMono(): Mono<T> {
    return Mono.fromFuture { this.toCompletableFuture() }
}

fun <T> ApiFuture<T>.toCompletableFuture(): CompletableFuture<T> {
    val completableFuture = CompletableFuture<T>()
    this.addListener(
        {
            try {
                completableFuture.complete(this.get())
            } catch (e: Exception) {
                completableFuture.completeExceptionally(e)
            }
        },
        Executors.newSingleThreadExecutor()
    )
    return completableFuture
}

@Component
class FirebaseAdapter {
    fun tokenToUser(token: FirebaseToken): UserDto {
        return UserDto(
            uid = token.uid,
            email = token.email,
            displayName = token.name,
            photoUrl = token.picture,
            emailVerified = token.isEmailVerified
        )
    }
}
