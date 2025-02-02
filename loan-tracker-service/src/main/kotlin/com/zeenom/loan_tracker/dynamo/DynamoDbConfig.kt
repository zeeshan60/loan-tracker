package com.zeenom.loan_tracker.dynamo

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import java.net.URI

@Configuration
class DynamoDbConfig {
    @Bean
    @Profile("local")
    fun dynamoDbAsyncClient(): DynamoDbAsyncClient {
        return DynamoDbAsyncClient.builder()
            .endpointOverride(URI.create("http://localhost:8000"))
            .region(Region.AP_SOUTHEAST_1)
            .credentialsProvider { AwsBasicCredentials.create("testkey", "testsecret") }
            .build()
    }

    @Bean
    @Profile("!local")
    fun dynamoDbAsyncClientProd(): DynamoDbAsyncClient {
        return DynamoDbAsyncClient.builder()
            .region(Region.AP_SOUTHEAST_1)
            .build()
    }
}
