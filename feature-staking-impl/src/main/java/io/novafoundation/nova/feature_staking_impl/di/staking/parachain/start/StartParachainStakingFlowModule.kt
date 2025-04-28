package io.novafoundation.nova.feature_staking_impl.di.staking.parachain.start

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CandidatesRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.DelegatorStateRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.RealStartParachainStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.StartParachainStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.MinimumDelegationValidationFactory
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.NoPendingRevokeValidationFactory
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.parachainStakingStart

@Module
class StartParachainStakingFlowModule {

    @Provides
    @FeatureScope
    fun provideMinimumValidationFactory(
        candidatesRepository: CandidatesRepository,
        stakingConstantsRepository: ParachainStakingConstantsRepository,
        delegatorStateUseCase: DelegatorStateUseCase,
    ) = MinimumDelegationValidationFactory(stakingConstantsRepository, candidatesRepository, delegatorStateUseCase)

    @Provides
    @FeatureScope
    fun provideNoPendingRevokeValidationFactory(
        delegatorStateRepository: DelegatorStateRepository,
    ) = NoPendingRevokeValidationFactory(delegatorStateRepository)

    @Provides
    @FeatureScope
    fun provideValidationSystem(
        minimumDelegationValidationFactory: MinimumDelegationValidationFactory,
        noPendingRevokeValidationFactory: NoPendingRevokeValidationFactory,
    ): StartParachainStakingValidationSystem = ValidationSystem.parachainStakingStart(minimumDelegationValidationFactory, noPendingRevokeValidationFactory)

    @Provides
    @FeatureScope
    fun provideInteractor(
        extrinsicService: ExtrinsicService,
        singleAssetSharedState: StakingSharedState,
        stakingConstantsRepository: ParachainStakingConstantsRepository,
        delegatorStateRepository: DelegatorStateRepository,
        candidatesRepository: CandidatesRepository,
        accountRepository: AccountRepository,
    ): StartParachainStakingInteractor = RealStartParachainStakingInteractor(
        extrinsicService = extrinsicService,
        singleAssetSharedState = singleAssetSharedState,
        stakingConstantsRepository = stakingConstantsRepository,
        delegatorStateRepository = delegatorStateRepository,
        candidatesRepository = candidatesRepository,
        accountRepository = accountRepository
    )
}
