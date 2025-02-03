package com.zeenom.loan_tracker.common

import java.math.BigDecimal

data class LoanAmountDto(val amount: BigDecimal, val isOwed: Boolean)