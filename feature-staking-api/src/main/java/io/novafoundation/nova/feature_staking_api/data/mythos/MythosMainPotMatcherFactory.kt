package io.novafoundation.nova.feature_staking_api.data.mythos

import io.novafoundation.nova.feature_account_api.domain.account.system.SystemAccountMatcher
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface MythosMainPotMatcherFactory {

    suspend fun create(chainAsset: Chain.Asset): SystemAccountMatcher?
}
