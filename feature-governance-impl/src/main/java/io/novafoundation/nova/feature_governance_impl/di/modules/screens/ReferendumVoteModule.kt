package io.novafoundation.nova.feature_governance_impl.di.modules.screens

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.VoteReferendumInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.RealVoteReferendumInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.referendum.VoteReferendumValidationSystem
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.referendum.voteReferendumValidationSystem
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.common.LocksChangeFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.common.RealLocksChangeFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.hints.ReferendumVoteHintsMixinFactory
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.runtime.repository.ChainStateRepository

@Module
class ReferendumVoteModule {

    @Provides
    @FeatureScope
    fun provideReferendumVoteInteractor(
        governanceSourceRegistry: GovernanceSourceRegistry,
        chainStateRepository: ChainStateRepository,
        selectedChainState: GovernanceSharedState,
        accountRepository: AccountRepository,
        locksRepository: BalanceLocksRepository,
        extrinsicService: ExtrinsicService,
        computationalCache: ComputationalCache
    ): VoteReferendumInteractor {
        return RealVoteReferendumInteractor(
            governanceSourceRegistry = governanceSourceRegistry,
            chainStateRepository = chainStateRepository,
            selectedChainState = selectedChainState,
            accountRepository = accountRepository,
            extrinsicService = extrinsicService,
            locksRepository = locksRepository,
            computationalCache = computationalCache
        )
    }

    @Provides
    @FeatureScope
    fun provideHintsMixinFactory(
        resHintsMixinFactory: ResourcesHintsMixinFactory
    ): ReferendumVoteHintsMixinFactory = ReferendumVoteHintsMixinFactory(resHintsMixinFactory)

    @Provides
    @FeatureScope
    fun provideLocksChangeFormatter(
        resourceManager: ResourceManager,
        amountFormatter: AmountFormatter
    ): LocksChangeFormatter = RealLocksChangeFormatter(resourceManager, amountFormatter)

    @Provides
    @FeatureScope
    fun provideValidationSystem(
        governanceSourceRegistry: GovernanceSourceRegistry,
        governanceSharedState: GovernanceSharedState,
    ): VoteReferendumValidationSystem = ValidationSystem.voteReferendumValidationSystem(governanceSourceRegistry, governanceSharedState)
}
