package io.novafoundation.nova.feature_account_api.data.extrinsic

import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.Signer

data class SubmissionOrigin(
    /**
     * Account on which behalf the operation will be executed
     */
    val executingAccount: AccountId,

    /**
     * Account that will sign and submit transaction
     * It might differ from [executingAccount] if [Signer] modified the origin.
     * For example in the case of Proxied wallet [executingAccount] is proxied and [signingAccount] is proxy
     */
    val signingAccount: AccountId
) {

    companion object {

        fun singleOrigin(origin: AccountId) = SubmissionOrigin(origin, origin)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubmissionOrigin

        if (!executingAccount.contentEquals(other.executingAccount)) return false
        return signingAccount.contentEquals(other.signingAccount)
    }

    override fun hashCode(): Int {
        var result = executingAccount.contentHashCode()
        result = 31 * result + signingAccount.contentHashCode()
        return result
    }
}
