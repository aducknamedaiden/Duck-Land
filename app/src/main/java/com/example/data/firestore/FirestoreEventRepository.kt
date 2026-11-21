package com.example.data.firestore

import android.content.Context
import com.example.R
import com.example.domain.model.CollectorProfile
import com.example.domain.model.EventEntry
import com.example.domain.model.RubberDuck
import com.example.domain.model.WeeklyEvent
import com.example.domain.repository.EventRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreEventRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : EventRepository {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUid: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before joining events.")
        if (expectedUid != null) {
            check(uid == expectedUid) { "Signed-in user mismatch." }
        }
        return uid
    }

    override suspend fun ensureSeededEvents(): Result<Unit> {
        val path = "weekly_events"
        return try {
            requireUserId()
            val seeds = listOf(
                Triple(
                    "week_golden_splash",
                    mapOf(
                        "title" to "Golden Splash Gala",
                        "themeTag" to "Week 47 • Gala",
                        "description" to "Show off your shiniest, crown-wearing, or golden-hour pond ducks for community cheers!",
                        "bonusRarity" to "legendary",
                        "active" to true,
                        "weekNumber" to 47L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    true
                ),
                Triple(
                    "week_rainy_parade",
                    mapOf(
                        "title" to "Cozy Rainy Day Parade",
                        "themeTag" to "Week 46 • Cozy",
                        "description" to "Beanies, scarves, and pastel umbrellas — celebrate your coziest pond companions.",
                        "bonusRarity" to "rare",
                        "active" to false,
                        "weekNumber" to 46L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    false
                ),
                Triple(
                    "week_neon_swim",
                    mapOf(
                        "title" to "Starlight Night Swim",
                        "themeTag" to "Week 48 • Preview",
                        "description" to "Upcoming midnight glow showcase for starry specs and celestial ducklings.",
                        "bonusRarity" to "epic",
                        "active" to false,
                        "weekNumber" to 48L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    false
                )
            )
            seeds.forEach { (id, payload, _) ->
                val ref = db.collection("weekly_events").document(id)
                val snap = ref.get().await()
                if (!snap.exists()) {
                    ref.set(payload).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override fun observeEvents(): Flow<List<WeeklyEvent>> = flow {
        if (auth.currentUser?.uid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "weekly_events"
        emitAll(
            db.collection(path)
                .whereGreaterThanOrEqualTo("weekNumber", 1L)
                .limit(10)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(WeeklyEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareByDescending<WeeklyEvent> { it.active }.thenByDescending { it.weekNumber })
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

    override fun observeEventEntries(eventId: String): Flow<List<EventEntry>> = flow {
        if (eventId.isBlank() || auth.currentUser?.uid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "weekly_events/$eventId/entries"
        emitAll(
            db.collection("weekly_events").document(eventId).collection("entries")
                .whereEqualTo("eventId", eventId)
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(EventEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareByDescending<EventEntry> { it.voteCount }
                                .thenByDescending { it.createdAt?.seconds ?: 0L }
                        )
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

    override suspend fun fetchVotedEntryIds(
        eventId: String,
        userId: String,
        entryIds: List<String>
    ): Set<String> {
        if (eventId.isBlank() || userId.isBlank() || entryIds.isEmpty()) return emptySet()
        return try {
            requireUserId(userId)
            coroutineScope {
                entryIds.take(50).map { entryId ->
                    async {
                        val voteSnap = db.collection("weekly_events").document(eventId)
                            .collection("entries").document(entryId)
                            .collection("votes").document(userId)
                            .get()
                            .await()
                        if (voteSnap.exists()) entryId else null
                    }
                }.awaitAll().filterNotNull().toSet()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "weekly_events/$eventId/entries")
            emptySet()
        }
    }

    override suspend fun submitEntry(
        userId: String,
        profile: CollectorProfile,
        eventId: String,
        duck: RubberDuck,
        caption: String
    ): Result<Unit> {
        val path = "weekly_events/$eventId/entries"
        return try {
            val uid = requireUserId(userId)
            val cleanCaption = caption.trim()
            require(cleanCaption.length in 1..120) { "Caption must be between 1 and 120 characters." }

            val entryRef = db.collection("weekly_events").document(eventId)
                .collection("entries").document()
            entryRef.set(
                mapOf(
                    "eventId" to eventId,
                    "userId" to uid,
                    "ownerDisplayName" to profile.displayName.ifBlank { "Collector" }.take(40),
                    "ownerAvatar" to profile.avatarConfig,
                    "duckId" to duck.id,
                    "duckName" to duck.name,
                    "duckSpeciesId" to duck.speciesId,
                    "duckRarity" to duck.rarity,
                    "duckPalette" to duck.palette,
                    "caption" to cleanCaption,
                    "voteCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun voteForEntry(
        userId: String,
        voterName: String,
        eventId: String,
        entry: EventEntry
    ): Result<Long> {
        val entryId = entry.id
        val path = "weekly_events/$eventId/entries/$entryId/votes/$userId"
        return try {
            val uid = requireUserId(userId)
            require(entry.userId != uid) { "You can vote for other collectors' ducks!" }

            val entryRef = db.collection("weekly_events").document(eventId)
                .collection("entries").document(entryId)
            val voteRef = entryRef.collection("votes").document(uid)
            val walletRef = db.collection("wallets").document(uid)

            val existingVote = voteRef.get().await()
            if (existingVote.exists()) {
                return Result.failure(IllegalStateException("You already voted for this duck!"))
            }

            val walletSnap = walletRef.get().await()
            val currentCoins = walletSnap.getLong("coins") ?: 250L
            val voterBonus = 15L
            val creatorBonus = 25L
            val claimRef = db.collection("coin_claims").document("vote_${entryId}_${uid}")
            val notificationRef = db.collection("users").document(entry.userId)
                .collection("notifications").document()
            val cleanVoterName = voterName.trim().ifBlank { "A collector" }.take(40)
            val notifTitle = "Showcase Vote! +25 Grapes".take(80)
            val notifBody = "$cleanVoterName voted for ${entry.duckName}! +25 Bonus Grapes are ready to claim.".take(200)

            val batch = db.batch()
            batch.set(
                voteRef,
                mapOf(
                    "voterId" to uid,
                    "entryId" to entryId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                entryRef,
                mapOf(
                    "voteCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                walletRef,
                mapOf(
                    "coins" to currentCoins + voterBonus,
                    "lastClaimId" to "${eventId}__${entryId}",
                    "lastClaimType" to "showcase_vote",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                claimRef,
                mapOf(
                    "recipientId" to entry.userId,
                    "sourceId" to entryId,
                    "sourceType" to "showcase_vote_reward",
                    "eventId" to eventId,
                    "amount" to creatorBonus,
                    "claimed" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                notificationRef,
                mapOf(
                    "userId" to entry.userId,
                    "type" to "showcase_vote",
                    "title" to notifTitle,
                    "body" to notifBody,
                    "referenceId" to eventId,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(voterBonus)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
