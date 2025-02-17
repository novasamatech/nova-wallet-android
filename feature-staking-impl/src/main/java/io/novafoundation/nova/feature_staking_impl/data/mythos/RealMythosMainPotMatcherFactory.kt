package io.novafoundation.nova.feature_staking_impl.data.mythos

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.domain.account.system.AccountSystemAccountMatcher
import io.novafoundation.nova.feature_account_api.domain.account.system.SystemAccountMatcher
import io.novafoundation.nova.feature_staking_api.data.mythos.MythosMainPotMatcherFactory
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosStakingRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.MYTHOS
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@FeatureScope
class RealMythosMainPotMatcherFactory @Inject constructor(
    private val mythosStakingRepository: MythosStakingRepository
): MythosMainPotMatcherFactory {

    private val fetchMutex = Mutex()
    private var cache: SystemAccountMatcher? = null

    override suspend fun create(chainAsset: Chain.Asset): SystemAccountMatcher? {
        if (MYTHOS !in chainAsset.staking) return null

        return fetchMutex.withLock {
            if (cache == null) {
                cache = mythosStakingRepository.getMainStakingPot(chainAsset.chainId)
                    .map(::AccountSystemAccountMatcher)
                    .getOrNull()
            }

            cache
        }
    }
}
