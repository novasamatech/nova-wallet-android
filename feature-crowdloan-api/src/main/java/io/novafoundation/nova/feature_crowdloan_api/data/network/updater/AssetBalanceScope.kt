package io.novafoundation.nova.feature_crowdloan_api.data.network.updater

import io.novafoundation.nova.core.updater.UpdateScope
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_crowdloan_api.data.network.updater.AssetBalanceScope.ScopeValue
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.flow.Flow

interface AssetBalanceScope : UpdateScope<ScopeValue> {

    class ScopeValue(val metaAccount: MetaAccount, val asset: Asset)

    override fun invalidationFlow(): Flow<ScopeValue>
}
