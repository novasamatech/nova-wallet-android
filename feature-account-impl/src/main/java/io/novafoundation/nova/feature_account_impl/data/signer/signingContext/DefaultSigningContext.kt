package io.novafoundation.nova.feature_account_impl.data.signer.signingContext

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.data.signer.SigningContext
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novasama.substrate_sdk_android.runtime.extrinsic.Nonce

class DefaultSigningContext(
    override val chain: Chain,
    private val rpcCalls: RpcCalls,
) : SigningContext {

    override suspend fun getNonce(accountId: AccountIdKey): Nonce {
        return rpcCalls.getNonce(chain.id, chain.addressOf(accountId))
    }
}
