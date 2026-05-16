package com.flowdroid.launcher.data.db

import androidx.room.*
import com.flowdroid.launcher.data.models.*
import kotlinx.coroutines.flow.Flow

// ─── DAOs ──────────────────────────────────────────────────────────────────────

@Dao
interface AppDao {
    @Query("SELECT * FROM apps WHERE isHidden = 0 AND folderId IS NULL ORDER BY sortOrder, label")
    fun getRootApps(): Flow<List<AppEntry>>

    @Query("SELECT * FROM apps WHERE isHidden = 0 AND folderId = :folderId ORDER BY sortOrder, label")
    fun getAppsInFolder(folderId: Long): Flow<List<AppEntry>>

    @Query("SELECT * FROM apps WHERE isHidden = 1 ORDER BY label")
    fun getHiddenApps(): Flow<List<AppEntry>>

    @Query("SELECT * FROM apps WHERE label LIKE '%' || :query || '%' OR customLabel LIKE '%' || :query || '%'")
    fun searchApps(query: String): Flow<List<AppEntry>>

    @Query("SELECT * FROM apps WHERE packageName = :pkg LIMIT 1")
    suspend fun getApp(pkg: String): AppEntry?

    @Upsert
    suspend fun upsertApp(app: AppEntry)

    @Upsert
    suspend fun upsertApps(apps: List<AppEntry>)

    @Delete
    suspend fun deleteApp(app: AppEntry)

    @Query("DELETE FROM apps WHERE packageName NOT IN (:installedPackages)")
    suspend fun removeUninstalledApps(installedPackages: List<String>)
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE isHidden = 0 ORDER BY sortOrder, name")
    fun getFolders(): Flow<List<AppFolder>>

    @Query("SELECT * FROM folders ORDER BY sortOrder, name")
    fun getAllFolders(): Flow<List<AppFolder>>

    @Query("SELECT * FROM folders WHERE name LIKE '%' || :query || '%'")
    fun searchFolders(query: String): Flow<List<AppFolder>>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getFolder(id: Long): AppFolder?

    @Upsert
    suspend fun upsertFolder(folder: AppFolder): Long

    @Delete
    suspend fun deleteFolder(folder: AppFolder)
}

@Dao
interface ContainerDao {
    @Query("SELECT * FROM containers ORDER BY name")
    fun getContainers(): Flow<List<WinlatorContainer>>

    @Query("SELECT * FROM containers WHERE name LIKE '%' || :query || '%'")
    fun searchContainers(query: String): Flow<List<WinlatorContainer>>

    @Query("SELECT * FROM containers WHERE id = :id LIMIT 1")
    suspend fun getContainer(id: Int): WinlatorContainer?

    @Upsert
    suspend fun upsertContainer(container: WinlatorContainer)

    @Delete
    suspend fun deleteContainer(container: WinlatorContainer)
}

// ─── Database ─────────────────────────────────────────────────────────────────

@Database(
    entities = [AppEntry::class, AppFolder::class, WinlatorContainer::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class FlowDroidDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun folderDao(): FolderDao
    abstract fun containerDao(): ContainerDao
}

class Converters {
    @TypeConverter fun fromGraphicsDriver(v: GraphicsDriver): String = v.name
    @TypeConverter fun toGraphicsDriver(v: String): GraphicsDriver = GraphicsDriver.valueOf(v)
    @TypeConverter fun fromAudioDriver(v: AudioDriver): String = v.name
    @TypeConverter fun toAudioDriver(v: String): AudioDriver = AudioDriver.valueOf(v)
    @TypeConverter fun fromDxWrapper(v: DxWrapper): String = v.name
    @TypeConverter fun toDxWrapper(v: String): DxWrapper = DxWrapper.valueOf(v)
    @TypeConverter fun fromWinVersion(v: WinVersion): String = v.name
    @TypeConverter fun toWinVersion(v: String): WinVersion = WinVersion.valueOf(v)
}
