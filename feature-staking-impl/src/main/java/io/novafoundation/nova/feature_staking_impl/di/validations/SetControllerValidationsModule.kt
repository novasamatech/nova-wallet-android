package io.novafoundation.nova.feature_staking_impl.di.validations

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.CompositeValidation
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.validations.NotZeroBalanceValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.controller.IsNotControllerAccountValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.controller.SetControllerFeeValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.controller.SetControllerValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.controller.SetControllerValidationSystem
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughAmountToTransferValidationGeneric

@Module
class SetControllerValidationsModule {

    @FeatureScope
    @Provides
    fun provideFeeValidation(): SetControllerFeeValidation {
        return EnoughAmountToTransferValidationGeneric(
            feeExtractor = { it.fee },
            availableBalanceProducer = { it.transferable },
            errorProducer = { SetControllerValidationFailure.NOT_ENOUGH_TO_PAY_FEES }
        )
    }

    @FeatureScope
    @Provides
    fun provideControllerValidation(
        stakingSharedState: StakingSharedState,
        stakingRepository: StakingRepository
    ) = IsNotControllerAccountValidation(
        stakingRepository = stakingRepository,
        controllerAddressProducer = { it.controllerAddress },
        errorProducer = { SetControllerValidationFailure.ALREADY_CONTROLLER },
        sharedState = stakingSharedState
    )

    @FeatureScope
    @Provides
    fun provideZeroBalanceControllerValidation(
        stakingSharedState: StakingSharedState,
        assetSourceRegistry: AssetSourceRegistry,
    ): NotZeroBalanceValidation {
        return NotZeroBalanceValidation(
            assetSourceRegistry = assetSourceRegistry,
            stakingSharedState = stakingSharedState
        )
    }

    @FeatureScope
    @Provides
    fun provideSetControllerValidationSystem(
        enoughToPayFeesValidation: SetControllerFeeValidation,
        isNotControllerAccountValidation: IsNotControllerAccountValidation,
        controllerAccountIsNotZeroBalance: NotZeroBalanceValidation
    ) = SetControllerValidationSystem(
        CompositeValidation(
            validations = listOf(
                enoughToPayFeesValidation,
                isNotControllerAccountValidation,
                controllerAccountIsNotZeroBalance
            )
        )
    )
}
