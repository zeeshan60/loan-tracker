package com.zeenom.loan_tracker.dtos

import java.math.BigDecimal

data class LoanAmountDto(val amount: BigDecimal, val isOwed: Boolean)