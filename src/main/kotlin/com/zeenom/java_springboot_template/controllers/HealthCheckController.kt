package com.zeenom.java_springboot_template.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@RestController
class HealthCheckController {
    @GetMapping("/health")
    fun healthCheck(): Mono<MessageResponse> {
        return MessageResponse(message = "I'm alive!").toMono()
    }
}
