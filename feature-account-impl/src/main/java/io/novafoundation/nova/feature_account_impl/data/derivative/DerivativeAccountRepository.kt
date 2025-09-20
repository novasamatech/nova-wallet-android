package io.novafoundation.nova.feature_account_impl.data.derivative

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import javax.inject.Inject

interface DerivativeAccountRepository {

    fun areDerivativeAccountsSupported(chain: Chain): Boolean
}

@FeatureScope
class RealDerivativeAccountRepository @Inject constructor(
): DerivativeAccountRepository {

    override fun areDerivativeAccountsSupported(chain: Chain): Boolean {
        // TODO derivative: separate feature flag
        return chain.hasSubstrateRuntime
    }
}
