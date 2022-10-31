package io.novafoundation.nova.feature_governance_impl.di.modules.screens

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.GovernanceUnlockInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.RealGovernanceUnlockInteractor
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.runtime.repository.ChainStateRepository

@Module
class ReferendumUnlockModule {

    @Provides
    @FeatureScope
    fun provideGovernanceLocksOverviewInteractor(
        selectedAssetState: GovernanceSharedState,
        governanceSourceRegistry: GovernanceSourceRegistry,
        chainStateRepository: ChainStateRepository,
        computationalCache: ComputationalCache,
        accountRepository: AccountRepository,
        balanceLocksRepository: BalanceLocksRepository,
        extrinsicService: ExtrinsicService,
    ): GovernanceUnlockInteractor {
        return RealGovernanceUnlockInteractor(
            selectedAssetState = selectedAssetState,
            governanceSourceRegistry = governanceSourceRegistry,
            chainStateRepository = chainStateRepository,
            computationalCache = computationalCache,
            accountRepository = accountRepository,
            balanceLocksRepository = balanceLocksRepository,
            extrinsicService = extrinsicService
        )
    }
}
