package com.example.data.firestore

import android.content.Context
import com.example.R
import com.example.domain.model.CoinClaim
import com.example.domain.model.CollectorProfile
import com.example.domain.model.DuckPalette
import com.example.domain.model.DuckSpecies
import com.example.domain.model.DuckSpeciesCatalog
import com.example.domain.model.PondNotification
import com.example.domain.model.RubberDuck
import com.example.domain.model.Wallet
import com.example.domain.repository.CollectorRepository
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

class FirestoreCollectorRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : CollectorRepository {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUid: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        if (expectedUid != null) {
            check(uid == expectedUid) { "Signed-in user changed; refusing cross-user write." }
        }
        return uid
    }

    override suspend fun ensureInitialized(userId: String, defaultDisplayName: String): Result<Unit> {
        val safeName = defaultDisplayName.trim().ifBlank { "Pond Collector" }.take(40)
        return try {
            val uid = requireUserId(userId)
            val userRef = db.collection("users").document(uid)
            val walletRef = db.collection("wallets").document(uid)

            val userSnap = userRef.get().await()
            val walletSnap = walletRef.get().await()

            val batch = db.batch()
            var needsStarterDucks = false

            if (!userSnap.exists()) {
                needsStarterDucks = true
                batch.set(
                    userRef,
                    mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "bio" to "Collecting happy rubber ducks across Duck Land :)",
                        "avatarConfig" to DuckPalette(
                            bodyColor = "sunny_yellow",
                            billColor = "coral_orange",
                            hat = "sprout",
                            eyewear = "shades",
                            accessory = "bowtie",
                            pondBg = "grape_pond"
                        ).toMap(),
                        "duckCount" to 0L,
                        "tradesCompleted" to 0L,
                        "banned" to false,
                        "banReason" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else if (!userSnap.contains("banned")) {
                batch.update(
                    userRef,
                    mapOf(
                        "banned" to false,
                        "banReason" to "",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            if (!walletSnap.exists()) {
                batch.set(
                    walletRef,
                    mapOf(
                        "userId" to uid,
                        "coins" to 250L,
                        "lastClaimId" to "",
                        "lastClaimType" to "none",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            if (!userSnap.exists() || !userSnap.contains("banned") || !walletSnap.exists()) {
                batch.commit().await()
            }

            if (needsStarterDucks) {
                val existingDucks = db.collection("ducks")
                    .whereEqualTo("ownerId", uid)
                    .limit(1)
                    .get()
                    .await()
                if (existingDucks.isEmpty) {
                    val starterSpecies = listOf(
                        DuckSpeciesCatalog.byId("sunny_classic") to 101L,
                        DuckSpeciesCatalog.byId("mint_sprout") to 204L,
                        DuckSpeciesCatalog.byId("sailor_quack") to 308L
                    )
                    val starterBatch = db.batch()
                    starterSpecies.forEach { (species, serial) ->
                        val duckRef = db.collection("ducks").document()
                        starterBatch.set(
                            duckRef,
                            mapOf(
                                "ownerId" to uid,
                                "speciesId" to species.speciesId,
                                "name" to species.defaultName,
                                "rarity" to species.rarity.id,
                                "serialNumber" to serial,
                                "palette" to species.defaultPalette.toMap(),
                                "listed" to false,
                                "listingId" to "",
                                "lastTradeId" to "",
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    starterBatch.update(
                        userRef,
                        mapOf(
                            "duckCount" to starterSpecies.size.toLong(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    val notifRef = userRef.collection("notifications").document()
                    starterBatch.set(
                        notifRef,
                        mapOf(
                            "userId" to uid,
                            "type" to "event_started",
                            "title" to "Golden Splash Gala is live!",
                            "body" to "Welcome to Duck Land! Submit one of your starter ducks to this week's pond showcase.",
                            "referenceId" to "week_golden_splash",
                            "read" to false,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    starterBatch.commit().await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$userId")
            Result.failure(e)
        }
    }

    override fun observeProfile(userId: String): Flow<CollectorProfile?> = flow {
        if (userId.isBlank() || auth.currentUser?.uid != userId) {
            emit(null)
            return@flow
        }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { it.toObject(CollectorProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(root, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    override fun observeWallet(userId: String): Flow<Wallet?> = flow {
        if (userId.isBlank() || auth.currentUser?.uid != userId) {
            emit(null)
            return@flow
        }
        val path = "wallets/$userId"
        emitAll(
            db.collection("wallets").document(userId)
                .snapshots()
                .map { it.toObject(Wallet::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(root, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    override fun observeMyDucks(userId: String): Flow<List<RubberDuck>> = flow {
        if (userId.isBlank() || auth.currentUser?.uid != userId) {
            emit(emptyList())
            return@flow
        }
        val path = "ducks"
        emitAll(
            db.collection(path)
                .whereEqualTo("ownerId", userId)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(RubberDuck::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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

    override fun observeNotifications(userId: String): Flow<List<PondNotification>> = flow {
        if (userId.isBlank() || auth.currentUser?.uid != userId) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/notifications"
        emitAll(
            db.collection("users").document(userId).collection("notifications")
                .limit(40)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(PondNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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

    override fun observePendingClaims(userId: String): Flow<List<CoinClaim>> = flow {
        if (userId.isBlank() || auth.currentUser?.uid != userId) {
            emit(emptyList())
            return@flow
        }
        val path = "coin_claims"
        emitAll(
            db.collection(path)
                .whereEqualTo("recipientId", userId)
                .whereEqualTo("claimed", false)
                .limit(20)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CoinClaim::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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

    override suspend fun hatchDuck(
        userId: String,
        currentCoins: Long,
        cost: Long,
        species: DuckSpecies,
        serialNumber: Long
    ): Result<RubberDuck> {
        val path = "ducks"
        return try {
            val uid = requireUserId(userId)
            require(cost > 0L && currentCoins >= cost) { "Not enough Grapes to hatch this egg." }
            val duckId = "duck_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val duckRef = db.collection("ducks").document(duckId)
            val walletRef = db.collection("wallets").document(uid)
            val userRef = db.collection("users").document(uid)

            val batch = db.batch()
            batch.set(
                duckRef,
                mapOf(
                    "ownerId" to uid,
                    "speciesId" to species.speciesId,
                    "name" to species.defaultName,
                    "rarity" to species.rarity.id,
                    "serialNumber" to serialNumber,
                    "palette" to species.defaultPalette.toMap(),
                    "listed" to false,
                    "listingId" to "",
                    "lastTradeId" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                walletRef,
                mapOf(
                    "coins" to (currentCoins - cost),
                    "lastClaimId" to "",
                    "lastClaimType" to "none",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                userRef,
                mapOf(
                    "duckCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            Result.success(
                RubberDuck(
                    id = duckId,
                    ownerId = uid,
                    speciesId = species.speciesId,
                    name = species.defaultName,
                    rarity = species.rarity.id,
                    serialNumber = serialNumber,
                    palette = species.defaultPalette.toMap()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun claimDailyBonus(userId: String, currentCoins: Long): Result<Unit> {
        val path = "wallets/$userId"
        return try {
            val uid = requireUserId(userId)
            val walletRef = db.collection("wallets").document(uid)
            walletRef.update(
                mapOf(
                    "coins" to (currentCoins + 75L),
                    "lastClaimId" to "",
                    "lastClaimType" to "daily",
                    "lastDailyClaimAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun claimPendingCoins(userId: String, currentCoins: Long, claim: CoinClaim): Result<Unit> {
        val path = "coin_claims/${claim.id}"
        return try {
            val uid = requireUserId(userId)
            require(claim.id.isNotBlank() && !claim.claimed && claim.recipientId == uid)
            val claimRef = db.collection("coin_claims").document(claim.id)
            val walletRef = db.collection("wallets").document(uid)

            val batch = db.batch()
            batch.update(
                claimRef,
                mapOf(
                    "claimed" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                walletRef,
                mapOf(
                    "coins" to (currentCoins + claim.amount),
                    "lastClaimId" to claim.id,
                    "lastClaimType" to "sale",
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

    override suspend fun updateProfileAndAvatar(
        userId: String,
        displayName: String,
        bio: String,
        avatar: DuckPalette
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val trimmedName = displayName.trim().ifBlank { "Pond Collector" }.take(40)
            val trimmedBio = bio.trim().take(160)
            db.collection("users").document(uid).update(
                mapOf(
                    "displayName" to trimmedName,
                    "bio" to trimmedBio,
                    "avatarConfig" to avatar.toMap(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun renameDuck(userId: String, duckId: String, newName: String): Result<Unit> {
        val path = "ducks/$duckId"
        return try {
            requireUserId(userId)
            val cleanName = newName.trim()
            require(cleanName.length in 1..40) { "Duck name must be between 1 and 40 characters." }
            db.collection("ducks").document(duckId).update(
                mapOf(
                    "name" to cleanName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun markNotificationRead(userId: String, notificationId: String): Result<Unit> {
        val path = "users/$userId/notifications/$notificationId"
        return try {
            val uid = requireUserId(userId)
            db.collection("users").document(uid)
                .collection("notifications").document(notificationId)
                .update("read", true)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun markAllNotificationsRead(userId: String, unreadIds: List<String>): Result<Unit> {
        if (unreadIds.isEmpty()) return Result.success(Unit)
        val path = "users/$userId/notifications"
        return try {
            val uid = requireUserId(userId)
            val batch = db.batch()
            unreadIds.take(40).forEach { id ->
                val ref = db.collection("users").document(uid)
                    .collection("notifications").document(id)
                batch.update(ref, "read", true)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
