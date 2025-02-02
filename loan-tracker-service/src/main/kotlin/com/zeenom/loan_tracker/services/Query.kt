package com.zeenom.loan_tracker.services

interface Query<IN: Any, OUT> {
    suspend fun execute(input: IN): OUT
}