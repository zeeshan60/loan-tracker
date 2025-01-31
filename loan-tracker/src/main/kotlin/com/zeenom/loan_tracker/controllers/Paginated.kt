package com.zeenom.loan_tracker.controllers

data class Paginated<T> (
    val data: T,
    val next: String?
)