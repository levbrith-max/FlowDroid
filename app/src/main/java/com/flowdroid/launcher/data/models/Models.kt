package com.flowdroid.launcher.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

// ─── App Entry ────────────────────────────────────────────────────────────────

@Entity(tableName = "apps")
data class AppEntry(
    @PrimaryKey val packageName: String,
    val label: String,
    val folderId: Long? = null,       // null = root
    val isHidden: Boolean = false,
    val isLocked: Boolean = false,
    val lockPin: String? = null,      // SHA-256 hashed PIN
    val sortOrder: Int = 0,
    val customLabel: String? = null,
)

// ─── Folder ───────────────────────────────────────────────────────────────────

@Entity(tableName = "folders")
data class AppFolder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#5C6BC0",
    val isHidden: Boolean = false,
    val isLocked: Boolean = false,
    val lockPin: String? = null,
    val sortOrder: Int = 0,
    val iconEmoji: String = "📁",
)

// ─── Winlator Container ───────────────────────────────────────────────────────

@Entity(tableName = "containers")
data class WinlatorContainer(
    @PrimaryKey val id: Int,
    val name: String,
    val screenSize: String = "1280x720",          // WxH
    val screenDpi: Int = 96,
    val cpuCount: Int = 4,
    val ramMb: Int = 2048,
    val graphicsDriver: GraphicsDriver = GraphicsDriver.TURNIP,
    val audioDriver: AudioDriver = AudioDriver.PULSEAUDIO,
    val dxwrapper: DxWrapper = DxWrapper.DXVK,
    val dxwrapperConfig: String = "",
    val winComponents: String = "",               // comma-separated
    val desktopTheme: String = "Light",
    val startupApp: String = "",
    val envVars: String = "",
    val drives: String = "",
    val isBoxed86: Boolean = true,
    val winVersion: WinVersion = WinVersion.WIN10,
    val containerPath: String = "",               // absolute path on device
    val notes: String = "",
)

enum class GraphicsDriver(val label: String) {
    TURNIP("Turnip (Adreno)"),
    VIRGL("VirGL"),
    SOFTPIPE("Software (Softpipe)"),
    D8VK("D8VK"),
}

enum class AudioDriver(val label: String) {
    PULSEAUDIO("PulseAudio"),
    ALSA("ALSA (disabled)"),
}

enum class DxWrapper(val label: String) {
    DXVK("DXVK"),
    WGL("WGL (No wrapper)"),
    CNATIVE("CNative"),
}

enum class WinVersion(val label: String, val regValue: String) {
    WIN7("Windows 7", "win7"),
    WIN8("Windows 8", "win8"),
    WIN81("Windows 8.1", "win81"),
    WIN10("Windows 10", "win10"),
    WIN11("Windows 11", "win11"),
}

// ─── Search Result ────────────────────────────────────────────────────────────

sealed class SearchResult {
    data class App(val entry: AppEntry) : SearchResult()
    data class Folder(val folder: AppFolder) : SearchResult()
    data class Container(val container: WinlatorContainer) : SearchResult()
}
