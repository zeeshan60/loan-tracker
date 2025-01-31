package com.zeenom.loan_tracker.services

interface Query<IN, OUT> {
    fun execute(input: IN): OUT
}