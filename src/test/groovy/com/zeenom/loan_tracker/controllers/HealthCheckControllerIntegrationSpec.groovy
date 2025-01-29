package com.zeenom.loan_tracker.controllers

import com.zeenom.loan_tracker.LoanTrackerApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import spock.lang.Specification

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = LoanTrackerApplication)
class HealthCheckControllerIntegrationSpec extends Specification {

    @Autowired
    private WebTestClient webTestClient

    def "health check endpoint returns alive message"() {
        expect:
        webTestClient.get().uri("/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.message').isEqualTo("I'm alive!")
    }
}

