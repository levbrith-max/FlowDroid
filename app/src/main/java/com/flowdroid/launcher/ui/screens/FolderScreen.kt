package com.flowdroid.launcher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowdroid.launcher.data.models.AppEntry
import com.flowdroid.launcher.data.repository.AppRepository
import com.flowdroid.launcher.ui.components.*
import com.flowdroid.launcher.ui.theme.*
import com.flowdroid.launcher.viewmodel.HomeViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

// ─── Folder Screen ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    folderId: Long,
    onBack: () -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
    folderViewModel: FolderViewModel = hiltViewModel(),
) {
    val apps by folderViewModel.getAppsInFolder(folderId).collectAsState(initial = emptyList())
    val folder by folderViewModel.getFolder(folderId).collectAsState(initial = null)
    val snackbarHostState = remember { SnackbarHostState() }
    var menuExpanded by remember { mutableStateOf<String?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        folder?.name ?: "Dossier",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Retour") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (apps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Dossier vide", color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                item { SectionHeader("${apps.size} application(s)") }
                items(apps, key = { it.packageName }) { app ->
                    Box {
                        FlowItemRow(
                            icon = { AppIconBox(app.label) },
                            title = app.customLabel ?: app.label,
                            subtitle = app.packageName,
                            badge = {
                                if (app.isLocked) LockBadge()
                                if (app.isHidden) HiddenBadge()
                            },
                            onClick = { homeViewModel.launchApp(app.packageName) },
                            onLongClick = { menuExpanded = app.packageName },
                        )
                        FlowContextMenu(
                            expanded = menuExpanded == app.packageName,
                            onDismiss = { menuExpanded = null },
                            items = listOf(
                                ContextMenuItem("Ouvrir", Icons.Default.Launch) {
                                    homeViewModel.launchApp(app.packageName)
                                },
                                ContextMenuItem("Retirer du dossier", Icons.Default.FolderOff) {
                                    homeViewModel.moveAppToFolder(app.packageName, null)
                                },
                                ContextMenuItem(
                                    if (app.isHidden) "Afficher" else "Masquer",
                                    if (app.isHidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                ) { homeViewModel.setAppHidden(app.packageName, !app.isHidden) },
                            ),
                        )
                    }
                    Divider(
                        color = MaterialTheme.colorScheme.outline.copy(0.3f),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

// ─── Hidden Apps Screen ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenAppsScreen(
    onBack: () -> Unit,
    viewModel: FolderViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val hiddenApps by viewModel.hiddenApps.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Applications masquées", style = MaterialTheme.typography.titleLarge)
                        Text("Appui long pour démasquer",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Retour") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (hiddenApps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Visibility, null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(0.2f))
                    Spacer(Modifier.height(12.dp))
                    Text("Aucune application masquée",
                        color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                item { SectionHeader("${hiddenApps.size} application(s) masquée(s)") }
                items(hiddenApps, key = { it.packageName }) { app ->
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        FlowItemRow(
                            icon = { AppIconBox(app.label, color = FlowTextSub) },
                            title = app.customLabel ?: app.label,
                            subtitle = app.packageName,
                            badge = { HiddenBadge() },
                            onClick = { homeViewModel.requestUnlockApp(app.packageName) {
                                homeViewModel.launchApp(app.packageName)
                            }},
                            onLongClick = { menu = true },
                        )
                        FlowContextMenu(
                            expanded = menu,
                            onDismiss = { menu = false },
                            items = listOf(
                                ContextMenuItem("Afficher", Icons.Outlined.Visibility) {
                                    homeViewModel.setAppHidden(app.packageName, false)
                                },
                                ContextMenuItem("Lancer quand même", Icons.Default.Launch) {
                                    homeViewModel.launchApp(app.packageName)
                                },
                            ),
                        )
                    }
                    Divider(
                        color = MaterialTheme.colorScheme.outline.copy(0.3f),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

// ─── FolderViewModel ─────────────────────────────────────────────────────────

@HiltViewModel
class FolderViewModel @Inject constructor(
    private val appRepo: AppRepository,
) : ViewModel() {

    val hiddenApps: Flow<List<AppEntry>> = appRepo.getHiddenApps()

    fun getAppsInFolder(folderId: Long): Flow<List<AppEntry>> =
        appRepo.getAppsInFolder(folderId)

    fun getFolder(folderId: Long): StateFlow<com.flowdroid.launcher.data.models.AppFolder?> =
        appRepo.getAllFolders()
            .map { list -> list.find { it.id == folderId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
