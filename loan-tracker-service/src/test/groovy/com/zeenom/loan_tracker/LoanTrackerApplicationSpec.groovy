package com.zeenom.loan_tracker

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@SpringBootTest
@ContextConfiguration(classes = LoanTrackerApplication)
class LoanTrackerApplicationSpec extends Specification {

    @Autowired
    ApplicationContext applicationContext

    def "contextLoads"() {
        expect:
        applicationContext != null
    }
}