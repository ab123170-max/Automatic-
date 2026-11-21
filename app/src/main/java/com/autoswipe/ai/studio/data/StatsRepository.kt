package com.autoswipe.ai.studio.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class UsageStatsSummary(
    val totalSwipes: Int = 0,
    val totalSessions: Int = 0,
    val totalActiveTimeMs: Long = 0L,
    val averageVideoIntervalMs: Long = 0L,
    val totalDetectionFailures: Int = 0,
    val totalGestureFailures: Int = 0,
    val perAppStats: List<AppStatsEntity> = emptyList()
)

class StatsRepository(private val dao: StatsDao) {
    private val writeMutex = Mutex()

    val summaryFlow: Flow<UsageStatsSummary> = combine(
        dao.observeGlobalStats(),
        dao.observeAllAppStats()
    ) { global, perApp ->
        val g = global ?: GlobalStatsEntity()
        val avgDelay = if (g.swipeIntervalsCount > 0) {
            g.totalVideoIntervalMs / g.swipeIntervalsCount
        } else {
            0L
        }
        UsageStatsSummary(
            totalSwipes = g.totalSwipes,
            totalSessions = g.totalSessions,
            totalActiveTimeMs = g.totalActiveTimeMs,
            averageVideoIntervalMs = avgDelay,
            totalDetectionFailures = g.totalDetectionFailures,
            totalGestureFailures = g.totalGestureFailures,
            perAppStats = perApp
        )
    }

    suspend fun recordSessionStarted() = writeMutex.withLock {
        val current = dao.getGlobalStats() ?: GlobalStatsEntity()
        dao.upsertGlobalStats(
            current.copy(totalSessions = current.totalSessions + 1)
        )
    }

    suspend fun recordActiveTimeDelta(deltaMs: Long) {
        if (deltaMs <= 0L) return
        writeMutex.withLock {
            val current = dao.getGlobalStats() ?: GlobalStatsEntity()
            dao.upsertGlobalStats(
                current.copy(totalActiveTimeMs = current.totalActiveTimeMs + deltaMs)
            )
        }
    }

    suspend fun recordSuccessfulSwipe(
        appId: String,
        displayName: String,
        videoIntervalMs: Long,
        nowMs: Long
    ) = writeMutex.withLock {
        val safeInterval = videoIntervalMs.coerceAtLeast(0L)
        val global = dao.getGlobalStats() ?: GlobalStatsEntity()
        dao.upsertGlobalStats(
            global.copy(
                totalSwipes = global.totalSwipes + 1,
                totalVideoIntervalMs = global.totalVideoIntervalMs + safeInterval,
                swipeIntervalsCount = global.swipeIntervalsCount + if (safeInterval > 0L) 1 else 0
            )
        )

        val existingApp = dao.getAppStats(appId) ?: AppStatsEntity(
            appId = appId,
            displayName = displayName
        )
        dao.upsertAppStats(
            existingApp.copy(
                displayName = displayName,
                swipeCount = existingApp.swipeCount + 1,
                totalVideoIntervalMs = existingApp.totalVideoIntervalMs + safeInterval,
                lastUpdatedEpochMs = nowMs
            )
        )
    }

    suspend fun recordDetectionTimeout(
        appId: String,
        displayName: String,
        nowMs: Long
    ) = writeMutex.withLock {
        val global = dao.getGlobalStats() ?: GlobalStatsEntity()
        dao.upsertGlobalStats(
            global.copy(totalDetectionFailures = global.totalDetectionFailures + 1)
        )
        val existingApp = dao.getAppStats(appId) ?: AppStatsEntity(
            appId = appId,
            displayName = displayName
        )
        dao.upsertAppStats(
            existingApp.copy(
                displayName = displayName,
                detectionFailureCount = existingApp.detectionFailureCount + 1,
                lastUpdatedEpochMs = nowMs
            )
        )
    }

    suspend fun recordGestureFailure(
        appId: String,
        displayName: String,
        nowMs: Long
    ) = writeMutex.withLock {
        val global = dao.getGlobalStats() ?: GlobalStatsEntity()
        dao.upsertGlobalStats(
            global.copy(totalGestureFailures = global.totalGestureFailures + 1)
        )
        val existingApp = dao.getAppStats(appId) ?: AppStatsEntity(
            appId = appId,
            displayName = displayName
        )
        dao.upsertAppStats(
            existingApp.copy(
                displayName = displayName,
                gestureFailureCount = existingApp.gestureFailureCount + 1,
                lastUpdatedEpochMs = nowMs
            )
        )
    }

    suspend fun clearAll() = writeMutex.withLock {
        dao.clearAppStats()
        dao.clearGlobalStats()
    }
}
