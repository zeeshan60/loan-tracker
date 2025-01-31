package com.zeenom.loan_tracker.controllers

import com.zeenom.loan_tracker.LoanTrackerApplication
import com.zeenom.loan_tracker.properties.AuthProperties
import com.zeenom.loan_tracker.services.FirebaseService
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import spock.lang.Specification

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = LoanTrackerApplication)
class AuthControllerSpec extends Specification {

    @Autowired
    private WebTestClient webTestClient

    @Autowired
    private AuthProperties authProperties

    @MockitoBean
    private FirebaseService firebaseService

    def "given verified id token generates jwt token with expiry successfully"() {
        given:
        def idToken = "verified-id-token"
        Mockito.doReturn("jwt-token").when(firebaseService).verifyIdToken(idToken)

        when:
        def response = webTestClient.post().uri("/login")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $idToken")
                .exchange()

        then:
        response.expectStatus().isOk()
        response.expectBody().jsonPath('$.token').isNotEmpty()
    }
}