package com.zeenom.loan_tracker.controllers


import com.zeenom.loan_tracker.LoanTrackerApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import spock.lang.Specification

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = LoanTrackerApplication)
class FriendsControllerSpec extends Specification {

    @Autowired
    private WebTestClient webTestClient


    def "friends endpoint returns friends"() {

        expect:
        webTestClient.get().uri("api/v1/friends")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.data.friends').isNotEmpty()
                .jsonPath('$.next').isEqualTo(null)
    }

    def "test cors configuration"() {
        when:
        def response = webTestClient.options()
                .uri("/api/v1/friends")
                .header("Origin", "http://any-origin.com")
                .header("Access-Control-Request-Method", "GET")
                .exchange()

        then:
        response.expectHeader().valueEquals("Access-Control-Allow-Origin", "*")
        response.expectHeader().valueEquals("Access-Control-Allow-Methods", "GET")
        response.expectHeader().exists("Access-Control-Max-Age")
    }
}