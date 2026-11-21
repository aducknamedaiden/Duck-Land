package com.example.ui.market

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.DuckPalette
import com.example.domain.model.DuckRarity
import com.example.domain.model.MarketplaceListing
import com.example.domain.model.RubberDuck
import com.example.domain.model.TradeOffer
import com.example.domain.model.UiState
import com.example.ui.components.DuckCanvas
import com.example.ui.components.EmptyPondCard
import com.example.ui.components.GrapeClusterIcon
import com.example.ui.components.PondCoinsChip
import com.example.ui.components.RarityBadge
import com.example.ui.components.RarityDuckCardContainer
import com.example.ui.components.WindowWidthClass

@Composable
fun MarketplaceScreen(
    currentUserId: String,
    uiState: UiState<MarketplaceData>,
    isBusy: Boolean,
    onBuyListing: (MarketplaceListing) -> Unit,
    onCancelListing: (MarketplaceListing) -> Unit,
    onProposeTrade: (RubberDuck, MarketplaceListing, Long, String) -> Unit,
    onAcceptTrade: (TradeOffer) -> Unit,
    onDeclineTrade: (TradeOffer) -> Unit,
    onCancelTrade: (TradeOffer) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var rarityFilter by remember { mutableStateOf<DuckRarity?>(null) }
    var selectedListing by remember { mutableStateOf<MarketplaceListing?>(null) }
    var tradeTargetListing by remember { mutableStateOf<MarketplaceListing?>(null) }

    if (tradeTargetListing != null && uiState is UiState.Success) {
        val availableDucks = uiState.data.myDucks.filter { !it.listed }
        ProposeTradeDialog(
            targetListing = tradeTargetListing!!,
            myAvailableDucks = availableDucks,
            myCoins = uiState.data.wallet?.coins ?: 0L,
            onDismiss = { tradeTargetListing = null },
            onSubmit = { duck, bonusCoins, note ->
                onProposeTrade(duck, tradeTargetListing!!, bonusCoins, note)
                tradeTargetListing = null
                selectedListing = null
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthClass = WindowWidthClass.fromWidth(maxWidth)
        val isSplitPane = widthClass == WindowWidthClass.EXPANDED

        if (selectedListing != null && !isSplitPane && uiState is UiState.Success) {
            BackHandler { selectedListing = null }
            AlertDialog(
                onDismissRequest = { selectedListing = null },
                confirmButton = {},
                text = {
                    ListingDetailPane(
                        listing = selectedListing!!,
                        currentUserId = currentUserId,
                        myCoins = uiState.data.wallet?.coins ?: 0L,
                        isBusy = isBusy,
                        onClose = { selectedListing = null },
                        onBuy = {
                            onBuyListing(selectedListing!!)
                            selectedListing = null
                        },
                        onProposeTrade = { tradeTargetListing = selectedListing },
                        onCancelListing = {
                            onCancelListing(selectedListing!!)
                            selectedListing = null
                        }
                    )
                }
            )
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
                        title = "Marketplace unavailable",
                        subtitle = uiState.message
                    )
                }
            }

            is UiState.Success -> {
                val data = uiState.data
                val pendingIncomingCount = data.tradeOffers.count {
                    it.recipientId == currentUserId && it.status == "pending"
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Duck Marketplace",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = "Buy with Grapes or swap ducks safely",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PondCoinsChip(coins = data.wallet?.coins ?: 0L)
                    }

                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Browse") },
                            modifier = Modifier.testTag("market_tab_browse")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                val suffix = if (pendingIncomingCount > 0) " ($pendingIncomingCount)" else ""
                                Text("Trades$suffix")
                            },
                            modifier = Modifier.testTag("market_tab_trades")
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("My Listings") },
                            modifier = Modifier.testTag("market_tab_mine")
                        )
                    }

                    Row(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            when (selectedTab) {
                                0 -> {
                                    val browseItems = remember(data.activeListings, rarityFilter) {
                                        if (rarityFilter == null) data.activeListings
                                        else data.activeListings.filter {
                                            DuckRarity.fromId(it.duckRarity) == rarityFilter
                                        }
                                    }
                                    val cols = when (widthClass) {
                                        WindowWidthClass.COMPACT -> 2
                                        WindowWidthClass.MEDIUM -> 3
                                        WindowWidthClass.EXPANDED -> if (selectedListing != null) 2 else 3
                                    }
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            item {
                                                FilterChip(
                                                    selected = rarityFilter == null,
                                                    onClick = { rarityFilter = null },
                                                    label = { Text("All") }
                                                )
                                            }
                                            items(DuckRarity.entries) { rarity ->
                                                FilterChip(
                                                    selected = rarityFilter == rarity,
                                                    onClick = {
                                                        rarityFilter = if (rarityFilter == rarity) null else rarity
                                                    },
                                                    label = { Text(rarity.label) }
                                                )
                                            }
                                        }
                                        if (browseItems.isEmpty()) {
                                            Box(modifier = Modifier.padding(16.dp)) {
                                                EmptyPondCard(
                                                    title = stringResource(R.string.empty_market_title),
                                                    subtitle = stringResource(R.string.empty_market_subtitle)
                                                )
                                            }
                                        } else {
                                            LazyVerticalGrid(
                                                columns = GridCells.Fixed(cols),
                                                contentPadding = PaddingValues(16.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                items(browseItems, key = { it.id }) { listing ->
                                                    ListingGridCard(
                                                        listing = listing,
                                                        isOwn = listing.sellerId == currentUserId,
                                                        onClick = { selectedListing = listing }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                1 -> {
                                    if (data.tradeOffers.isEmpty()) {
                                        Box(modifier = Modifier.padding(16.dp)) {
                                            EmptyPondCard(
                                                title = "No trade offers yet",
                                                subtitle = "Tap any duck marked for trade in the Marketplace to send an offer!"
                                            )
                                        }
                                    } else {
                                        LazyColumn(
                                            contentPadding = PaddingValues(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(data.tradeOffers, key = { it.id }) { trade ->
                                                TradeOfferCard(
                                                    trade = trade,
                                                    currentUserId = currentUserId,
                                                    isBusy = isBusy,
                                                    onAccept = { onAcceptTrade(trade) },
                                                    onDecline = { onDeclineTrade(trade) },
                                                    onCancel = { onCancelTrade(trade) }
                                                )
                                            }
                                        }
                                    }
                                }

                                2 -> {
                                    if (data.myListings.isEmpty()) {
                                        Box(modifier = Modifier.padding(16.dp)) {
                                            EmptyPondCard(
                                                title = "No listings posted",
                                                subtitle = "Open any duck in your Pond tab and tap List on Marketplace."
                                            )
                                        }
                                    } else {
                                        LazyColumn(
                                            contentPadding = PaddingValues(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(data.myListings, key = { it.id }) { listing ->
                                                MyListingRow(
                                                    listing = listing,
                                                    isBusy = isBusy,
                                                    onCancel = { onCancelListing(listing) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(visible = isSplitPane && selectedListing != null && selectedTab == 0) {
                            val activeItem = data.activeListings.firstOrNull { it.id == selectedListing?.id }
                            if (activeItem != null) {
                                Surface(
                                    tonalElevation = 3.dp,
                                    shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
                                    modifier = Modifier
                                        .width(340.dp)
                                        .fillMaxHeight()
                                ) {
                                    ListingDetailPane(
                                        listing = activeItem,
                                        currentUserId = currentUserId,
                                        myCoins = data.wallet?.coins ?: 0L,
                                        isBusy = isBusy,
                                        onClose = { selectedListing = null },
                                        onBuy = {
                                            onBuyListing(activeItem)
                                            selectedListing = null
                                        },
                                        onProposeTrade = { tradeTargetListing = activeItem },
                                        onCancelListing = {
                                            onCancelListing(activeItem)
                                            selectedListing = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListingGridCard(
    listing: MarketplaceListing,
    isOwn: Boolean,
    onClick: () -> Unit
) {
    val rarity = DuckRarity.fromId(listing.duckRarity)
    RarityDuckCardContainer(
        rarity = rarity,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("listing_card_${listing.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DuckCanvas(
                palette = DuckPalette.fromMap(listing.duckPalette),
                size = 104.dp,
                cornerRadius = 18.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = listing.duckName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isOwn) "Your listing" else "by ${listing.sellerName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RarityBadge(rarity = rarity)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    GrapeClusterIcon(
                        modifier = Modifier.size(16.dp),
                        grapeColor = MaterialTheme.colorScheme.primary,
                        highlightColor = MaterialTheme.colorScheme.primaryContainer,
                        leafColor = MaterialTheme.colorScheme.secondary,
                        contentDesc = null
                    )
                    Text(
                        text = "${listing.askingCoins}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ListingDetailPane(
    listing: MarketplaceListing,
    currentUserId: String,
    myCoins: Long,
    isBusy: Boolean,
    onClose: () -> Unit,
    onBuy: () -> Unit,
    onProposeTrade: () -> Unit,
    onCancelListing: () -> Unit
) {
    val isMine = listing.sellerId == currentUserId
    val canAfford = myCoins >= listing.askingCoins
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RarityBadge(rarity = DuckRarity.fromId(listing.duckRarity))
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Close listing detail")
            }
        }
        DuckCanvas(
            palette = DuckPalette.fromMap(listing.duckPalette),
            size = 152.dp,
            cornerRadius = 26.dp
        )
        Text(
            text = listing.duckName,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Seller: ${listing.sellerName} • Serial #${listing.duckSerialNumber}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (listing.wantsTrade) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "Open to trades (${listing.desiredRarity.replaceFirstChar { it.uppercase() }} rarity)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        if (isMine) {
            OutlinedButton(
                onClick = onCancelListing,
                enabled = !isBusy,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Cancel Listing")
            }
        } else {
            Button(
                onClick = onBuy,
                enabled = !isBusy && canAfford,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("buy_listing_button")
            ) {
                Icon(Icons.Rounded.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (canAfford) "Buy for ${listing.askingCoins} Grapes" else "Need ${listing.askingCoins} Grapes")
            }
            if (listing.wantsTrade) {
                OutlinedButton(
                    onClick = onProposeTrade,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("propose_trade_button")
                ) {
                    Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Propose Duck Trade")
                }
            }
        }
    }
}

@Composable
private fun TradeOfferCard(
    trade: TradeOffer,
    currentUserId: String,
    isBusy: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCancel: () -> Unit
) {
    val isIncoming = trade.recipientId == currentUserId
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isIncoming) "From ${trade.senderName}" else "To ${trade.recipientName}",
                    style = MaterialTheme.typography.titleMedium
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = when (trade.status) {
                        "pending" -> MaterialTheme.colorScheme.secondaryContainer
                        "accepted" -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = trade.status.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DuckCanvas(
                        palette = DuckPalette.fromMap(trade.offeredDuckPalette),
                        size = 74.dp,
                        cornerRadius = 16.dp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(trade.offeredDuckName, style = MaterialTheme.typography.labelMedium)
                    if (trade.offeredCoins > 0L) {
                        Text(
                            "+${trade.offeredCoins} Grapes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Rounded.SwapHoriz,
                    contentDescription = "Swap for",
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DuckCanvas(
                        palette = DuckPalette.fromMap(trade.requestedDuckPalette),
                        size = 74.dp,
                        cornerRadius = 16.dp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(trade.requestedDuckName, style = MaterialTheme.typography.labelMedium)
                }
            }

            if (trade.note.isNotBlank()) {
                Text(
                    text = "\"${trade.note}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (trade.status == "pending") {
                if (isIncoming) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onDecline,
                            enabled = !isBusy,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("decline_trade_${trade.id}")
                        ) {
                            Text("Decline")
                        }
                        Button(
                            onClick = onAccept,
                            enabled = !isBusy,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("accept_trade_${trade.id}")
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Accept")
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onCancel,
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Cancel Offer")
                    }
                }
            }
        }
    }
}

@Composable
private fun MyListingRow(
    listing: MarketplaceListing,
    isBusy: Boolean,
    onCancel: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DuckCanvas(
                palette = DuckPalette.fromMap(listing.duckPalette),
                size = 64.dp,
                cornerRadius = 16.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(listing.duckName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${listing.askingCoins} Grapes • ${listing.status.replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (listing.status == "active") {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isBusy,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Unlist")
                }
            }
        }
    }
}

@Composable
private fun ProposeTradeDialog(
    targetListing: MarketplaceListing,
    myAvailableDucks: List<RubberDuck>,
    myCoins: Long,
    onDismiss: () -> Unit,
    onSubmit: (RubberDuck, Long, String) -> Unit
) {
    var selectedDuck by remember { mutableStateOf(myAvailableDucks.firstOrNull()) }
    var bonusCoinsText by remember { mutableStateOf("0") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Offer Trade for ${targetListing.duckName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (myAvailableDucks.isEmpty()) {
                    Text(
                        text = "You have no unlisted ducks available right now. Unlist a duck or hatch a new one first!",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text("Choose a duck from your pond:", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(myAvailableDucks, key = { it.id }) { duck ->
                            val isChosen = selectedDuck?.id == duck.id
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isChosen) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.clickable { selectedDuck = duck }
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    DuckCanvas(
                                        palette = DuckPalette.fromMap(duck.palette),
                                        size = 56.dp,
                                        cornerRadius = 12.dp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        duck.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = bonusCoinsText,
                        onValueChange = { if (it.all(Char::isDigit)) bonusCoinsText = it },
                        label = { Text("Bonus Grapes (You have $myCoins)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { if (it.length <= 140) note = it },
                        label = { Text("Friendly note (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (myAvailableDucks.isNotEmpty()) {
                val bonus = bonusCoinsText.toLongOrNull() ?: 0L
                Button(
                    onClick = {
                        selectedDuck?.let { onSubmit(it, bonus, note) }
                    },
                    enabled = selectedDuck != null && bonus <= myCoins,
                    modifier = Modifier.testTag("submit_trade_offer_button")
                ) {
                    Text("Send Offer")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
