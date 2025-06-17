package io.novafoundation.nova.feature_account_api.data.signer

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.Nonce

interface SigningContext {

    interface Factory {

        fun default(chain: Chain): SigningContext
    }

    val chain: Chain

    suspend fun getNonce(accountId: AccountIdKey): Nonce
}
