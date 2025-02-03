package com.zeenom.loan_tracker.common

import java.math.BigDecimal

data class LoanAmountResponse(val amount: BigDecimal, val isOwed: Boolean)