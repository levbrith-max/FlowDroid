package com.flowdroid.launcher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowdroid.launcher.data.models.*
import com.flowdroid.launcher.data.repository.AppRepository
import com.flowdroid.launcher.data.repository.ContainerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val rootApps: List<AppEntry> = emptyList(),
    val folders: List<AppFolder> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val isLoading: Boolean = false,
    val pinDialogTarget: PinTarget? = null,
    val snackbarMessage: String? = null,
)

sealed class PinTarget {
    data class AppLock(val packageName: String) : PinTarget()
    data class FolderLock(val folderId: Long) : PinTarget()
    data class AppUnlock(val packageName: String, val onSuccess: () -> Unit) : PinTarget()
    data class FolderUnlock(val folderId: Long, val onSuccess: () -> Unit) : PinTarget()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appRepo: AppRepository,
    private val containerRepo: ContainerRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _pinDialog = MutableStateFlow<PinTarget?>(null)
    private val _snackbar = MutableStateFlow<String?>(null)
    private val _loading = MutableStateFlow(false)

    @OptIn(FlowPreview::class)
    private val searchResults: Flow<List<SearchResult>> = _query
        .debounce(200)
        .filter { it.isNotBlank() }
        .flatMapLatest { q ->
            combine(
                appRepo.searchApps(q),
                appRepo.searchFolders(q),
                containerRepo.searchContainers(q),
            ) { apps, folders, containers ->
                buildList {
                    addAll(folders.map { SearchResult.Folder(it) })
                    addAll(apps.map { SearchResult.App(it) })
                    addAll(containers.map { SearchResult.Container(it) })
                }
            }
        }

    val uiState: StateFlow<HomeUiState> = combine(
        appRepo.getRootApps(),
        appRepo.getFolders(),
        _query,
        searchResults.onStart { emit(emptyList()) },
        _loading,
        _pinDialog,
        _snackbar,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val rootApps   = values[0] as List<AppEntry>
        val folders    = values[1] as List<AppFolder>
        val query      = values[2] as String
        @Suppress("UNCHECKED_CAST")
        val results    = values[3] as List<SearchResult>
        val loading    = values[4] as Boolean
        val pinDialog  = values[5] as PinTarget?
        val snackbar   = values[6] as String?
        HomeUiState(
            rootApps      = rootApps,
            folders       = folders,
            searchQuery   = query,
            searchResults = results,
            isSearching   = query.isNotBlank(),
            isLoading     = loading,
            pinDialogTarget = pinDialog,
            snackbarMessage = snackbar,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(isLoading = true))

    init {
        viewModelScope.launch {
            _loading.value = true
            appRepo.syncInstalledApps()
            containerRepo.syncFromDisk()
            _loading.value = false
        }
    }

    fun onSearchQuery(q: String) { _query.value = q }
    fun clearSearch() { _query.value = "" }

    // ─── App actions ──────────────────────────────────────────────────────────

    fun launchApp(packageName: String) {
        viewModelScope.launch {
            val app = appRepo.getHiddenApps() // just to trigger any needed check – real launch below
            appRepo.launchApp(packageName)
        }
    }

    fun requestUnlockApp(packageName: String, onSuccess: () -> Unit) {
        _pinDialog.value = PinTarget.AppUnlock(packageName, onSuccess)
    }

    fun setAppHidden(packageName: String, hidden: Boolean) {
        viewModelScope.launch {
            appRepo.setAppHidden(packageName, hidden)
            _snackbar.value = if (hidden) "App masquée" else "App visible"
        }
    }

    fun requestLockApp(packageName: String) {
        _pinDialog.value = PinTarget.AppLock(packageName)
    }

    fun confirmLockApp(packageName: String, pin: String) {
        viewModelScope.launch {
            appRepo.setAppLocked(packageName, true, pin)
            _pinDialog.value = null
            _snackbar.value = "App verrouillée"
        }
    }

    fun unlockApp(packageName: String, pin: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (appRepo.verifyPin(packageName, pin)) {
                _pinDialog.value = null
                onSuccess()
            } else {
                _snackbar.value = "PIN incorrect"
            }
        }
    }

    fun removeAppLock(packageName: String) {
        viewModelScope.launch {
            appRepo.setAppLocked(packageName, false)
            _snackbar.value = "Verrouillage retiré"
        }
    }

    fun moveAppToFolder(packageName: String, folderId: Long?) {
        viewModelScope.launch { appRepo.moveAppToFolder(packageName, folderId) }
    }

    // ─── Folder actions ───────────────────────────────────────────────────────

    fun createFolder(name: String, colorHex: String, emoji: String) {
        viewModelScope.launch { appRepo.createFolder(name, colorHex, emoji) }
    }

    fun setFolderHidden(id: Long, hidden: Boolean) {
        viewModelScope.launch {
            appRepo.setFolderHidden(id, hidden)
            _snackbar.value = if (hidden) "Dossier masqué" else "Dossier visible"
        }
    }

    fun requestLockFolder(id: Long) { _pinDialog.value = PinTarget.FolderLock(id) }

    fun confirmLockFolder(id: Long, pin: String) {
        viewModelScope.launch {
            appRepo.setFolderLocked(id, true, pin)
            _pinDialog.value = null
            _snackbar.value = "Dossier verrouillé"
        }
    }

    fun requestUnlockFolder(id: Long, onSuccess: () -> Unit) {
        _pinDialog.value = PinTarget.FolderUnlock(id, onSuccess)
    }

    fun deleteFolder(id: Long) {
        viewModelScope.launch {
            appRepo.deleteFolder(id, moveAppsToRoot = true)
            _snackbar.value = "Dossier supprimé"
        }
    }

    fun dismissPinDialog() { _pinDialog.value = null }
    fun clearSnackbar() { _snackbar.value = null }
}
