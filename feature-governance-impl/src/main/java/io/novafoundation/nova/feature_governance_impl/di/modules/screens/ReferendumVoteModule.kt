package io.novafoundation.nova.feature_governance_impl.di.modules.screens

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.VoteReferendumInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.RealVoteReferendumInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.VoteReferendumValidationSystem
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.voteReferendumValidationSystem
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.hints.ReferendumVoteHintsMixinFactory
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
        extrinsicService: ExtrinsicService
    ): VoteReferendumInteractor {
        return RealVoteReferendumInteractor(
            governanceSourceRegistry = governanceSourceRegistry,
            chainStateRepository = chainStateRepository,
            selectedChainState = selectedChainState,
            accountRepository = accountRepository,
            extrinsicService = extrinsicService
        )
    }

    @Provides
    @FeatureScope
    fun provideHintsMixinFactory(
        resHintsMixinFactory: ResourcesHintsMixinFactory
    ): ReferendumVoteHintsMixinFactory = ReferendumVoteHintsMixinFactory(resHintsMixinFactory)

    @Provides
    @FeatureScope
    fun provideValidationSystem(
        governanceSourceRegistry: GovernanceSourceRegistry
    ): VoteReferendumValidationSystem = ValidationSystem.voteReferendumValidationSystem(governanceSourceRegistry)
}
