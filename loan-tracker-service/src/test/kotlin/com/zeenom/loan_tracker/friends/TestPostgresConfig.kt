package com.zeenom.loan_tracker.friends

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

abstract class TestPostgresConfig {
//Comment this if you dont want to use Testcontainers for PostgreSQL and in application-test.yml uncomment local configuration
    companion object {
        private val postgresContainer: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:15.1").apply {
            withDatabaseName("test-db")
            withUsername("testuser")
            withPassword("testpass")
            start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") {
                "r2dbc:postgresql://${postgresContainer.host}:${postgresContainer.getMappedPort(5432)}/${postgresContainer.databaseName}"
            }
            registry.add("spring.r2dbc.username") { postgresContainer.username }
            registry.add("spring.r2dbc.password") { postgresContainer.password }

            // Add Flyway properties
            registry.add("spring.flyway.enabled") { true }
            registry.add("spring.flyway.url") {
                "jdbc:postgresql://${postgresContainer.host}:${postgresContainer.getMappedPort(5432)}/${postgresContainer.databaseName}"
            }
            registry.add("spring.flyway.user") { postgresContainer.username }
            registry.add("spring.flyway.password") { postgresContainer.password }
        }
    }
}