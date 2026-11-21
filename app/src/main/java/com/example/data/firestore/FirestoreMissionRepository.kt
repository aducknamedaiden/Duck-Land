package com.example.data.firestore

import android.content.Context
import com.example.R
import com.example.domain.model.DailyMissionCatalog
import com.example.domain.model.DailyMissionProgress
import com.example.domain.repository.MissionRepository
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

class FirestoreMissionRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : MissionRepository {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUid: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing missions.")
        if (expectedUid != null) {
            check(uid == expectedUid) { "Signed-in user changed; refusing cross-user write." }
        }
        return uid
    }

    override fun observeDailyMissions(
        userId: String,
        dateKey: String
    ): Flow<List<DailyMissionProgress>> = flow {
        if (userId.isBlank() || auth.currentUser?.uid != userId) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/daily_missions"
        val orderMap = DailyMissionCatalog.all.withIndex().associate { it.value.missionId to it.index }
        emitAll(
            db.collection("users").document(userId)
                .collection("daily_missions")
                .whereEqualTo("dateKey", dateKey)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DailyMissionProgress::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { orderMap[it.missionId] ?: Int.MAX_VALUE }
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

    override suspend fun ensureDailyMissionsInitialized(
        userId: String,
        dateKey: String
    ): Result<Unit> {
        val path = "users/$userId/daily_missions"
        return try {
            val uid = requireUserId(userId)
            val missionsCol = db.collection("users").document(uid).collection("daily_missions")
            val existingSnap = missionsCol
                .whereEqualTo("dateKey", dateKey)
                .get()
                .await()
            val existingById = existingSnap.documents.associateBy { it.id }

            val batch = db.batch()
            var hasWrites = false

            DailyMissionCatalog.all.forEach { def ->
                val docId = DailyMissionCatalog.docId(dateKey, def.missionId)
                val existingDoc = existingById[docId]
                val ref = missionsCol.document(docId)
                if (existingDoc == null || !existingDoc.exists()) {
                    val initialProgress =
                        if (def.missionId == DailyMissionCatalog.MISSION_VISIT_POND) def.target else 0L
                    batch.set(
                        ref,
                        mapOf(
                            "userId" to uid,
                            "missionId" to def.missionId,
                            "dateKey" to dateKey,
                            "title" to def.title,
                            "description" to def.description,
                            "progress" to initialProgress,
                            "target" to def.target,
                            "rewardCoins" to def.rewardCoins,
                            "claimed" to false,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    hasWrites = true
                } else if (def.missionId == DailyMissionCatalog.MISSION_VISIT_POND) {
                    val claimed = existingDoc.getBoolean("claimed") ?: false
                    val progress = existingDoc.getLong("progress") ?: 0L
                    if (!claimed && progress < def.target) {
                        batch.update(
                            ref,
                            mapOf(
                                "progress" to def.target,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        hasWrites = true
                    }
                }
            }

            if (hasWrites) {
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun recordMissionProgress(
        userId: String,
        dateKey: String,
        missionId: String
    ): Result<Unit> {
        val def = DailyMissionCatalog.byId(missionId) ?: return Result.success(Unit)
        val docId = DailyMissionCatalog.docId(dateKey, missionId)
        val path = "users/$userId/daily_missions/$docId"
        return try {
            val uid = requireUserId(userId)
            val ref = db.collection("users").document(uid)
                .collection("daily_missions").document(docId)
            val snap = ref.get().await()
            if (!snap.exists()) {
                ref.set(
                    mapOf(
                        "userId" to uid,
                        "missionId" to def.missionId,
                        "dateKey" to dateKey,
                        "title" to def.title,
                        "description" to def.description,
                        "progress" to 1L.coerceAtMost(def.target),
                        "target" to def.target,
                        "rewardCoins" to def.rewardCoins,
                        "claimed" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val claimed = snap.getBoolean("claimed") ?: false
                val currentProgress = snap.getLong("progress") ?: 0L
                if (!claimed && currentProgress < def.target) {
                    ref.update(
                        mapOf(
                            "progress" to (currentProgress + 1L).coerceAtMost(def.target),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
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

    override suspend fun claimMissionReward(
        userId: String,
        currentCoins: Long,
        mission: DailyMissionProgress
    ): Result<Unit> {
        val path = "users/$userId/daily_missions/${mission.id}"
        return try {
            val uid = requireUserId(userId)
            require(mission.id.isNotBlank() && mission.userId == uid) { "Invalid mission record." }
            require(!mission.claimed) { "Mission reward already claimed today." }
            require(mission.isCompleted) { "Complete this mission before claiming its Grapes." }

            val walletRef = db.collection("wallets").document(uid)
            val missionRef = db.collection("users").document(uid)
                .collection("daily_missions").document(mission.id)

            val freshCoins = walletRef.get().await().getLong("coins") ?: currentCoins
            val batch = db.batch()
            batch.update(
                missionRef,
                mapOf(
                    "claimed" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                walletRef,
                mapOf(
                    "coins" to (freshCoins + mission.rewardCoins),
                    "lastClaimId" to mission.id,
                    "lastClaimType" to "mission",
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
}
