package com.zeenom.loan_tracker.services

import com.google.api.core.ApiFuture
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Service
class FirebaseService(private val firebaseAuth: FirebaseAuth, private val firebaseAdapter: FirebaseAdapter) {

    suspend fun verifyIdToken(idToken: String): FirebaseUser {
        val firebaseToken = firebaseAuth.verifyIdTokenAsync(idToken).await()
        return firebaseAdapter.tokenToUser(firebaseToken)
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
    fun tokenToUser(token: FirebaseToken): FirebaseUser {
        return FirebaseUser(
            uid = token.uid,
            email = token.email,
            displayName = token.name,
            photoUrl = token.picture
        )
    }
}

data class FirebaseUser(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String,
)
