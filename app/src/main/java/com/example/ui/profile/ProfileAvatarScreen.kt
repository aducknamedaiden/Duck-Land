package com.example.ui.profile

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.DuckPalette
import com.example.domain.model.ThemePreference
import com.example.domain.model.UiState
import com.example.ui.components.DuckCanvas
import com.example.ui.components.EmptyPondCard
import com.example.ui.components.GrapeClusterIcon
import com.example.ui.components.ModularDuckAvatarCustomizer
import com.example.ui.components.WindowWidthClass
import com.example.ui.owner.PromoCodesBannerCard

@Composable
fun ProfileAvatarScreen(
    uiState: UiState<StudioProfileData>,
    isBusy: Boolean,
    hasOsNotificationPermission: Boolean,
    isOwner: Boolean,
    redeemedCodes: Set<String>,
    onSaveProfile: (String, String, DuckPalette) -> Unit,
    onThemeChange: (ThemePreference) -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    onPermissionResult: (Boolean) -> Unit,
    onRedeemCode: (String, Long) -> Unit,
    onOpenOwnerPanel: () -> Unit,
    onOpenAiStylist: () -> Unit,
    onSignOut: () -> Unit
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onPermissionResult(granted)
    }

    when (uiState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyPondCard(
                    title = "Studio unavailable",
                    subtitle = uiState.message
                )
            }
        }

        is UiState.Success -> {
            val data = uiState.data
            val profile = data.profile
            val savedAvatar = remember(profile?.avatarConfig) {
                DuckPalette.fromMap(profile?.avatarConfig)
            }
            var draftAvatar by remember(profile?.userId, profile?.avatarConfig) {
                mutableStateOf(savedAvatar)
            }
            var draftName by remember(profile?.userId, profile?.displayName) {
                mutableStateOf(profile?.displayName ?: "Pond Collector")
            }
            var draftBio by remember(profile?.userId, profile?.bio) {
                mutableStateOf(profile?.bio ?: "")
            }
            val hasUnsavedChanges = draftAvatar != savedAvatar ||
                draftName.trim() != (profile?.displayName ?: "Pond Collector").trim() ||
                draftBio.trim() != (profile?.bio ?: "").trim()

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val widthClass = WindowWidthClass.fromWidth(maxWidth)
                val isWide = widthClass == WindowWidthClass.EXPANDED

                if (isWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .width(340.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            AvatarPreviewCard(
                                avatar = draftAvatar,
                                displayName = draftName,
                                duckCount = profile?.duckCount ?: 0L,
                                coins = data.wallet?.coins ?: 0L,
                                hasUnsavedChanges = hasUnsavedChanges
                            )
                            SettingsCard(
                                themePreference = data.themePreference,
                                notificationsEnabled = data.notificationsEnabled,
                                hasOsNotificationPermission = hasOsNotificationPermission,
                                onThemeChange = onThemeChange,
                                onNotificationsToggle = onNotificationsToggle,
                                onRequestOsPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                onSignOut = onSignOut
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            AvatarBuilderCard(
                                draftAvatar = draftAvatar,
                                draftName = draftName,
                                draftBio = draftBio,
                                isBusy = isBusy,
                                hasUnsavedChanges = hasUnsavedChanges,
                                onAvatarChange = { draftAvatar = it },
                                onNameChange = { draftName = it },
                                onBioChange = { draftBio = it },
                                onOpenAiStylist = onOpenAiStylist,
                                onSave = { onSaveProfile(draftName, draftBio, draftAvatar) }
                            )
                            PromoCodesBannerCard(
                                redeemedCodes = redeemedCodes,
                                isOwner = isOwner,
                                isBusy = isBusy,
                                onRedeemCode = { code ->
                                    onRedeemCode(code, data.wallet?.coins ?: 0L)
                                },
                                onOpenOwnerPanel = onOpenOwnerPanel
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Avatar & Studio",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        AvatarPreviewCard(
                            avatar = draftAvatar,
                            displayName = draftName,
                            duckCount = profile?.duckCount ?: 0L,
                            coins = data.wallet?.coins ?: 0L,
                            hasUnsavedChanges = hasUnsavedChanges
                        )
                        AvatarBuilderCard(
                            draftAvatar = draftAvatar,
                            draftName = draftName,
                            draftBio = draftBio,
                            isBusy = isBusy,
                            hasUnsavedChanges = hasUnsavedChanges,
                            onAvatarChange = { draftAvatar = it },
                            onNameChange = { draftName = it },
                            onBioChange = { draftBio = it },
                            onOpenAiStylist = onOpenAiStylist,
                            onSave = { onSaveProfile(draftName, draftBio, draftAvatar) }
                        )
                        PromoCodesBannerCard(
                            redeemedCodes = redeemedCodes,
                            isOwner = isOwner,
                            isBusy = isBusy,
                            onRedeemCode = { code ->
                                onRedeemCode(code, data.wallet?.coins ?: 0L)
                            },
                            onOpenOwnerPanel = onOpenOwnerPanel
                        )
                        SettingsCard(
                            themePreference = data.themePreference,
                            notificationsEnabled = data.notificationsEnabled,
                            hasOsNotificationPermission = hasOsNotificationPermission,
                            onThemeChange = onThemeChange,
                            onNotificationsToggle = onNotificationsToggle,
                            onRequestOsPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            onSignOut = onSignOut
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarPreviewCard(
    avatar: DuckPalette,
    displayName: String,
    duckCount: Long,
    coins: Long,
    hasUnsavedChanges: Boolean
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (hasUnsavedChanges) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = "Unsaved avatar preview",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
            DuckCanvas(
                palette = avatar,
                size = 168.dp,
                cornerRadius = 32.dp
            )
            Text(
                text = displayName.ifBlank { "Pond Collector" },
                style = MaterialTheme.typography.titleLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "$duckCount Ducks",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GrapeClusterIcon(
                            modifier = Modifier.size(16.dp),
                            grapeColor = MaterialTheme.colorScheme.primary,
                            highlightColor = MaterialTheme.colorScheme.primaryContainer,
                            leafColor = MaterialTheme.colorScheme.secondary,
                            contentDesc = null
                        )
                        Text(
                            text = "$coins Grapes",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarBuilderCard(
    draftAvatar: DuckPalette,
    draftName: String,
    draftBio: String,
    isBusy: Boolean,
    hasUnsavedChanges: Boolean,
    onAvatarChange: (DuckPalette) -> Unit,
    onNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onOpenAiStylist: () -> Unit,
    onSave: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ModularDuckAvatarCustomizer(
                palette = draftAvatar,
                onPaletteChange = onAvatarChange,
                onOpenAiStylist = onOpenAiStylist
            )

            OutlinedTextField(
                value = draftName,
                onValueChange = { if (it.length <= 40) onNameChange(it) },
                label = { Text("Collector Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("collector_name_input")
            )
            OutlinedTextField(
                value = draftBio,
                onValueChange = { if (it.length <= 160) onBioChange(it) },
                label = { Text("Pond Bio") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onSave,
                enabled = !isBusy && draftName.isNotBlank(),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_avatar_profile_button")
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (hasUnsavedChanges) "Save Avatar & Profile to Pond"
                    else "Saved in Firestore"
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    themePreference: ThemePreference,
    notificationsEnabled: Boolean,
    hasOsNotificationPermission: Boolean,
    onThemeChange: (ThemePreference) -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    onRequestOsPermission: () -> Unit,
    onSignOut: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ThemePreference.entries) { pref ->
                    FilterChip(
                        selected = themePreference == pref,
                        onClick = { onThemeChange(pref) },
                        label = { Text(pref.label) },
                        modifier = Modifier.testTag("theme_chip_${pref.name.lowercase()}")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text("Trade & Event Alerts", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (hasOsNotificationPermission) "Real-time notifications active"
                            else "System notification permission needed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = onNotificationsToggle,
                    modifier = Modifier.testTag("notifications_switch")
                )
            }

            if (!hasOsNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                OutlinedButton(
                    onClick = onRequestOsPermission,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("grant_notification_permission_button")
                ) {
                    Text("Enable Push Alert Permission")
                }
            }

            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
