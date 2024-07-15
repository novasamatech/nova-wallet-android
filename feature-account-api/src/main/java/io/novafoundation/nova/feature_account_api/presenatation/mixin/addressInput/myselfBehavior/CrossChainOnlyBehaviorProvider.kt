package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.myselfBehavior

import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class CrossChainOnlyBehaviorProvider(
    private val accountUseCase: SelectedAccountUseCase,
    private val originChain: Flow<Chain>,
    destinationChain: Flow<Chain>,
) : MyselfBehaviorProvider {

    override val behavior: Flow<MyselfBehavior> = combine(originChain, destinationChain, ::Behavior)

    private inner class Behavior(
        private val originChain: Chain,
        private val destinationChain: Chain
    ) : MyselfBehavior {

        override suspend fun myselfAvailable(): Boolean {
            val metaAccount = accountUseCase.getSelectedMetaAccount()

            return originChain.id != destinationChain.id && metaAccount.hasAccountIn(destinationChain)
        }

        override suspend fun myself(): String? {
            val metaAccount = accountUseCase.getSelectedMetaAccount()

            return metaAccount.addressIn(destinationChain)
        }
    }
}
