package com.autoswipe.ai.studio

import android.app.Application
import com.autoswipe.ai.studio.data.SettingsRepository
import com.autoswipe.ai.studio.data.StatsDatabase
import com.autoswipe.ai.studio.data.StatsRepository
import com.autoswipe.ai.studio.service.EngineCoordinator
import com.autoswipe.ai.studio.service.FloatingControlManager

class AutoSwipeApplication : Application() {

    lateinit var coordinator: EngineCoordinator
        private set

    lateinit var floatingControlManager: FloatingControlManager
        private set

    override fun onCreate() {
        super.onCreate()
        val settingsRepo = SettingsRepository(this)
        val statsDb = StatsDatabase.getInstance(this)
        val statsRepo = StatsRepository(statsDb.statsDao())

        coordinator = EngineCoordinator(
            context = this,
            settingsRepository = settingsRepo,
            statsRepository = statsRepo
        )
        floatingControlManager = FloatingControlManager(this, coordinator)
        coordinator.attachFloatingManager(floatingControlManager)
    }
}
