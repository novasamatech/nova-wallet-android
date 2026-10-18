package io.novafoundation.nova.feature_account_impl.data.extrinsic.recovery

import android.content.SharedPreferences
import com.google.gson.Gson
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicSubmissionResolution
import java.math.BigInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class PendingExtrinsicSubmission(
    val hash: String,
    val chainId: String? = null,
    val anchor: FinalizedBlockReference? = null,
    val nextBlock: BigInteger? = null,
    val expiresAt: BigInteger? = null
)

data class FinalizedBlockReference(val number: BigInteger, val hash: String)

class ExtrinsicSubmissionJournal(
    private val preferences: SharedPreferences,
    private val gson: Gson
) {

    private val mutex = Mutex()

    fun pending(metaId: Long): PendingExtrinsicSubmission? {
        val raw = preferences.getString(key(metaId), null) ?: return null
        if (!raw.startsWith("{")) return PendingExtrinsicSubmission(raw) // Existing hash-only records stay blocked.
        return requireNotNull(gson.fromJson(raw, PendingExtrinsicSubmission::class.java)).also {
            require(it.hash.isNotBlank())
            require(it.chainId != null && it.anchor != null && it.nextBlock != null && it.expiresAt != null)
            require(it.anchor.number.isNonNegative && it.anchor.hash.isNotBlank())
            require(it.nextBlock > it.anchor.number && it.nextBlock <= it.expiresAt && it.expiresAt > it.anchor.number)
        }
    }

    fun pendingHash(metaId: Long): String? = pending(metaId)
        ?.hash

    fun resolution(metaId: Long): ExtrinsicSubmissionResolution? =
        preferences.getString(resultKey(metaId), null)
            ?.let { gson.fromJson(it, ExtrinsicSubmissionResolution::class.java) }

    suspend fun record(metaId: Long, submission: PendingExtrinsicSubmission) = withContext(Dispatchers.IO) {
        mutex.withLock {
            check(pending(metaId) == null) { "A recoverable submission is already pending" }
            // apply() cannot confirm durability before broadcasting. A failed commit aborts submission.
            check(
                preferences.edit()
                    .putString(key(metaId), gson.toJson(submission))
                    .remove(resultKey(metaId))
                    .commit()
            )
        }
    }

    suspend fun advance(metaId: Long, hash: String, nextBlock: BigInteger) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val submission = pending(metaId)
                ?.takeIf { it.hash == hash } ?: return@withLock
            require(nextBlock == requireNotNull(submission.nextBlock) + BigInteger.ONE)
            check(
                preferences.edit()
                    .putString(key(metaId), gson.toJson(submission.copy(nextBlock = nextBlock)))
                    .commit()
            )
        }
    }

    suspend fun resolved(metaId: Long, result: ExtrinsicSubmissionResolution) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (pendingHash(metaId) != result.hash) return@withLock
            check(
                preferences.edit()
                    .remove(key(metaId))
                    .putString(resultKey(metaId), gson.toJson(result))
                    .commit()
            )
        }
    }

    suspend fun acknowledge(metaId: Long, hash: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val result = resolution(metaId)
                ?.takeIf { it.hash == hash } ?: return@withLock
            check(
                preferences.edit()
                    .putString(resultKey(metaId), gson.toJson(result.copy(acknowledged = true)))
                    .commit()
            )
        }
    }

    private fun key(metaId: Long) = "bittensor_pending_submission_$metaId"

    private fun resultKey(metaId: Long) = "bittensor_submission_result_$metaId"
}
