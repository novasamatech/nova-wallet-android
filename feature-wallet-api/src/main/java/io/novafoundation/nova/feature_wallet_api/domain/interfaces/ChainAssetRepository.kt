package io.novafoundation.nova.feature_wallet_api.domain.interfaces

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface ChainAssetRepository {

    suspend fun insertCustomAsset(chainAsset: Chain.Asset)
}
