package com.zeenom.loan_tracker.dtos

data class Paginated<T> (
    val data: T,
    val next: String?
)