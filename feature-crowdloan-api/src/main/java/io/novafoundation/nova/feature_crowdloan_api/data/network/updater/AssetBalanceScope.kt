package io.novafoundation.nova.feature_crowdloan_api.data.network.updater

import io.novafoundation.nova.core.updater.UpdateScope
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.flow.Flow

interface AssetBalanceScope : UpdateScope {
    override fun invalidationFlow(): Flow<Asset>
}
