package com.zeenom.loan_tracker.common

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class RequestLoggingFilter : WebFilter {
    val logger = LoggerFactory.getLogger(RequestLoggingFilter::class.java)
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        return chain.filter(exchange)
            .doOnTerminate {
                val method = exchange.request.method
                val uri = exchange.request.uri.toString()
                val status = exchange.response.statusCode?.value() ?: 0
                logger.info("Request: $method $uri, Status: $status")
            }
    }
}