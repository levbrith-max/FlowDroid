package com.flowdroid.launcher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowdroid.launcher.data.models.*
import com.flowdroid.launcher.data.repository.ContainerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContainerUiState(
    val containers: List<WinlatorContainer> = emptyList(),
    val editingContainer: WinlatorContainer? = null,
    val isWinlatorInstalled: Boolean = false,
    val isLudashiInstalled: Boolean = false,
    val isSaving: Boolean = false,
    val snackbarMessage: String? = null,
    val showNewContainerDialog: Boolean = false,
)

@HiltViewModel
class ContainerViewModel @Inject constructor(
    private val repo: ContainerRepository,
) : ViewModel() {

    private val _editing = MutableStateFlow<WinlatorContainer?>(null)
    private val _saving = MutableStateFlow(false)
    private val _snackbar = MutableStateFlow<String?>(null)
    private val _showNew = MutableStateFlow(false)

    val uiState: StateFlow<ContainerUiState> = combine(
        repo.getContainers(),
        _editing,
        _saving,
        _snackbar,
        _showNew,
    ) { containers, editing, saving, snackbar, showNew ->
        ContainerUiState(
            containers = containers,
            editingContainer = editing,
            isWinlatorInstalled = repo.isWinlatorInstalled(),
            isLudashiInstalled = repo.isLudashiInstalled(),
            isSaving = saving,
            snackbarMessage = snackbar,
            showNewContainerDialog = showNew,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContainerUiState())

    init {
        viewModelScope.launch { repo.syncFromDisk() }
    }

    fun launchContainer(id: Int) = repo.launchContainer(id)
    fun openWinlator()           = repo.openWinlator()
    fun openLudashi()            = repo.openLudashi()

    fun startEditing(container: WinlatorContainer) { _editing.value = container.copy() }
    fun cancelEditing() { _editing.value = null }
    fun showNewContainerDialog() { _showNew.value = true }
    fun hideNewContainerDialog() { _showNew.value = false }

    // Live field updates (called by the edit form)
    fun updateName(v: String)         { _editing.update { it?.copy(name = v) } }
    fun updateScreenSize(v: String)   { _editing.update { it?.copy(screenSize = v) } }
    fun updateScreenDpi(v: Int)       { _editing.update { it?.copy(screenDpi = v) } }
    fun updateCpuCount(v: Int)        { _editing.update { it?.copy(cpuCount = v) } }
    fun updateRamMb(v: Int)           { _editing.update { it?.copy(ramMb = v) } }
    fun updateGraphicsDriver(v: GraphicsDriver) { _editing.update { it?.copy(graphicsDriver = v) } }
    fun updateAudioDriver(v: AudioDriver)       { _editing.update { it?.copy(audioDriver = v) } }
    fun updateDxWrapper(v: DxWrapper)           { _editing.update { it?.copy(dxwrapper = v) } }
    fun updateWinVersion(v: WinVersion)         { _editing.update { it?.copy(winVersion = v) } }
    fun updateEnvVars(v: String)      { _editing.update { it?.copy(envVars = v) } }
    fun updateNotes(v: String)        { _editing.update { it?.copy(notes = v) } }
    fun updateBoxed86(v: Boolean)     { _editing.update { it?.copy(isBoxed86 = v) } }

    fun saveEditing() {
        val c = _editing.value ?: return
        viewModelScope.launch {
            _saving.value = true
            val ok = repo.saveContainer(c)
            _editing.value = null
            _saving.value = false
            _snackbar.value = if (ok) "✅ Container sauvegardé" else "⚠️ Sauvegardé en local uniquement (accès fichier limité)"
        }
    }

    fun createNewContainer(name: String) {
        viewModelScope.launch {
            val nextId = uiState.value.containers.maxOfOrNull { it.id }?.plus(1) ?: 1
            val c = WinlatorContainer(id = nextId, name = name)
            repo.saveContainer(c)
            _showNew.value = false
            _snackbar.value = "Container « $name » créé"
            startEditing(c)
        }
    }

    fun deleteContainer(container: WinlatorContainer) {
        viewModelScope.launch {
            repo.deleteContainer(container)
            _snackbar.value = "Container supprimé"
        }
    }

    fun syncContainers() {
        viewModelScope.launch {
            repo.syncFromDisk()
            _snackbar.value = "Containers synchronisés"
        }
    }

    fun clearSnackbar() { _snackbar.value = null }
}
