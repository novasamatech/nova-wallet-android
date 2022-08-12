package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.updaters

import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface BalanceLocksUpdateSystemFactory {
    fun create(chainId: ChainId, chainAssetId: ChainAssetId): UpdateSystem
}
