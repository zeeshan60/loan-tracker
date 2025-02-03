package com.zeenom.loan_tracker.common

interface Query<IN: Any, OUT> {
    suspend fun execute(input: IN): OUT
}