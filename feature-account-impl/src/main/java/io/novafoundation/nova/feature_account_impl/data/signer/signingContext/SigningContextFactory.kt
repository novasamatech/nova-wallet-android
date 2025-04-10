package io.novafoundation.nova.feature_account_impl.data.signer.signingContext

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.signer.SigningContext
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import javax.inject.Inject

@FeatureScope
internal class SigningContextFactory @Inject constructor(
    private val rpcCalls: RpcCalls
): SigningContext.Factory {

    override fun default(chain: Chain): SigningContext {
        return DefaultSigningContext(chain, rpcCalls)
    }
}
