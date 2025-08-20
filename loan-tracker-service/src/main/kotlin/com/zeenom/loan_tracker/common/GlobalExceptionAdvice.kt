package com.zeenom.loan_tracker.common

import com.zeenom.loan_tracker.common.exceptions.NotFoundException
import com.zeenom.loan_tracker.common.exceptions.UnauthorizedException
import io.jsonwebtoken.ExpiredJwtException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ServerWebInputException

@RestControllerAdvice
class GlobalExceptionAdvice {

    private val logger = LoggerFactory.getLogger(GlobalExceptionAdvice::class.java)

    @ExceptionHandler(NotFoundException::class)
    suspend fun handleNotFound(ex: NotFoundException): ResponseEntity<ErrorResponse> {
        logger.info("Error occurred", ex)
        val errorResponse = ErrorResponse(ErrorMessage(ex.message ?: "Resource not found"))
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    suspend fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        logger.info("Error occurred", ex)
        val errorResponse = ErrorResponse(ErrorMessage(ex.message ?: "Unable to handle request"))
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(IllegalStateException::class)
    suspend fun handleIllegalState(ex: IllegalStateException): ResponseEntity<ErrorResponse> {

        logger.info("Error occurred", ex)
        val errorResponse = ErrorResponse(ErrorMessage(ex.message ?: "Something went wrong"))
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse)
    }

    @ExceptionHandler(UnauthorizedException::class)
    suspend fun handleUnauthorized(ex: UnauthorizedException): ResponseEntity<ErrorResponse> {
        logger.info("Error occurred", ex)
        val errorResponse = ErrorResponse(ErrorMessage(ex.message ?: "Unauthorized"))
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }

    @ExceptionHandler(ServerWebInputException::class)
    suspend fun handleMissingRequestValue(ex: ServerWebInputException): ResponseEntity<ErrorResponse> {
        logger.info("Error occurred", ex)
        val errorResponse = ErrorResponse(ErrorMessage(ex.message))
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    suspend fun handleException(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Error occurred", ex)
        val errorResponse = ErrorResponse(ErrorMessage(ex.message ?: "Something went wrong"))
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}

data class ErrorResponse(val error: ErrorMessage)
data class ErrorMessage(val message: String)