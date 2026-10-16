package io.novafoundation.nova.feature_account_api.data.extrinsic.recovery

import kotlinx.coroutines.flow.Flow

data class ExtrinsicSubmissionResolution(val hash: String, val outcome: RecoveredExtrinsicOutcome, val acknowledged: Boolean = false)

enum class RecoveredExtrinsicOutcome { SUCCESS, FAILURE, EXPIRED }

sealed interface ExtrinsicRecoveryState {

    data object None : ExtrinsicRecoveryState

    data class Pending(val hash: String) : ExtrinsicRecoveryState

    data class Resolved(val result: ExtrinsicSubmissionResolution) : ExtrinsicRecoveryState
}

interface ExtrinsicSubmissionRecovery {

    fun pendingHash(metaId: Long): String?

    fun observe(metaId: Long, chainId: String): Flow<ExtrinsicRecoveryState>

    suspend fun reconcile(metaId: Long, chainId: String): ExtrinsicRecoveryState

    suspend fun acknowledge(metaId: Long, hash: String)
}
