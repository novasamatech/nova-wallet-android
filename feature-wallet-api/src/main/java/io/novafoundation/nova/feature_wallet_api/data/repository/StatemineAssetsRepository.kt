package io.novafoundation.nova.feature_wallet_api.data.repository

import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.StatemineAssetDetails
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow

interface StatemineAssetsRepository {

    suspend fun getAssetDetails(
        chainId: ChainId,
        assetType: Chain.Asset.Type.Statemine,
    ): StatemineAssetDetails

    suspend fun subscribeAndSyncAssetDetails(
        chainId: ChainId,
        assetType: Chain.Asset.Type.Statemine,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<StatemineAssetDetails>
}
