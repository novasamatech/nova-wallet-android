package io.novafoundation.nova.feature_account_api.data.extrinsic

import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.Signer

data class SubmissionOrigin(
    /**
     * Origin that was originally requested to sign the transaction
     */
    val requestedOrigin: AccountId,

    /**
     * Origin that was actually used to sign the transaction.
     * It might differ from [requestedOrigin] if [Signer] modified the origin, for example in the case of Proxied wallet
     */
    val actualOrigin: AccountId
) {

    companion object {

        fun singleOrigin(origin: AccountId) = SubmissionOrigin(origin, origin)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubmissionOrigin

        if (!requestedOrigin.contentEquals(other.requestedOrigin)) return false
        return actualOrigin.contentEquals(other.actualOrigin)
    }

    override fun hashCode(): Int {
        var result = requestedOrigin.contentHashCode()
        result = 31 * result + actualOrigin.contentHashCode()
        return result
    }
}
