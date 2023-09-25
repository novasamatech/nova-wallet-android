package io.novafoundation.nova.feature_crowdloan_api.data.network.updater

import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface ContributionsUpdaterFactory {
    fun create(chain: Chain, assetBalanceScope: AssetBalanceScope): Updater<AssetBalanceScope.ScopeValue>
}
