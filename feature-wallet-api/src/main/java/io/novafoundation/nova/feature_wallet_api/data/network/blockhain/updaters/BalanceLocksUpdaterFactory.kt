package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.updaters

import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface BalanceLocksUpdaterFactory {
    fun create(chainId: ChainId, chainAssetId: ChainAssetId): Updater
}
