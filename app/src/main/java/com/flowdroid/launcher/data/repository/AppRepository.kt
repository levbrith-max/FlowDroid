package com.flowdroid.launcher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import com.flowdroid.launcher.data.db.AppDao
import com.flowdroid.launcher.data.db.FolderDao
import com.flowdroid.launcher.data.models.AppEntry
import com.flowdroid.launcher.data.models.AppFolder
import com.flowdroid.launcher.util.HashUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDao: AppDao,
    private val folderDao: FolderDao,
) {
    // ─── Apps ─────────────────────────────────────────────────────────────────

    fun getRootApps(): Flow<List<AppEntry>> = appDao.getRootApps()
    fun getAppsInFolder(folderId: Long): Flow<List<AppEntry>> = appDao.getAppsInFolder(folderId)
    fun getHiddenApps(): Flow<List<AppEntry>> = appDao.getHiddenApps()
    fun searchApps(query: String): Flow<List<AppEntry>> = appDao.searchApps(query)

    suspend fun syncInstalledApps() = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val resolved = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)

        val installed = resolved.map { ri ->
            val pkg = ri.activityInfo.packageName
            val existing = appDao.getApp(pkg)
            existing ?: AppEntry(
                packageName = pkg,
                label = ri.loadLabel(pm).toString(),
            )
        }

        appDao.upsertApps(installed)
        appDao.removeUninstalledApps(installed.map { it.packageName })
    }

    suspend fun setAppHidden(packageName: String, hidden: Boolean) {
        val app = appDao.getApp(packageName) ?: return
        appDao.upsertApp(app.copy(isHidden = hidden))
    }

    suspend fun setAppLocked(packageName: String, locked: Boolean, pin: String? = null) {
        val app = appDao.getApp(packageName) ?: return
        appDao.upsertApp(app.copy(
            isLocked = locked,
            lockPin = if (locked && pin != null) HashUtil.sha256(pin) else null
        ))
    }

    suspend fun verifyPin(packageName: String, pin: String): Boolean {
        val app = appDao.getApp(packageName) ?: return false
        return app.lockPin == HashUtil.sha256(pin)
    }

    suspend fun moveAppToFolder(packageName: String, folderId: Long?) {
        val app = appDao.getApp(packageName) ?: return
        appDao.upsertApp(app.copy(folderId = folderId))
    }

    fun launchApp(packageName: String) {
        context.packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.let { context.startActivity(it) }
    }

    fun getAppIcon(packageName: String): Drawable? = try {
        context.packageManager.getApplicationIcon(packageName)
    } catch (e: PackageManager.NameNotFoundException) { null }

    // ─── Folders ──────────────────────────────────────────────────────────────

    fun getFolders(): Flow<List<AppFolder>> = folderDao.getFolders()
    fun getAllFolders(): Flow<List<AppFolder>> = folderDao.getAllFolders()
    fun searchFolders(q: String): Flow<List<AppFolder>> = folderDao.searchFolders(q)

    suspend fun createFolder(name: String, colorHex: String = "#5C6BC0", emoji: String = "📁"): Long =
        folderDao.upsertFolder(AppFolder(name = name, colorHex = colorHex, iconEmoji = emoji))

    suspend fun updateFolder(folder: AppFolder) = folderDao.upsertFolder(folder)

    suspend fun setFolderHidden(id: Long, hidden: Boolean) {
        val folder = folderDao.getFolder(id) ?: return
        folderDao.upsertFolder(folder.copy(isHidden = hidden))
    }

    suspend fun setFolderLocked(id: Long, locked: Boolean, pin: String? = null) {
        val folder = folderDao.getFolder(id) ?: return
        folderDao.upsertFolder(folder.copy(
            isLocked = locked,
            lockPin = if (locked && pin != null) HashUtil.sha256(pin) else null,
        ))
    }

    suspend fun deleteFolder(id: Long, moveAppsToRoot: Boolean = true) {
        val folder = folderDao.getFolder(id) ?: return
        if (moveAppsToRoot) {
            appDao.getAppsInFolder(id).collect { apps ->
                apps.forEach { appDao.upsertApp(it.copy(folderId = null)) }
            }
        }
        folderDao.deleteFolder(folder)
    }
}
