package com.zeenom.loan_tracker.common

import com.zeenom.loan_tracker.transactions.AmountDto
import org.springframework.stereotype.Service


@Service
class LoanAmountResponseAdapter {

    fun AmountDto.toResponse() = LoanAmountResponse(
        amount = this.amount,
        isOwed = this.isOwed
    )

    fun fromDto(loanAmountDto: AmountDto) = loanAmountDto.toResponse()
}