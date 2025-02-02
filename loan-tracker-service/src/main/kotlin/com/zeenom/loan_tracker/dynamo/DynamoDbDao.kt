package com.zeenom.loan_tracker.dynamo

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model.*

@Repository
class DynamoDbDao(private val dynamoDbClient: DynamoDbAsyncClient) {

    suspend fun listTables(): List<String>? {
        val request = ListTablesRequest.builder().build()

        return Mono.fromFuture { dynamoDbClient.listTables(request) }.flatMapMany { response: ListTablesResponse ->
                Flux.fromIterable(response.tableNames())
            }.collectList().awaitSingleOrNull()
    }

    suspend fun createTable(): String {
        val request = CreateTableRequest.builder().tableName("events-v0").attributeDefinitions(
                AttributeDefinition.builder().attributeName("userId").attributeType(ScalarAttributeType.S).build(),
                AttributeDefinition.builder().attributeName("eventId").attributeType(ScalarAttributeType.S).build(),
                AttributeDefinition.builder().attributeName("createdAt").attributeType(ScalarAttributeType.S).build(),
                AttributeDefinition.builder().attributeName("eventType").attributeType(ScalarAttributeType.S).build()
            ).keySchema(
                KeySchemaElement.builder().attributeName("userId").keyType(KeyType.HASH).build(),
                KeySchemaElement.builder().attributeName("eventId").keyType(KeyType.RANGE).build()
            ).localSecondaryIndexes(
                LocalSecondaryIndex.builder().indexName("createdAt-index").keySchema(
                        KeySchemaElement.builder().attributeName("userId").keyType(KeyType.HASH).build(),
                        KeySchemaElement.builder().attributeName("createdAt").keyType(KeyType.RANGE).build()
                    ).projection(Projection.builder().projectionType(ProjectionType.ALL).build()).build(),
                LocalSecondaryIndex.builder().indexName("eventType-index").keySchema(
                        KeySchemaElement.builder().attributeName("userId").keyType(KeyType.HASH).build(),
                        KeySchemaElement.builder().attributeName("eventType").keyType(KeyType.RANGE).build()
                    ).projection(Projection.builder().projectionType(ProjectionType.ALL).build()).build()
            ).billingMode(BillingMode.PAY_PER_REQUEST).build()

        return Mono.fromFuture { dynamoDbClient.createTable(request) }.map { "Successfully table created" }
            .awaitSingle()
    }

    suspend fun deleteTable(): String {
        val request = DeleteTableRequest.builder().tableName("events-v0").build()

        return Mono.fromFuture { dynamoDbClient.deleteTable(request) }.map { "Successfully table deleted" }
            .awaitSingle()
    }
}