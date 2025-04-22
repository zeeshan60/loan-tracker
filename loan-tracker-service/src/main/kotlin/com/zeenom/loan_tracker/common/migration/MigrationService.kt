package com.zeenom.loan_tracker.common.migration;

import com.zeenom.loan_tracker.friends.FriendEvent
import com.zeenom.loan_tracker.friends.FriendEventRepository
import com.zeenom.loan_tracker.friends.FriendModel
import com.zeenom.loan_tracker.friends.FriendModelRepository
import com.zeenom.loan_tracker.users.UserEventRepository
import com.zeenom.loan_tracker.users.UserModelRepository
import com.zeenom.loan_tracker.users.userModels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.time.Instant

@Component
@Profile("!test")
class ApplicationStartupListener(
    val migrationService: MigrationService
) : ApplicationListener<ApplicationReadyEvent> {

    val logger = LoggerFactory.getLogger(ApplicationStartupListener::class.java)

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        CoroutineScope(Dispatchers.IO).launch { migrationService.migrateData() }
    }
}

@Service
class MigrationService(
    private val migrationStatusRepository: MigrationStatusRepository,
    private val userEventRepository: UserEventRepository,
    private val friendEventRepository: FriendEventRepository,
    private val userModelRepository: UserModelRepository,
    private val friendModelRepository: FriendModelRepository
) {
    val logger = LoggerFactory.getLogger(MigrationService::class.java)

    val currentVersion = 2

    suspend fun migrateData() {
        val lastSuccessfulMigrationVersion =
            migrationStatusRepository.findLastFinishedByOrderByVersionDesc()?.version ?: 0
        (lastSuccessfulMigrationVersion + 1..currentVersion).forEach {
            migrate(it)
        }
    }

    private suspend fun migrate(version: Int) {
        val findByVersion = migrationStatusRepository.findByVersion(version)
        val currentVersion = findByVersion ?: migrationStatusRepository.save(
            MigrationStatus(
                version = version,
                createdAt = Instant.now()
            )
        )

        when (version) {
            0 -> {}
            1 -> {}
            2 -> migrateV2()
            else -> throw IllegalStateException("Migration not found for version $version")
        }
        migrationStatusRepository.save(
            currentVersion.copy(
                finishedAt = Instant.now()
            )
        )
        logger.info("Migration done for version $version")
    }

    private suspend fun migrateV2() {
        logger.info("Migrating to version 2.0")
        val userEvents = userEventRepository.findAll().toList()
        userModelRepository.saveAll(userModels(userEvents)).toList()

        val friendEvents = friendEventRepository.findAll().toList()
        friendModelRepository.saveAll(friendModels(friendEvents)).toList()
    }

    fun friendModels(eventStreams: List<FriendEvent>) =
        eventStreams.groupBy { it.streamId }.map { (_, events) ->
            friendModel(events) ?: throw IllegalStateException("Friend model not found")
        }

    fun friendModel(eventStream: List<FriendEvent>) =
        eventStream.map { it.toEvent() }.sortedBy { it.version }
            .fold(null as FriendModel?) { model, event ->
                event.applyEvent(model)
            }
}