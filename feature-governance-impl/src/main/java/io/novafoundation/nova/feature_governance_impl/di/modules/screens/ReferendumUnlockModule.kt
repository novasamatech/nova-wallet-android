package io.novafoundation.nova.feature_governance_impl.di.modules.screens

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.list.GovernanceLocksOverviewInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.list.RealGovernanceLocksOverviewInteractor
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
    ): GovernanceLocksOverviewInteractor {
        return RealGovernanceLocksOverviewInteractor(
            selectedAssetState = selectedAssetState,
            governanceSourceRegistry = governanceSourceRegistry,
            chainStateRepository = chainStateRepository,
            computationalCache = computationalCache,
            accountRepository = accountRepository
        )
    }
}
