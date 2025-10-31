package io.novafoundation.nova.feature_wallet_api.domain

import io.novafoundation.nova.feature_wallet_api.domain.model.GetAssetOption
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

interface AssetGetOptionsUseCase {

    fun observeAssetGetOptionsForSelectedAccount(chainAssetFlow: Flow<Chain.Asset?>): Flow<Set<GetAssetOption>>
}
