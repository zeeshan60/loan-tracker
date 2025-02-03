package com.zeenom.loan_tracker.firebase

import com.google.api.core.ApiFuture
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken
import com.zeenom.loan_tracker.users.UserDao
import com.zeenom.loan_tracker.users.UserDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Service
class FirebaseService(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseAdapter: FirebaseAdapter,
    private val userDao: UserDao
) {

    suspend fun verifyIdToken(idToken: String): UserDto = withContext(Dispatchers.IO) {
        val firebaseToken = firebaseAuth.verifyIdTokenAsync(idToken).await()
        val user = firebaseAdapter.tokenToUser(firebaseToken)
        launch { userDao.loginUser(user) }
        user
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
