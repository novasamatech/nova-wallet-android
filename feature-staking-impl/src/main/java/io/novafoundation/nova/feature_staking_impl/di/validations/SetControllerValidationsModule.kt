package io.novafoundation.nova.feature_staking_impl.di.validations

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.CompositeValidation
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.validations.NotZeroBalanceValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.controller.IsNotControllerAccountValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.controller.SetControllerFeeValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.controller.SetControllerValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.validations.controller.SetControllerValidationSystem
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughAmountToTransferValidation

@Module
class SetControllerValidationsModule {

    @FeatureScope
    @Provides
    fun provideFeeValidation(): SetControllerFeeValidation {
        return EnoughAmountToTransferValidation(
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
        walletRepository: WalletRepository
    ): NotZeroBalanceValidation {
        return NotZeroBalanceValidation(
            walletRepository = walletRepository,
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
