package com.flowdroid.launcher.data.winlator

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.flowdroid.launcher.data.models.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles all interaction with Winlator & Ludashi installed on the device.
 *
 * Winlator package : com.winlator
 * Ludashi package  : com.ludashi.benchmark  (or com.ludashi.gameturbo)
 *
 * Containers are stored at:
 *   /data/data/com.winlator/files/containers/<id>/  (requires root or shared UID)
 * Alternatively via the DocumentsProvider URI:
 *   content://com.winlator.fileprovider/...
 *
 * For devices without root we rely on the public shared storage copy that
 * Winlator exports to:
 *   /sdcard/Android/data/com.winlator/files/containers/
 */
@Singleton
class WinlatorIntegration @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "WinlatorIntegration"

        // Package names
        const val PKG_WINLATOR = "com.winlator"
        const val PKG_WINLATOR_CMOD = "com.winlator.cmod"
        const val PKG_LUDASHI = "com.ludashi.benchmark"

        // Intent actions published by Winlator >= 7.x
        const val ACTION_LAUNCH_CONTAINER = "com.winlator.LAUNCH_CONTAINER"
        const val EXTRA_CONTAINER_ID = "container_id"

        // Shared-storage path (accessible without root on Android 11+)
        private const val SHARED_BASE = "Android/data/com.winlator/files/containers"
    }

    // ─── Package detection ────────────────────────────────────────────────────

    val winlatorPackage: String?
        get() = listOf(PKG_WINLATOR, PKG_WINLATOR_CMOD).firstOrNull { isInstalled(it) }

    val ludashiPackage: String?
        get() = if (isInstalled(PKG_LUDASHI)) PKG_LUDASHI else null

    fun isWinlatorInstalled() = winlatorPackage != null
    fun isLudashiInstalled() = ludashiPackage != null

    private fun isInstalled(pkg: String): Boolean = try {
        context.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    // ─── Container scanning ───────────────────────────────────────────────────

    /**
     * Scans the shared-storage path for Winlator containers.
     * Returns a list of partially-populated [WinlatorContainer] objects.
     * The caller should merge these with any extra metadata stored in Room.
     */
    suspend fun scanContainers(): List<WinlatorContainer> = withContext(Dispatchers.IO) {
        val results = mutableListOf<WinlatorContainer>()
        val pkg = winlatorPackage ?: return@withContext results

        val bases = listOf(
            File(context.getExternalFilesDir(null)?.parentFile?.parentFile, "$SHARED_BASE"),
            File("/sdcard/$SHARED_BASE"),
            File("/storage/emulated/0/$SHARED_BASE"),
        )

        for (base in bases) {
            if (!base.exists()) continue
            base.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
                try {
                    val id = dir.name.toIntOrNull() ?: return@forEach
                    val props = readPropertiesFile(File(dir, "Properties"))
                    results += WinlatorContainer(
                        id = id,
                        name = props["name"] ?: "Container $id",
                        screenSize = props["screenSize"] ?: "1280x720",
                        screenDpi = props["screenDpi"]?.toIntOrNull() ?: 96,
                        cpuCount = props["cpuCount"]?.toIntOrNull() ?: 4,
                        ramMb = props["ramSize"]?.toIntOrNull() ?: 2048,
                        graphicsDriver = props["graphicsDriver"]?.let {
                            runCatching { GraphicsDriver.valueOf(it.uppercase()) }.getOrNull()
                        } ?: GraphicsDriver.TURNIP,
                        audioDriver = props["audioDriver"]?.let {
                            runCatching { AudioDriver.valueOf(it.uppercase()) }.getOrNull()
                        } ?: AudioDriver.PULSEAUDIO,
                        dxwrapper = props["dxwrapper"]?.let {
                            runCatching { DxWrapper.valueOf(it.uppercase()) }.getOrNull()
                        } ?: DxWrapper.DXVK,
                        winVersion = props["winVersion"]?.let { ver ->
                            WinVersion.entries.find { it.regValue == ver }
                        } ?: WinVersion.WIN10,
                        containerPath = dir.absolutePath,
                        envVars = props["envVars"] ?: "",
                        drives = props["drives"] ?: "",
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse container ${dir.name}", e)
                }
            }
            if (results.isNotEmpty()) break
        }
        results
    }

    /**
     * Writes modified [WinlatorContainer] properties back to the Properties file.
     * Requires the container path to be accessible (external storage).
     */
    suspend fun saveContainer(container: WinlatorContainer): Boolean = withContext(Dispatchers.IO) {
        try {
            val propsFile = File(container.containerPath, "Properties")
            if (!propsFile.exists()) return@withContext false

            val current = readPropertiesFile(propsFile).toMutableMap()
            current["name"] = container.name
            current["screenSize"] = container.screenSize
            current["screenDpi"] = container.screenDpi.toString()
            current["cpuCount"] = container.cpuCount.toString()
            current["ramSize"] = container.ramMb.toString()
            current["graphicsDriver"] = container.graphicsDriver.name.lowercase()
            current["audioDriver"] = container.audioDriver.name.lowercase()
            current["dxwrapper"] = container.dxwrapper.name.lowercase()
            current["winVersion"] = container.winVersion.regValue
            current["envVars"] = container.envVars
            current["drives"] = container.drives

            propsFile.writeText(current.entries.joinToString("\n") { "${it.key}=${it.value}" })
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save container ${container.id}", e)
            false
        }
    }

    // ─── Launch ───────────────────────────────────────────────────────────────

    /** Launches the given container ID in Winlator via explicit Intent. */
    fun launchContainer(containerId: Int) {
        val pkg = winlatorPackage ?: return
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
            putExtra(EXTRA_CONTAINER_ID, containerId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } ?: return
        context.startActivity(intent)
    }

    /** Opens Winlator at its main screen (no specific container). */
    fun openWinlator() {
        val pkg = winlatorPackage ?: return
        context.packageManager.getLaunchIntentForPackage(pkg)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.let { context.startActivity(it) }
    }

    /** Opens Ludashi's main screen. */
    fun openLudashi() {
        val pkg = ludashiPackage ?: return
        context.packageManager.getLaunchIntentForPackage(pkg)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.let { context.startActivity(it) }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun readPropertiesFile(file: File): Map<String, String> {
        if (!file.exists()) return emptyMap()
        return file.readLines()
            .filter { it.contains('=') }
            .associate { line ->
                val idx = line.indexOf('=')
                line.substring(0, idx).trim() to line.substring(idx + 1).trim()
            }
    }
}
