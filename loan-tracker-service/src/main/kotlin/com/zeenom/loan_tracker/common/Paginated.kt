package com.zeenom.loan_tracker.common

data class Paginated<T>(
    val data: T,
    val next: String?
)

fun <T> T.toPaginated(next: String? = null) = Paginated(this, next)