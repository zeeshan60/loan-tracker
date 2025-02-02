package com.zeenom.loan_tracker.services

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

@Service
class FirebaseService(private val firebaseAuth: FirebaseAuth) {

    suspend fun verifyIdToken(idToken: String): String {
        return withContext(Dispatchers.IO) {
            firebaseAuth.verifyIdToken(idToken).uid
        }
    }
}
