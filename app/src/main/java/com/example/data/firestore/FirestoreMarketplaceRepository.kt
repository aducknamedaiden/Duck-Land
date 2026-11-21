package com.example.data.firestore

import android.content.Context
import com.example.R
import com.example.domain.model.CollectorProfile
import com.example.domain.model.MarketplaceListing
import com.example.domain.model.RubberDuck
import com.example.domain.model.TradeOffer
import com.example.domain.repository.MarketplaceRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreMarketplaceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : MarketplaceRepository {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUid: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before trading.")
        if (expectedUid != null) {
            check(uid == expectedUid) { "Signed-in user mismatch." }
        }
        return uid
    }

    override fun observeActiveListings(): Flow<List<MarketplaceListing>> = flow {
        if (auth.currentUser?.uid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "marketplace_listings"
        emitAll(
            db.collection(path)
                .whereEqualTo("status", "active")
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(MarketplaceListing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (auth.currentUser != null) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    override fun observeMyListings(userId: String): Flow<List<MarketplaceListing>> = flow {
        if (userId.isBlank() || auth.currentUser?.uid != userId) {
            emit(emptyList())
            return@flow
        }
        val path = "marketplace_listings"
        emitAll(
            db.collection(path)
                .whereEqualTo("sellerId", userId)
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(MarketplaceListing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    override fun observeTradeOffers(userId: String): Flow<List<TradeOffer>> = flow {
        if (userId.isBlank() || auth.currentUser?.uid != userId) {
            emit(emptyList())
            return@flow
        }
        val path = "trade_offers"
        emitAll(
            db.collection(path)
                .whereArrayContains("participantIds", userId)
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(TradeOffer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    override suspend fun createListing(
        userId: String,
        profile: CollectorProfile,
        duck: RubberDuck,
        askingCoins: Long,
        wantsTrade: Boolean,
        desiredRarity: String
    ): Result<Unit> {
        val listingId = "list_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "marketplace_listings/$listingId"
        return try {
            val uid = requireUserId(userId)
            require(!duck.listed) { "This duck is already listed on the marketplace." }
            require(askingCoins in 1L..1_000_000L) { "Asking Grapes must be between 1 and 1,000,000." }

            val listingRef = db.collection("marketplace_listings").document(listingId)
            val duckRef = db.collection("ducks").document(duck.id)

            val batch = db.batch()
            batch.set(
                listingRef,
                mapOf(
                    "sellerId" to uid,
                    "sellerName" to profile.displayName.ifBlank { "Collector" }.take(40),
                    "sellerAvatar" to profile.avatarConfig,
                    "buyerId" to "",
                    "duckId" to duck.id,
                    "duckName" to duck.name,
                    "duckSpeciesId" to duck.speciesId,
                    "duckRarity" to duck.rarity,
                    "duckSerialNumber" to duck.serialNumber,
                    "duckPalette" to duck.palette,
                    "askingCoins" to askingCoins,
                    "wantsTrade" to wantsTrade,
                    "desiredRarity" to desiredRarity,
                    "status" to "active",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                duckRef,
                mapOf(
                    "listed" to true,
                    "listingId" to listingId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun cancelListing(userId: String, listing: MarketplaceListing): Result<Unit> {
        val path = "marketplace_listings/${listing.id}"
        return try {
            val uid = requireUserId(userId)
            require(listing.sellerId == uid && listing.status == "active")
            val listingRef = db.collection("marketplace_listings").document(listing.id)
            val duckRef = db.collection("ducks").document(listing.duckId)

            val batch = db.batch()
            batch.update(
                listingRef,
                mapOf(
                    "status" to "cancelled",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                duckRef,
                mapOf(
                    "listed" to false,
                    "listingId" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun buyListing(
        buyerId: String,
        buyerName: String,
        buyerCoins: Long,
        listing: MarketplaceListing
    ): Result<Unit> {
        val path = "marketplace_listings/${listing.id}"
        return try {
            val uid = requireUserId(buyerId)
            require(listing.sellerId != uid) { "You already own this duck." }
            require(listing.status == "active") { "This listing is no longer active." }
            require(buyerCoins >= listing.askingCoins) { "Not enough Grapes for this duck." }

            val listingRef = db.collection("marketplace_listings").document(listing.id)
            val duckRef = db.collection("ducks").document(listing.duckId)
            val walletRef = db.collection("wallets").document(uid)
            val claimRef = db.collection("coin_claims").document("sale_${listing.id}")
            val sellerNotifRef = db.collection("users").document(listing.sellerId)
                .collection("notifications").document()

            val batch = db.batch()
            batch.update(
                listingRef,
                mapOf(
                    "status" to "sold",
                    "buyerId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                duckRef,
                mapOf(
                    "ownerId" to uid,
                    "listed" to false,
                    "listingId" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                walletRef,
                mapOf(
                    "coins" to (buyerCoins - listing.askingCoins),
                    "lastClaimId" to "",
                    "lastClaimType" to "none",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                claimRef,
                mapOf(
                    "recipientId" to listing.sellerId,
                    "sourceId" to listing.id,
                    "sourceType" to "listing_sale",
                    "amount" to listing.askingCoins,
                    "claimed" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                sellerNotifRef,
                mapOf(
                    "userId" to listing.sellerId,
                    "type" to "listing_sold",
                    "title" to "${listing.duckName} sold!",
                    "body" to "${buyerName.ifBlank { "A collector" }} adopted ${listing.duckName} for ${listing.askingCoins} Grapes.",
                    "referenceId" to listing.id,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun proposeTrade(
        senderId: String,
        senderName: String,
        senderCoins: Long,
        offeredDuck: RubberDuck,
        listing: MarketplaceListing,
        offeredCoins: Long,
        note: String
    ): Result<Unit> {
        val tradeId = "trade_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "trade_offers/$tradeId"
        return try {
            val uid = requireUserId(senderId)
            require(listing.sellerId != uid) { "You cannot trade with yourself." }
            require(!offeredDuck.listed) { "Unlist your offered duck before proposing it in a trade." }
            require(offeredCoins in 0L..100_000L) { "Bonus Grapes must be between 0 and 100,000." }
            require(senderCoins >= offeredCoins) { "Not enough Grapes for this trade bonus." }

            val tradeRef = db.collection("trade_offers").document(tradeId)
            val walletRef = db.collection("wallets").document(uid)
            val notifRef = db.collection("users").document(listing.sellerId)
                .collection("notifications").document()

            val batch = db.batch()
            batch.set(
                tradeRef,
                mapOf(
                    "senderId" to uid,
                    "senderName" to senderName.ifBlank { "Collector" }.take(40),
                    "recipientId" to listing.sellerId,
                    "recipientName" to listing.sellerName.ifBlank { "Seller" }.take(40),
                    "participantIds" to listOf(uid, listing.sellerId),
                    "listingId" to listing.id,
                    "offeredDuckId" to offeredDuck.id,
                    "offeredDuckName" to offeredDuck.name,
                    "offeredDuckRarity" to offeredDuck.rarity,
                    "offeredDuckPalette" to offeredDuck.palette,
                    "requestedDuckId" to listing.duckId,
                    "requestedDuckName" to listing.duckName,
                    "requestedDuckRarity" to listing.duckRarity,
                    "requestedDuckPalette" to listing.duckPalette,
                    "offeredCoins" to offeredCoins,
                    "note" to note.trim().take(140),
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (offeredCoins > 0L) {
                batch.update(
                    walletRef,
                    mapOf(
                        "coins" to (senderCoins - offeredCoins),
                        "lastClaimId" to "",
                        "lastClaimType" to "none",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.set(
                notifRef,
                mapOf(
                    "userId" to listing.sellerId,
                    "type" to "trade_received",
                    "title" to "New trade offer for ${listing.duckName}",
                    "body" to "${senderName.ifBlank { "A collector" }} offered ${offeredDuck.name}" +
                        if (offeredCoins > 0L) " + $offeredCoins Grapes." else ".",
                    "referenceId" to tradeId,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun acceptTrade(
        recipientId: String,
        recipientName: String,
        recipientCoins: Long,
        trade: TradeOffer,
        requestedDuckListingId: String
    ): Result<Unit> {
        val path = "trade_offers/${trade.id}"
        return try {
            val uid = requireUserId(recipientId)
            require(trade.recipientId == uid && trade.status == "pending") {
                "Only pending incoming trades can be accepted."
            }

            // Pre-flight ownership verification so stale double-trades return clear feedback
            val offeredSnap = db.collection("ducks").document(trade.offeredDuckId).get().await()
            val requestedSnap = db.collection("ducks").document(trade.requestedDuckId).get().await()
            if (offeredSnap.getString("ownerId") != trade.senderId) {
                return Result.failure(IllegalStateException("The offered duck was already traded or sold."))
            }
            if (requestedSnap.getString("ownerId") != uid) {
                return Result.failure(IllegalStateException("You no longer own the requested duck."))
            }

            val tradeRef = db.collection("trade_offers").document(trade.id)
            val offeredDuckRef = db.collection("ducks").document(trade.offeredDuckId)
            val requestedDuckRef = db.collection("ducks").document(trade.requestedDuckId)
            val walletRef = db.collection("wallets").document(uid)
            val senderNotifRef = db.collection("users").document(trade.senderId)
                .collection("notifications").document()

            val batch = db.batch()
            batch.update(
                tradeRef,
                mapOf(
                    "status" to "accepted",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                offeredDuckRef,
                mapOf(
                    "ownerId" to uid,
                    "listed" to false,
                    "listingId" to "",
                    "lastTradeId" to trade.id,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                requestedDuckRef,
                mapOf(
                    "ownerId" to trade.senderId,
                    "listed" to false,
                    "listingId" to "",
                    "lastTradeId" to trade.id,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val actualListingId = requestedSnap.getString("listingId").orEmpty().ifBlank { requestedDuckListingId }
            if (actualListingId.isNotBlank()) {
                val listingSnap = db.collection("marketplace_listings").document(actualListingId).get().await()
                if (listingSnap.exists() && listingSnap.getString("status") == "active") {
                    batch.update(
                        listingSnap.reference,
                        mapOf(
                            "status" to "cancelled",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            if (trade.offeredCoins > 0L) {
                batch.update(
                    walletRef,
                    mapOf(
                        "coins" to (recipientCoins + trade.offeredCoins),
                        "lastClaimId" to trade.id,
                        "lastClaimType" to "trade_accept",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.set(
                senderNotifRef,
                mapOf(
                    "userId" to trade.senderId,
                    "type" to "trade_accepted",
                    "title" to "Trade accepted!",
                    "body" to "${recipientName.ifBlank { "Collector" }} swapped ${trade.requestedDuckName} for your ${trade.offeredDuckName}.",
                    "referenceId" to trade.id,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun declineTrade(
        recipientId: String,
        recipientName: String,
        trade: TradeOffer
    ): Result<Unit> {
        val path = "trade_offers/${trade.id}"
        return try {
            val uid = requireUserId(recipientId)
            require(trade.recipientId == uid && trade.status == "pending")
            val tradeRef = db.collection("trade_offers").document(trade.id)
            val senderNotifRef = db.collection("users").document(trade.senderId)
                .collection("notifications").document()

            val batch = db.batch()
            batch.update(
                tradeRef,
                mapOf(
                    "status" to "declined",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (trade.offeredCoins > 0L) {
                val refundRef = db.collection("coin_claims").document("refund_${trade.id}")
                batch.set(
                    refundRef,
                    mapOf(
                        "recipientId" to trade.senderId,
                        "sourceId" to trade.id,
                        "sourceType" to "trade_refund",
                        "amount" to trade.offeredCoins,
                        "claimed" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.set(
                senderNotifRef,
                mapOf(
                    "userId" to trade.senderId,
                    "type" to "trade_declined",
                    "title" to "Trade offer declined",
                    "body" to "${recipientName.ifBlank { "Collector" }} declined the trade for ${trade.requestedDuckName}.",
                    "referenceId" to trade.id,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun cancelTrade(
        senderId: String,
        senderCoins: Long,
        trade: TradeOffer
    ): Result<Unit> {
        val path = "trade_offers/${trade.id}"
        return try {
            val uid = requireUserId(senderId)
            require(trade.senderId == uid && trade.status == "pending")
            val tradeRef = db.collection("trade_offers").document(trade.id)
            val walletRef = db.collection("wallets").document(uid)

            val batch = db.batch()
            batch.update(
                tradeRef,
                mapOf(
                    "status" to "cancelled",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (trade.offeredCoins > 0L) {
                batch.update(
                    walletRef,
                    mapOf(
                        "coins" to (senderCoins + trade.offeredCoins),
                        "lastClaimId" to trade.id,
                        "lastClaimType" to "trade_refund",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
