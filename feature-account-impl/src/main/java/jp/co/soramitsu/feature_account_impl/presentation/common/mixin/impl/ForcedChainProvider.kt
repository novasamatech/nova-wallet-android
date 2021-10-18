package jp.co.soramitsu.feature_account_impl.presentation.common.mixin.impl

import jp.co.soramitsu.common.mixin.MixinFactory
import jp.co.soramitsu.common.utils.castOrNull
import jp.co.soramitsu.common.utils.flowOf
import jp.co.soramitsu.common.utils.inBackground
import jp.co.soramitsu.feature_account_api.data.mappers.mapChainToUi
import jp.co.soramitsu.feature_account_api.presenatation.account.add.AddAccountPayload
import jp.co.soramitsu.feature_account_api.presenatation.chain.ChainUi
import jp.co.soramitsu.feature_account_impl.presentation.common.mixin.api.ForcedChainMixin
import jp.co.soramitsu.runtime.multiNetwork.ChainRegistry
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
