package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.storage.Preferences

class BittensorPendingSubmissionRepository(private val preferences: Preferences) {

    fun pendingHash(metaId: Long): String? = preferences.getString(key(metaId))

    fun record(metaId: Long, hash: String) {
        check(pendingHash(metaId) == null) { "A Bittensor submission is already pending" }
        preferences.putString(key(metaId), hash)
    }

    fun resolved(metaId: Long, hash: String) {
        if (pendingHash(metaId) == hash) preferences.removeField(key(metaId))
    }

    private fun key(metaId: Long) = "bittensor_pending_submission_$metaId"
}
