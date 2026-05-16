package com.flowdroid.launcher.data.repository

import com.flowdroid.launcher.data.db.ContainerDao
import com.flowdroid.launcher.data.models.WinlatorContainer
import com.flowdroid.launcher.data.winlator.WinlatorIntegration
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContainerRepository @Inject constructor(
    private val containerDao: ContainerDao,
    private val winlator: WinlatorIntegration,
) {
    fun getContainers(): Flow<List<WinlatorContainer>> = containerDao.getContainers()
    fun searchContainers(q: String): Flow<List<WinlatorContainer>> = containerDao.searchContainers(q)

    suspend fun syncFromDisk() {
        val found = winlator.scanContainers()
        found.forEach { containerDao.upsertContainer(it) }
    }

    suspend fun saveContainer(container: WinlatorContainer): Boolean {
        containerDao.upsertContainer(container)
        return winlator.saveContainer(container)
    }

    suspend fun deleteContainer(container: WinlatorContainer) =
        containerDao.deleteContainer(container)

    fun launchContainer(id: Int) = winlator.launchContainer(id)
    fun openWinlator()           = winlator.openWinlator()
    fun openLudashi()            = winlator.openLudashi()

    fun isWinlatorInstalled() = winlator.isWinlatorInstalled()
    fun isLudashiInstalled()  = winlator.isLudashiInstalled()
}
