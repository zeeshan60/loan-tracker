package com.zeenom.loan_tracker.common

import org.springframework.stereotype.Service


@Service
class LoanAmountResponseAdapter {

    fun LoanAmountDto.toResponse() = LoanAmountResponse(
        amount = this.amount,
        isOwed = this.isOwed
    )

    fun fromDto(loanAmountDto: LoanAmountDto) = loanAmountDto.toResponse()
}