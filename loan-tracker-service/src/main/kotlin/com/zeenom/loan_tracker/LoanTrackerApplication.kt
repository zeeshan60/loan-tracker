package com.zeenom.loan_tracker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
class LoanTrackerApplication

fun main(args: Array<String>) {
	runApplication<LoanTrackerApplication>(*args)
}


