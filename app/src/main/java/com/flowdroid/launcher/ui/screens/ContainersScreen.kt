package com.flowdroid.launcher.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowdroid.launcher.data.models.*
import com.flowdroid.launcher.ui.components.*
import com.flowdroid.launcher.ui.theme.*
import com.flowdroid.launcher.viewmodel.ContainerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainersScreen(
    onBack: () -> Unit,
    viewModel: ContainerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    if (state.showNewContainerDialog) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = viewModel::hideNewContainerDialog,
            title = { Text("Nouveau Container") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nom du container") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) viewModel.createNewContainer(newName.trim())
                }) { Text("Créer") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideNewContainerDialog) { Text("Annuler") }
            },
        )
    }

    state.editingContainer?.let { container ->
        ContainerEditSheet(
            container = container,
            isSaving = state.isSaving,
            viewModel = viewModel,
            onDismiss = viewModel::cancelEditing,
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Containers Winlator", style = MaterialTheme.typography.titleLarge)
                        if (!state.isWinlatorInstalled) {
                            Text("Winlator non détecté", style = MaterialTheme.typography.labelMedium,
                                color = FlowRed)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Retour") }
                },
                actions = {
                    if (state.isLudashiInstalled) {
                        IconButton(onClick = viewModel::openLudashi) {
                            Icon(Icons.Outlined.Gamepad, "Ouvrir Ludashi", tint = FlowAmber)
                        }
                    }
                    if (state.isWinlatorInstalled) {
                        IconButton(onClick = viewModel::openWinlator) {
                            Icon(Icons.Outlined.Window, "Ouvrir Winlator", tint = FlowBlue)
                        }
                    }
                    IconButton(onClick = viewModel::syncContainers) {
                        Icon(Icons.Default.Sync, "Synchroniser")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showNewContainerDialog,
                containerColor = FlowBlue,
                contentColor = Color.White,
            ) { Icon(Icons.Default.Add, "Nouveau container") }
        },
    ) { padding ->
        if (state.containers.isEmpty()) {
            EmptyContainersPane(
                isWinlatorInstalled = state.isWinlatorInstalled,
                onSyncClick = viewModel::syncContainers,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                item {
                    SectionHeader(
                        title = "${state.containers.size} container(s)",
                        action = "Synchroniser",
                        onAction = viewModel::syncContainers,
                    )
                }
                items(state.containers, key = { it.id }) { container ->
                    ContainerRow(
                        container = container,
                        onLaunch = { viewModel.launchContainer(container.id) },
                        onEdit   = { viewModel.startEditing(container) },
                        onDelete = { viewModel.deleteContainer(container) },
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(0.3f),
                        modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun ContainerRow(
    container: WinlatorContainer,
    onLaunch: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box {
        FlowItemRow(
            icon = { ContainerIconBox() },
            title = container.name,
            subtitle = "${container.screenSize} • ${container.graphicsDriver.label} • ${container.winVersion.label}",
            trailing = {
                Row {
                    IconButton(onClick = onLaunch, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.PlayArrow, "Lancer", tint = FlowGreen,
                            modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.MoreVert, "Options", modifier = Modifier.size(18.dp))
                    }
                }
            },
            onClick = onEdit,
            onLongClick = { menuExpanded = true },
        )
        FlowContextMenu(
            expanded = menuExpanded,
            onDismiss = { menuExpanded = false },
            items = listOf(
                ContextMenuItem("Lancer", Icons.Default.PlayArrow, FlowGreen, onLaunch),
                ContextMenuItem("Modifier", Icons.Default.Edit, onClick = onEdit),
                ContextMenuItem("Supprimer", Icons.Default.DeleteOutline, FlowRed, onDelete),
            ),
        )
    }
}

@Composable
private fun EmptyContainersPane(
    isWinlatorInstalled: Boolean,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.Memory, null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(0.15f))
        Spacer(Modifier.height(16.dp))
        Text(
            if (isWinlatorInstalled) "Aucun container trouvé" else "Winlator non installé",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(0.4f),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (isWinlatorInstalled)
                "Créez un container dans Winlator puis appuyez sur Synchroniser"
            else
                "Installez Winlator / Ludashi pour utiliser les containers",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(0.3f),
        )
        if (isWinlatorInstalled) {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onSyncClick,
                colors = ButtonDefaults.buttonColors(containerColor = FlowBlue),
            ) {
                Icon(Icons.Default.Sync, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Synchroniser")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContainerEditSheet(
    container: WinlatorContainer,
    isSaving: Boolean,
    viewModel: ContainerViewModel,
    onDismiss: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Modifier • ${container.name}", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Fermer") }
                },
                actions = {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = viewModel::saveEditing) {
                            Text("Sauvegarder", color = FlowBlue)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EditSection("Général") {
                FlowTextField("Nom", container.name, viewModel::updateName)
                FlowTextField("Notes", container.notes, viewModel::updateNotes, maxLines = 3)
            }
            EditSection("Affichage") {
                FlowTextField("Résolution (ex: 1280x720)", container.screenSize, viewModel::updateScreenSize)
                SliderField("DPI", container.screenDpi.toFloat(), 72f, 320f, steps = 24) {
                    viewModel.updateScreenDpi(it.toInt())
                }
            }
            EditSection("Ressources") {
                SliderField("Cœurs CPU", container.cpuCount.toFloat(), 1f, 8f, steps = 6) {
                    viewModel.updateCpuCount(it.toInt())
                }
                SliderField("RAM (Mo)", container.ramMb.toFloat(), 512f, 8192f, steps = 14) {
                    viewModel.updateRamMb(it.toInt())
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Box86/Box64", modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = container.isBoxed86, onCheckedChange = viewModel::updateBoxed86)
                }
            }
            EditSection("Graphiques & Audio") {
                Text("Driver graphique", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                ChipRow(items = GraphicsDriver.entries.map { it to it.label },
                    selected = container.graphicsDriver, onSelect = viewModel::updateGraphicsDriver)
                Spacer(Modifier.height(4.dp))
                Text("DX Wrapper", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                ChipRow(items = DxWrapper.entries.map { it to it.label },
                    selected = container.dxwrapper, onSelect = viewModel::updateDxWrapper)
                Spacer(Modifier.height(4.dp))
                Text("Driver audio", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                ChipRow(items = AudioDriver.entries.map { it to it.label },
                    selected = container.audioDriver, onSelect = viewModel::updateAudioDriver)
            }
            EditSection("Version Windows") {
                ChipRow(items = WinVersion.entries.map { it to it.label },
                    selected = container.winVersion, onSelect = viewModel::updateWinVersion)
            }
            EditSection("Variables d'environnement") {
                FlowTextField(label = "ENV (ex: DXVK_HUD=1 MESA_VK_WSI_DEBUG=sw)",
                    value = container.envVars, onValueChange = viewModel::updateEnvVars, maxLines = 4)
            }
            if (container.containerPath.isNotBlank()) {
                EditSection("Informations") {
                    Text("Chemin : ${container.containerPath}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    Text("ID : ${container.id}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                }
            }
        }
    }
}

@Composable
private fun EditSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 1.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = FlowBlue)
            content()
        }
    }
}

@Composable
private fun FlowTextField(label: String, value: String, onValueChange: (String) -> Unit, maxLines: Int = 1) {
    OutlinedTextField(value = value, onValueChange = onValueChange,
        label = { Text(label) }, maxLines = maxLines, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun SliderField(label: String, value: Float, min: Float, max: Float, steps: Int, onValueChange: (Float) -> Unit) {
    Column {
        Row {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(value.toInt().toString(), style = MaterialTheme.typography.bodyLarge, color = FlowBlue)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = min..max, steps = steps,
            colors = SliderDefaults.colors(thumbColor = FlowBlue, activeTrackColor = FlowBlue))
    }
}
