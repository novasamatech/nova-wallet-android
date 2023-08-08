package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.common.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.NominationPoolsBondMoreInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.RealNominationPoolsBondMoreInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations.NominationPoolsBondMoreValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations.nominationPoolsBondMore
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.hints.NominationPoolHintsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.PoolStateValidationFactory
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.hints.NominationPoolsBondMoreHintsFactory

@Module
class NominationPoolsCommonBondMoreModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        extrinsicService: ExtrinsicService,
        stakingSharedState: StakingSharedState
    ): NominationPoolsBondMoreInteractor {
        return RealNominationPoolsBondMoreInteractor(extrinsicService, stakingSharedState)
    }

    @Provides
    @ScreenScope
    fun provideValidationSystem(
        poolStateValidationFactory: PoolStateValidationFactory
    ): NominationPoolsBondMoreValidationSystem = ValidationSystem.nominationPoolsBondMore(poolStateValidationFactory)

    @Provides
    @ScreenScope
    fun provideHintsMixinFactory(
        nominationPoolHintsUseCase: NominationPoolHintsUseCase,
        resourceManager: ResourceManager
    ) = NominationPoolsBondMoreHintsFactory(nominationPoolHintsUseCase, resourceManager)
}
