package com.flowdroid.launcher.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.flowdroid.launcher.ui.theme.*

@Composable
fun FlowSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    placeholder: String = "Rechercher applications, dossiers, containers…",
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp),
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                decorationBox = @Composable { innerTextField ->
                    if (query.isEmpty()) {
                        Text(placeholder, style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    innerTextField()
                },
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            )
            AnimatedVisibility(visible = query.isNotEmpty()) {
                IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Effacer",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FlowItemRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    badge: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false))
                    if (badge != null) { Spacer(Modifier.width(6.dp)); badge() }
                }
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (trailing != null) { Spacer(Modifier.width(8.dp)); trailing() }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                Text(action, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun LockBadge() = Badge(containerColor = FlowAmber) {
    Icon(Icons.Default.Lock, contentDescription = "Verrouillé", modifier = Modifier.size(10.dp))
}

@Composable
fun HiddenBadge() = Badge(containerColor = FlowTextSub) {
    Icon(Icons.Default.VisibilityOff, contentDescription = "Caché", modifier = Modifier.size(10.dp))
}

@Composable
fun AppIconBox(label: String, color: Color = FlowBlue, size: Dp = 44.dp) {
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.take(1).uppercase(),
            style = MaterialTheme.typography.titleLarge.copy(
                color = color, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp,
            ),
        )
    }
}

@Composable
fun FolderIconBox(folder: com.flowdroid.launcher.data.models.AppFolder, size: Dp = 44.dp) {
    val color = runCatching {
        Color(android.graphics.Color.parseColor(folder.colorHex))
    }.getOrElse { FlowBlue }
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(folder.iconEmoji, fontSize = (size.value * 0.48f).sp)
    }
}

@Composable
fun ContainerIconBox(size: Dp = 44.dp) {
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(12.dp))
            .background(FlowBlue.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.Memory, contentDescription = null,
            tint = FlowBlue, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun PinDialog(
    title: String,
    description: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(description, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8) { pin = it; error = false } },
                    label = { Text("PIN (4–8 chiffres)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = error,
                    supportingText = if (error) ({ Text("PIN invalide") }) else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (pin.length >= 4) onConfirm(pin) else error = true }) {
                Text("Confirmer")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

data class ContextMenuItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color? = null,
    val onClick: () -> Unit,
)

@Composable
fun FlowContextMenu(expanded: Boolean, onDismiss: () -> Unit, items: List<ContextMenuItem>) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss,
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
        items.forEach { item ->
            DropdownMenuItem(
                text = { Text(item.label, color = item.tint ?: MaterialTheme.colorScheme.onSurface) },
                leadingIcon = {
                    Icon(item.icon, contentDescription = null,
                        tint = item.tint ?: MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp))
                },
                onClick = { item.onClick(); onDismiss() },
            )
        }
    }
}

@Composable
fun <T> ChipRow(items: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
            )
        }
    }
}
