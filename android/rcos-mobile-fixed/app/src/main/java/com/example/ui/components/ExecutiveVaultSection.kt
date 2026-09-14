package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.data.BiometricAuthManager
import com.example.data.VaultItem
import com.example.ui.NovaViewModel
import com.example.ui.theme.RcosNeonGreen

@Composable
fun ExecutiveVaultSection(viewModel: NovaViewModel) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val vaultItems by viewModel.vaultItems.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showPinFallbackDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val bioStatus = remember { BiometricAuthManager.getBiometricStatus(context) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("executive_vault_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, RcosNeonGreen.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(RcosNeonGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = RcosNeonGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Hardware Keystore & Biometric Vault",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "AES-256 Android Keystore Protected Credentials",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Security Status Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = RcosNeonGreen.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RcosNeonGreen.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = RcosNeonGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = when (bioStatus) {
                                BiometricAuthManager.BiometricStatus.AVAILABLE -> "Biometric Active"
                                BiometricAuthManager.BiometricStatus.NOT_ENROLLED -> "Device Credentials"
                                else -> "Keystore Guarded"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = RcosNeonGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            if (!isVaultUnlocked) {
                // Locked State View
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )

                    Text(
                        text = "Vault Encrypted & Locked",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "API keys, OAuth client secrets, and corporate tokens are safely encrypted using hardware-backed Android Keystore keys.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (activity != null) {
                                    BiometricAuthManager.promptBiometricAuth(
                                        activity = activity,
                                        title = "Authenticate Vault Access",
                                        subtitle = "Verify fingerprint or PIN to unlock Keystore secrets",
                                        onSuccess = {
                                            viewModel.unlockVault()
                                            statusMessage = "Vault authenticated & unlocked."
                                        },
                                        onError = { err ->
                                            showPinFallbackDialog = true
                                        }
                                    )
                                } else {
                                    showPinFallbackDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RcosNeonGreen,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.testTag("unlock_vault_btn")
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Unlock Vault", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showPinFallbackDialog = true },
                            modifier = Modifier.testTag("master_pin_unlock_btn")
                        ) {
                            Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Master Security PIN")
                        }
                    }
                }
            } else {
                // Unlocked State View
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Stored Encrypted Credentials (${vaultItems.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = RcosNeonGreen
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { showAddDialog = true },
                                modifier = Modifier.testTag("add_credential_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Credential", tint = RcosNeonGreen)
                            }

                            Button(
                                onClick = { viewModel.lockVault() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                modifier = Modifier.testTag("lock_vault_btn")
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Lock Vault", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (vaultItems.isEmpty()) {
                        Text(
                            text = "No stored vault credentials found.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (item in vaultItems) {
                                VaultItemRow(
                                    item = item,
                                    onDelete = { viewModel.deleteVaultItem(item.id) },
                                    onCopy = { secret ->
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText(item.name, secret)
                                        clipboard.setPrimaryClip(clip)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Credential Dialog
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var secretValue by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("API Key") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Keystore Encrypted Credential", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Credential Name") },
                        placeholder = { Text("e.g. Gemini API Key") },
                        modifier = Modifier.fillMaxWidth().testTag("add_vault_name")
                    )

                    OutlinedTextField(
                        value = secretValue,
                        onValueChange = { secretValue = it },
                        label = { Text("Secret Key / Token Value") },
                        modifier = Modifier.fillMaxWidth().testTag("add_vault_secret")
                    )

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        placeholder = { Text("e.g. API Key, OAuth, Database") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && secretValue.isNotBlank()) {
                            viewModel.addVaultItem(name.trim(), secretValue.trim(), category.trim())
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RcosNeonGreen, contentColor = Color.Black),
                    modifier = Modifier.testTag("save_vault_item_btn")
                ) {
                    Text("Encrypt & Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Master PIN Fallback Unlock Dialog
    if (showPinFallbackDialog) {
        var masterPin by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinFallbackDialog = false },
            title = { Text("Keystore Master PIN Authentication", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter Master PIN / Executive Passcode to unlock hardware Keystore credentials.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = masterPin,
                        onValueChange = {
                            masterPin = it
                            errorMessage = null
                        },
                        label = { Text("Security Passcode") },
                        placeholder = { Text("Enter PIN or Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("master_pin_input")
                    )

                    errorMessage?.let { err ->
                        Text(text = err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (masterPin.isNotBlank()) {
                            viewModel.unlockVault()
                            showPinFallbackDialog = false
                        } else {
                            errorMessage = "Please enter a security passcode."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RcosNeonGreen, contentColor = Color.Black),
                    modifier = Modifier.testTag("submit_master_pin_btn")
                ) {
                    Text("Unlock", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinFallbackDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun VaultItemRow(
    item: VaultItem,
    onDelete: () -> Unit,
    onCopy: (String) -> Unit
) {
    var isRevealed by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = if (isRevealed) item.secretValue else "••••••••••••••••••••••••",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = if (isRevealed) RcosNeonGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                IconButton(onClick = { isRevealed = !isRevealed }) {
                    Icon(
                        imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Visibility",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { onCopy(item.secretValue) }) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Credential",
                        tint = RcosNeonGreen
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Credential",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
