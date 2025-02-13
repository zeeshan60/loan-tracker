package com.zeenom.loan_tracker.common.exceptions

class NotFoundException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class UnauthorizedException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)