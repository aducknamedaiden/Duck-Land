package com.example.ui.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.DuckPalette
import com.example.domain.model.PromoCodeCatalog
import com.example.domain.model.PromoReward
import com.example.ui.components.DuckCanvas
import com.example.ui.components.GrapeClusterIcon
import com.example.ui.components.RarityBadge

@Composable
fun PromoCodesBannerCard(
    redeemedCodes: Set<String>,
    isOwner: Boolean,
    isBusy: Boolean,
    onRedeemCode: (String) -> Unit,
    onOpenOwnerPanel: () -> Unit
) {
    var codeInput by remember { mutableStateOf("") }
    var showVaultDialog by remember { mutableStateOf(false) }

    if (showVaultDialog) {
        PromoCodesVaultDialog(
            redeemedCodes = redeemedCodes,
            isBusy = isBusy,
            onSelectAndRedeem = { code ->
                codeInput = code
                onRedeemCode(code)
                showVaultDialog = false
            },
            onDismiss = { showVaultDialog = false }
        )
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ConfirmationNumber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text("Secret & Promo Codes", style = MaterialTheme.typography.titleMedium)
                }
                TextButton(
                    onClick = { showVaultDialog = true },
                    modifier = Modifier.testTag("open_codes_vault_button")
                ) {
                    Text("View Codes (${PromoCodeCatalog.publicCodes.size})")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { if (it.length <= 32) codeInput = it },
                    placeholder = { Text("Try BETA, QUACK100...") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("promo_code_input")
                )
                Button(
                    onClick = {
                        onRedeemCode(codeInput)
                        codeInput = ""
                    },
                    enabled = !isBusy && codeInput.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("redeem_code_button")
                ) {
                    Text("Redeem")
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PromoCodeCatalog.publicCodes.take(5), key = { it.code }) { def ->
                    val alreadyRedeemed = def.code in redeemedCodes
                    AssistChip(
                        onClick = { codeInput = def.code },
                        leadingIcon = if (alreadyRedeemed) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Redeemed",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        label = { Text(if (alreadyRedeemed) "${def.code} ✓" else def.code) },
                        modifier = Modifier.testTag("quick_code_${def.code.lowercase()}")
                    )
                }
            }

            if (isOwner) {
                Button(
                    onClick = onOpenOwnerPanel,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD54F),
                        contentColor = Color(0xFF1F1602)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_owner_panel_button")
                ) {
                    Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Owner Admin Panel", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun PromoCodesVaultDialog(
    redeemedCodes: Set<String>,
    isBusy: Boolean,
    onSelectAndRedeem: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Duck Land Promo Codes") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.height(340.dp)
            ) {
                items(PromoCodeCatalog.publicCodes, key = { it.code }) { def ->
                    val claimed = def.code in redeemedCodes
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (claimed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = def.code,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = def.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (claimed) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = "Claimed",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { onSelectAndRedeem(def.code) },
                                    enabled = !isBusy,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text("Claim")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun PromoRewardDialog(
    reward: PromoReward,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Code Redeemed!") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (reward) {
                    is PromoReward.DuckReward -> {
                        DuckCanvas(
                            palette = reward.duck?.let { DuckPalette.fromMap(it.palette) }
                                ?: reward.species.defaultPalette,
                            size = 148.dp,
                            cornerRadius = 28.dp
                        )
                        Text(
                            text = reward.customName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        RarityBadge(rarity = reward.rarity)
                        Text(
                            text = "Code ${reward.code} added this duck to your pond!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is PromoReward.CoinReward -> {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(96.dp)
                        ) {
                            GrapeClusterIcon(
                                modifier = Modifier.padding(20.dp),
                                grapeColor = MaterialTheme.colorScheme.primary,
                                highlightColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.45f),
                                leafColor = MaterialTheme.colorScheme.secondary,
                                contentDesc = null
                            )
                        }
                        Text(
                            text = "+${reward.coins} Grapes",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Code ${reward.code} boosted your Grape Wallet!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is PromoReward.OwnerUnlockReward -> {
                        Text(reward.summary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_promo_reward_button")
            ) {
                Text("Awesome!")
            }
        }
    )
}
