package com.example.data.firestore

import android.content.Context
import com.example.R
import com.example.domain.model.CollectorProfile
import com.example.domain.model.DuckPalette
import com.example.domain.model.DuckRarity
import com.example.domain.model.DuckSpecies
import com.example.domain.model.PromoCodeCatalog
import com.example.domain.model.PromoReward
import com.example.domain.model.RedeemedCode
import com.example.domain.model.RubberDuck
import com.example.domain.model.WeeklyEvent
import com.example.domain.repository.OwnerRepository
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
import kotlin.random.Random

class FirestoreOwnerRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : OwnerRepository {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUid: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before performing this action.")
        if (expectedUid != null) {
            check(uid == expectedUid) { "Signed-in user changed; refusing cross-user write." }
        }
        return uid
    }

    override fun observeIsOwner(userId: String): Flow<Boolean> = flow {
        if (userId.isBlank() || auth.currentUser?.uid != userId) {
            emit(false)
            return@flow
        }
        val path = "admins/$userId"
        val emailOwner = auth.currentUser?.let { user ->
            user.isEmailVerified && user.email.equals(OWNER_EMAIL, ignoreCase = true)
        } == true

        emitAll(
            db.collection("admins").document(userId)
                .snapshots()
                .map { snapshot -> snapshot.exists() || emailOwner }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(root, OperationType.GET, path)
                    }
                    emit(emailOwner)
                }
        )
    }

    override fun observeRedeemedCodes(userId: String): Flow<Set<String>> = flow {
        if (userId.isBlank() || auth.currentUser?.uid != userId) {
            emit(emptySet())
            return@flow
        }
        val path = "users/$userId/redeemed_codes"
        emitAll(
            db.collection("users").document(userId)
                .collection("redeemed_codes")
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(RedeemedCode::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .mapTo(linkedSetOf()) { it.code.uppercase() }
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

    override fun observeAllCollectors(): Flow<List<CollectorProfile>> = flow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            emit(emptyList())
            return@flow
        }
        val path = "users"
        emitAll(
            db.collection(path)
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CollectorProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareByDescending<CollectorProfile> { it.userId == uid }
                                .thenBy { it.displayName.lowercase() }
                        )
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    handleFirestoreError(root, OperationType.LIST, path)
                    emit(emptyList())
                }
        )
    }

    override suspend fun redeemCode(
        userId: String,
        rawCode: String,
        currentCoins: Long
    ): Result<PromoReward> {
        val definition = PromoCodeCatalog.lookup(rawCode)
            ?: return Result.failure(IllegalArgumentException("Unknown Duck Land code. Double check spelling!"))

        return try {
            val uid = requireUserId(userId)
            when (val reward = definition.reward) {
                is PromoReward.OwnerUnlockReward -> {
                    val adminRef = db.collection("admins").document(uid)
                    val existing = adminRef.get().await()
                    if (!existing.exists()) {
                        adminRef.set(
                            mapOf(
                                "userId" to uid,
                                "ownerCode" to PromoCodeCatalog.OWNER_CODE,
                                "grantedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                    Result.success(PromoReward.OwnerUnlockReward)
                }

                is PromoReward.CoinReward -> {
                    val codeRef = db.collection("users").document(uid)
                        .collection("redeemed_codes").document(definition.code)
                    val existing = codeRef.get().await()
                    if (existing.exists()) {
                        return Result.failure(IllegalStateException("You already redeemed ${definition.code}!"))
                    }
                    val walletRef = db.collection("wallets").document(uid)
                    val batch = db.batch()
                    batch.set(
                        codeRef,
                        mapOf(
                            "userId" to uid,
                            "code" to definition.code,
                            "rewardSummary" to reward.summary,
                            "redeemedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    batch.update(
                        walletRef,
                        mapOf(
                            "coins" to (currentCoins + reward.coins).coerceAtMost(MAX_COINS),
                            "lastClaimId" to definition.code,
                            "lastClaimType" to "promo",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    batch.commit().await()
                    Result.success(reward)
                }

                is PromoReward.DuckReward -> {
                    val codeRef = db.collection("users").document(uid)
                        .collection("redeemed_codes").document(definition.code)
                    val existing = codeRef.get().await()
                    if (existing.exists()) {
                        return Result.failure(IllegalStateException("You already redeemed ${definition.code}!"))
                    }
                    val duckId = "promo_${definition.code.lowercase()}_${UUID.randomUUID().toString().replace("-", "").take(10)}"
                    val serial = Random.nextLong(500L, 9999L)
                    val duckRef = db.collection("ducks").document(duckId)
                    val userRef = db.collection("users").document(uid)

                    val batch = db.batch()
                    batch.set(
                        codeRef,
                        mapOf(
                            "userId" to uid,
                            "code" to definition.code,
                            "rewardSummary" to reward.summary,
                            "redeemedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    batch.set(
                        duckRef,
                        mapOf(
                            "ownerId" to uid,
                            "speciesId" to reward.species.speciesId,
                            "name" to reward.customName,
                            "rarity" to reward.rarity.id,
                            "serialNumber" to serial,
                            "palette" to reward.species.defaultPalette.toMap(),
                            "listed" to false,
                            "listingId" to "",
                            "lastTradeId" to "",
                            "createdAt" to FieldValue.serverTimestamp(),
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

                    val grantedDuck = RubberDuck(
                        id = duckId,
                        ownerId = uid,
                        speciesId = reward.species.speciesId,
                        name = reward.customName,
                        rarity = reward.rarity.id,
                        serialNumber = serial,
                        palette = reward.species.defaultPalette.toMap()
                    )
                    Result.success(reward.copy(duck = grantedDuck))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "promo/${definition.code}")
            Result.failure(e)
        }
    }

    override suspend fun adminGrantDuck(
        targetUserId: String,
        species: DuckSpecies,
        customName: String,
        rarityOverride: DuckRarity,
        customPalette: DuckPalette,
        serialNumber: Long
    ): Result<RubberDuck> {
        val path = "ducks"
        return try {
            val adminUid = requireUserId()
            val targetUid = targetUserId.trim().ifBlank { adminUid }
            val safeName = customName.trim().ifBlank { species.defaultName }.take(40)
            val safeSerial = serialNumber.coerceIn(1L, 999_999_999L)
            val duckId = "owner_duck_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            val duckRef = db.collection("ducks").document(duckId)
            val userRef = db.collection("users").document(targetUid)

            val batch = db.batch()
            batch.set(
                duckRef,
                mapOf(
                    "ownerId" to targetUid,
                    "speciesId" to species.speciesId,
                    "name" to safeName,
                    "rarity" to rarityOverride.id,
                    "serialNumber" to safeSerial,
                    "palette" to customPalette.toMap(),
                    "listed" to false,
                    "listingId" to "",
                    "lastTradeId" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
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
            if (targetUid != adminUid) {
                val notifRef = userRef.collection("notifications").document()
                batch.set(
                    notifRef,
                    mapOf(
                        "userId" to targetUid,
                        "type" to "admin_announcement",
                        "title" to "Owner Gift Received!",
                        "body" to "The Duck Land Owner gifted you ${rarityOverride.label} $safeName (#$safeSerial)!",
                        "referenceId" to duckId,
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()

            Result.success(
                RubberDuck(
                    id = duckId,
                    ownerId = targetUid,
                    speciesId = species.speciesId,
                    name = safeName,
                    rarity = rarityOverride.id,
                    serialNumber = safeSerial,
                    palette = customPalette.toMap()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun adminSetOrAddCoins(
        targetUserId: String,
        amount: Long,
        isAdd: Boolean
    ): Result<Long> {
        val targetUid = targetUserId.trim()
        val path = "wallets/$targetUid"
        return try {
            requireUserId()
            val walletRef = db.collection("wallets").document(targetUid)
            val newTotal = if (isAdd) {
                val current = walletRef.get().await().getLong("coins") ?: 0L
                (current + amount).coerceIn(0L, MAX_COINS)
            } else {
                amount.coerceIn(0L, MAX_COINS)
            }
            walletRef.update(
                mapOf(
                    "coins" to newTotal,
                    "lastClaimId" to "admin_${System.currentTimeMillis()}",
                    "lastClaimType" to "admin_grant",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(newTotal)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun adminSetBanStatus(
        targetUserId: String,
        banned: Boolean,
        reason: String
    ): Result<Unit> {
        val targetUid = targetUserId.trim()
        val path = "users/$targetUid"
        return try {
            val adminUid = requireUserId()
            require(targetUid != adminUid) { "Owner cannot ban their own account." }
            val cleanReason = if (banned) {
                reason.trim().ifBlank { "Suspended by Duck Land Owner" }.take(200)
            } else {
                ""
            }
            db.collection("users").document(targetUid).update(
                mapOf(
                    "banned" to banned,
                    "banReason" to cleanReason,
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

    override suspend fun adminSendAnnouncement(
        targetUserId: String,
        title: String,
        body: String
    ): Result<Unit> {
        val targetUid = targetUserId.trim()
        val path = "users/$targetUid/notifications"
        return try {
            requireUserId()
            val safeTitle = title.trim().ifBlank { "Owner Pond Announcement" }.take(80)
            val safeBody = body.trim().ifBlank { "Greetings from the Duck Land Owner!" }.take(200)
            db.collection("users").document(targetUid)
                .collection("notifications").document()
                .set(
                    mapOf(
                        "userId" to targetUid,
                        "type" to "admin_announcement",
                        "title" to safeTitle,
                        "body" to safeBody,
                        "referenceId" to "owner_announcement",
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun adminActivateWeeklyEvent(
        allEvents: List<WeeklyEvent>,
        eventIdToActivate: String
    ): Result<Unit> {
        val path = "weekly_events/$eventIdToActivate"
        return try {
            requireUserId()
            val batch = db.batch()
            allEvents.forEach { ev ->
                if (ev.id.isNotBlank()) {
                    val shouldBeActive = ev.id == eventIdToActivate
                    if (ev.active != shouldBeActive) {
                        batch.update(
                            db.collection("weekly_events").document(ev.id),
                            mapOf(
                                "active" to shouldBeActive,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
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

    override suspend fun adminCreateWeeklyEvent(
        title: String,
        themeTag: String,
        description: String,
        bonusRarity: String,
        weekNumber: Long,
        activateImmediately: Boolean,
        existingEvents: List<WeeklyEvent>
    ): Result<String> {
        val eventId = "event_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "weekly_events/$eventId"
        return try {
            requireUserId()
            val batch = db.batch()
            if (activateImmediately) {
                existingEvents.filter { it.active && it.id.isNotBlank() }.forEach { ev ->
                    batch.update(
                        db.collection("weekly_events").document(ev.id),
                        mapOf(
                            "active" to false,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.set(
                db.collection("weekly_events").document(eventId),
                mapOf(
                    "title" to title.trim().ifBlank { "Royal Duck Gala" }.take(60),
                    "themeTag" to themeTag.trim().ifBlank { "Week $weekNumber" }.take(30),
                    "description" to description.trim().ifBlank { "Special pond showcase curated by the Owner!" }.take(200),
                    "bonusRarity" to bonusRarity.ifBlank { "any" },
                    "active" to activateImmediately,
                    "weekNumber" to weekNumber.coerceIn(1L, 10000L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(eventId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    private companion object {
        const val OWNER_EMAIL = "aidenwerth27@gmail.com"
        const val MAX_COINS = 100_000_000L
    }
}
