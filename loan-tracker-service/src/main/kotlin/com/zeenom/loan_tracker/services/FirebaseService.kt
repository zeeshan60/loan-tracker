package com.zeenom.loan_tracker.services

import com.google.firebase.auth.FirebaseAuth
import org.springframework.stereotype.Service

@Service
class FirebaseService(private val firebaseAuth: FirebaseAuth) {
    fun verifyIdToken(idToken: String): String {
        return firebaseAuth.verifyIdToken(idToken).uid
    }
}