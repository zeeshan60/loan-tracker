package com.zeenom.loan_tracker.configurations

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.zeenom.loan_tracker.properties.AuthProperties
import io.r2dbc.spi.ConnectionFactory
import org.flywaydb.core.Flyway
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DriverManagerDataSource
import reactor.core.publisher.Mono
import java.io.FileInputStream
import javax.sql.DataSource


@Configuration
class FirebaseConfig {

    @Bean
    fun firebaseApp(authProperties: AuthProperties): FirebaseApp {
        if (FirebaseApp.getApps().isEmpty()) {
            val serviceAccount = FileInputStream(authProperties.firebaseSecretJson)

            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build()

            return FirebaseApp.initializeApp(options)
        }
        return FirebaseApp.getInstance()
    }

    @Bean
    fun firebaseAuth(firebaseApp: FirebaseApp): FirebaseAuth = FirebaseAuth.getInstance(firebaseApp)
}

//@Configuration
//class FlywayConfig {
//
//    @Bean
//    fun flyway(): Mono<Void> {
//        return Mono.fromCallable {
//            Flyway.configure()
//                .dataSource(
//                    "jdbc:postgresql://localhost:5432/postgres",
//                    "postgres",
//                    "postgres"
//                ) // Converts ConnectionFactory to DataSource
//                .locations("classpath:db/migration")
//                .baselineOnMigrate(true)
//                .load()
//                .migrate()
//        }.then() // Executes migration and completes
//    }

//    @Bean
//    fun dataSource(): DriverManagerDataSource {
//        val dataSource = DriverManagerDataSource()
//        dataSource.setDriverClassName("org.postgresql.Driver")
//        dataSource.url = "jdbc:postgresql://localhost:5432/postgres"
//        dataSource.username = "postgres"
//        dataSource.password = "postgres"
//        return dataSource
//    }
//}