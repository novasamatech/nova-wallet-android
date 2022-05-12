package io.novafoundation.nova.feature_staking_impl.di.staking.parachain.start

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CandidatesRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.DelegatorStateRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.RealStartParachainStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.StartParachainStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.MinimumDelegationValidationFactory
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationSystem
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class StartParachainStakingFlowModule {

    @Provides
    @FeatureScope
    fun provideMinimumValidationFactory(
        candidatesRepository: CandidatesRepository,
        stakingConstantsRepository: ParachainStakingConstantsRepository
    ) = MinimumDelegationValidationFactory(stakingConstantsRepository, candidatesRepository)

    @Provides
    @FeatureScope
    fun provideValidationSystem(
        minimumDelegationValidationFactory: MinimumDelegationValidationFactory,
    ): StartParachainStakingValidationSystem = ValidationSystem {
        with(minimumDelegationValidationFactory) {
            minimumDelegation()
        }

        sufficientBalance(
            fee = { it.fee },
            amount = { it.amount },
            available = { it.asset.transferable },
            error = { StartParachainStakingValidationFailure.NotEnoughBalanceToPayFees }
        )
    }

    @Provides
    @FeatureScope
    fun provideInteractor(
        extrinsicService: ExtrinsicService,
        chainRegistry: ChainRegistry,
        singleAssetSharedState: StakingSharedState,
        collatorProvider: CollatorProvider,
        stakingConstantsRepository: ParachainStakingConstantsRepository,
        delegatorStateRepository: DelegatorStateRepository,
        candidatesRepository: CandidatesRepository,
    ): StartParachainStakingInteractor = RealStartParachainStakingInteractor(
        extrinsicService = extrinsicService,
        chainRegistry = chainRegistry,
        singleAssetSharedState = singleAssetSharedState,
        collatorProvider = collatorProvider,
        stakingConstantsRepository = stakingConstantsRepository,
        delegatorStateRepository = delegatorStateRepository,
        candidatesRepository = candidatesRepository
    )
}
