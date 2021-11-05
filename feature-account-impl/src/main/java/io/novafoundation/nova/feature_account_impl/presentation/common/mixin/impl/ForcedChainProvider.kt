package io.novafoundation.nova.feature_account_impl.presentation.common.mixin.impl

import io.novafoundation.nova.common.mixin.MixinFactory
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.ForcedChainMixin
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

class ForcedChainMixinFactory(
    private val chainRegistry: ChainRegistry,
    private val payload: AddAccountPayload,
) : MixinFactory<ForcedChainMixin> {

    override fun create(scope: CoroutineScope): ForcedChainMixin {
        return ForcedChainProvider(chainRegistry, payload, scope)
    }
}

class ForcedChainProvider(
    private val chainRegistry: ChainRegistry,
    private val addAccountPayload: AddAccountPayload,
    scope: CoroutineScope,
) : ForcedChainMixin {

    override val forcedChainLiveData: Flow<ChainUi?> = flowOf {
        addAccountPayload.castOrNull<AddAccountPayload.ChainAccount>()?.let {
            val chain = chainRegistry.getChain(it.chainId)

            mapChainToUi(chain)
        }
    }
        .inBackground()
        .shareIn(scope, SharingStarted.Eagerly, replay = 1)
}
