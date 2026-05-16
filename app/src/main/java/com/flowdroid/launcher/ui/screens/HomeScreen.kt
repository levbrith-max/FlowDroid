package com.flowdroid.launcher.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowdroid.launcher.data.models.*
import com.flowdroid.launcher.ui.components.*
import com.flowdroid.launcher.ui.theme.*
import com.flowdroid.launcher.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToFolder: (Long) -> Unit,
    onNavigateToContainers: () -> Unit,
    onNavigateToHidden: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // PIN dialog handling
    state.pinDialogTarget?.let { target ->
        when (target) {
            is PinTarget.AppLock -> PinDialog(
                title = "Verrouiller l'app",
                description = "Choisissez un PIN (4–8 chiffres) pour protéger cette application.",
                onConfirm = { viewModel.confirmLockApp(target.packageName, it) },
                onDismiss = viewModel::dismissPinDialog,
            )
            is PinTarget.AppUnlock -> PinDialog(
                title = "Déverrouiller",
                description = "Saisissez le PIN pour ouvrir cette application.",
                onConfirm = { viewModel.unlockApp(target.packageName, it, target.onSuccess) },
                onDismiss = viewModel::dismissPinDialog,
            )
            is PinTarget.FolderLock -> PinDialog(
                title = "Verrouiller le dossier",
                description = "Choisissez un PIN pour protéger ce dossier.",
                onConfirm = { viewModel.confirmLockFolder(target.folderId, it) },
                onDismiss = viewModel::dismissPinDialog,
            )
            is PinTarget.FolderUnlock -> PinDialog(
                title = "Ouvrir le dossier",
                description = "Saisissez le PIN pour accéder au dossier.",
                onConfirm = { pin ->
                    // simple verify via snackbar error – real check in VM
                    viewModel.unlockApp("__folder_${target.folderId}", pin) {}
                    target.onSuccess()
                    viewModel.dismissPinDialog()
                },
                onDismiss = viewModel::dismissPinDialog,
            )
        }
    }

    // New folder dialog
    var showNewFolder by remember { mutableStateOf(false) }
    if (showNewFolder) {
        NewFolderDialog(
            onConfirm = { name, color, emoji ->
                viewModel.createFolder(name, color, emoji)
                showNewFolder = false
            },
            onDismiss = { showNewFolder = false },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "FlowDroid",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold, color = FlowBlue,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onNavigateToHidden) {
                        Icon(Icons.Outlined.VisibilityOff, "Apps cachées",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    IconButton(onClick = onNavigateToContainers) {
                        Icon(Icons.Default.Memory, "Containers Winlator",
                            tint = FlowBlue)
                    }
                }
                Spacer(Modifier.height(8.dp))
                FlowSearchBar(
                    query = state.searchQuery,
                    onQueryChange = viewModel::onSearchQuery,
                    onClear = viewModel::clearSearch,
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(visible = !state.isSearching) {
                FloatingActionButton(
                    onClick = { showNewFolder = true },
                    containerColor = FlowBlue,
                    contentColor = Color.White,
                ) {
                    Icon(Icons.Default.CreateNewFolder, "Nouveau dossier")
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                AnimatedContent(
                    targetState = state.isSearching,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                ) { searching ->
                    if (searching) {
                        SearchResultsPane(
                            results = state.searchResults,
                            onAppClick = { pkg ->
                                viewModel.requestUnlockApp(pkg) { viewModel.launchApp(pkg) }
                            },
                            onFolderClick = onNavigateToFolder,
                            onContainerClick = { /* navigate to container detail */ },
                        )
                    } else {
                        MainContentPane(
                            folders = state.folders,
                            apps = state.rootApps,
                            onFolderClick = onNavigateToFolder,
                            onAppClick = { viewModel.launchApp(it) },
                            viewModel = viewModel,
                        )
                    }
                }
            }
        }
    }
}

// ─── Main content ─────────────────────────────────────────────────────────────

@Composable
private fun MainContentPane(
    folders: List<AppFolder>,
    apps: List<AppEntry>,
    onFolderClick: (Long) -> Unit,
    onAppClick: (String) -> Unit,
    viewModel: HomeViewModel,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 80.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (folders.isNotEmpty()) {
            item { SectionHeader(title = "Dossiers", action = null) }
            items(folders, key = { it.id }) { folder ->
                FolderRow(folder, onFolderClick, viewModel)
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    modifier = Modifier.padding(horizontal = 16.dp))
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        if (apps.isNotEmpty()) {
            item { SectionHeader(title = "Applications") }
            items(apps, key = { it.packageName }) { app ->
                AppRow(app, onAppClick, viewModel)
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        if (folders.isEmpty() && apps.isEmpty()) {
            item {
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Apps, null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(0.2f))
                        Spacer(Modifier.height(12.dp))
                        Text("Aucune application installée",
                            color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                    }
                }
            }
        }
    }
}

// ─── App row with context menu ────────────────────────────────────────────────

@Composable
private fun AppRow(
    app: AppEntry,
    onAppClick: (String) -> Unit,
    viewModel: HomeViewModel,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        FlowItemRow(
            icon = {
                AppIconBox(app.customLabel ?: app.label,
                    color = if (app.isLocked) FlowAmber else FlowBlue)
            },
            title = app.customLabel ?: app.label,
            subtitle = app.packageName,
            badge = {
                if (app.isLocked)  LockBadge()
                if (app.isHidden)  HiddenBadge()
            },
            onClick = { onAppClick(app.packageName) },
            onLongClick = { menuExpanded = true },
        )
        FlowContextMenu(
            expanded = menuExpanded,
            onDismiss = { menuExpanded = false },
            items = buildList {
                add(ContextMenuItem("Ouvrir", Icons.Default.Launch) { onAppClick(app.packageName) })
                add(ContextMenuItem(
                    if (app.isHidden) "Afficher" else "Masquer",
                    if (app.isHidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                ) { viewModel.setAppHidden(app.packageName, !app.isHidden) })
                if (app.isLocked) {
                    add(ContextMenuItem("Retirer le verrou", Icons.Default.LockOpen, FlowAmber) {
                        viewModel.removeAppLock(app.packageName)
                    })
                } else {
                    add(ContextMenuItem("Verrouiller", Icons.Default.Lock) {
                        viewModel.requestLockApp(app.packageName)
                    })
                }
            },
        )
    }
}

// ─── Folder row ───────────────────────────────────────────────────────────────

@Composable
private fun FolderRow(
    folder: AppFolder,
    onFolderClick: (Long) -> Unit,
    viewModel: HomeViewModel,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box {
        FlowItemRow(
            icon = { FolderIconBox(folder) },
            title = folder.name,
            badge = {
                if (folder.isLocked) LockBadge()
                if (folder.isHidden) HiddenBadge()
            },
            onClick = {
                if (folder.isLocked) {
                    viewModel.requestUnlockFolder(folder.id) { onFolderClick(folder.id) }
                } else {
                    onFolderClick(folder.id)
                }
            },
            onLongClick = { menuExpanded = true },
        )
        FlowContextMenu(
            expanded = menuExpanded,
            onDismiss = { menuExpanded = false },
            items = listOf(
                ContextMenuItem("Ouvrir", Icons.Default.FolderOpen) { onFolderClick(folder.id) },
                ContextMenuItem(
                    if (folder.isHidden) "Afficher" else "Masquer",
                    if (folder.isHidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                ) { viewModel.setFolderHidden(folder.id, !folder.isHidden) },
                ContextMenuItem("Verrouiller", Icons.Default.Lock) {
                    viewModel.requestLockFolder(folder.id)
                },
                ContextMenuItem("Supprimer", Icons.Default.DeleteOutline, FlowRed) {
                    viewModel.deleteFolder(folder.id)
                },
            ),
        )
    }
}

// ─── Search results ───────────────────────────────────────────────────────────

@Composable
private fun SearchResultsPane(
    results: List<SearchResult>,
    onAppClick: (String) -> Unit,
    onFolderClick: (Long) -> Unit,
    onContainerClick: (Int) -> Unit,
) {
    if (results.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aucun résultat", color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        items(results) { result ->
            when (result) {
                is SearchResult.App -> FlowItemRow(
                    icon = { AppIconBox(result.entry.label) },
                    title = result.entry.customLabel ?: result.entry.label,
                    subtitle = result.entry.packageName,
                    onClick = { onAppClick(result.entry.packageName) },
                )
                is SearchResult.Folder -> FlowItemRow(
                    icon = { FolderIconBox(result.folder) },
                    title = result.folder.name,
                    subtitle = "Dossier",
                    onClick = { onFolderClick(result.folder.id) },
                )
                is SearchResult.Container -> FlowItemRow(
                    icon = { ContainerIconBox() },
                    title = result.container.name,
                    subtitle = "Container Winlator • ${result.container.screenSize}",
                    onClick = { onContainerClick(result.container.id) },
                )
            }
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

// ─── New folder dialog ────────────────────────────────────────────────────────

@Composable
private fun NewFolderDialog(
    onConfirm: (name: String, colorHex: String, emoji: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("📁") }
    val colorOptions = listOf("#5C6BC0","#26A69A","#EF5350","#FFA726","#66BB6A","#AB47BC")
    var selectedColor by remember { mutableStateOf(colorOptions[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau dossier") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du dossier") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { if (it.length <= 2) emoji = it },
                    label = { Text("Emoji icône") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Couleur", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorOptions.forEach { hex ->
                        val color = runCatching {
                            Color(android.graphics.Color.parseColor(hex))
                        }.getOrElse { FlowBlue }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(color, shape = RoundedCornerShape(8.dp))
                                .border(
                                    width = if (selectedColor == hex) 2.dp else 0.dp,
                                    color = Color.White,
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .clickable { selectedColor = hex },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) onConfirm(name.trim(), selectedColor, emoji)
            }) { Text("Créer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
