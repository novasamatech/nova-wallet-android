package io.novafoundation.nova.feature_staking_impl.di.validations

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.CompositeValidation
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.validations.rewardDestination.RewardDestinationControllerRequiredValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.rewardDestination.RewardDestinationFeeValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.rewardDestination.RewardDestinationValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.validations.rewardDestination.RewardDestinationValidationSystem
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughAmountToTransferValidationGeneric

@Module
class RewardDestinationValidationsModule {

    @FeatureScope
    @Provides
    fun provideFeeValidation(): RewardDestinationFeeValidation = EnoughAmountToTransferValidationGeneric(
        feeExtractor = { it.fee },
        availableBalanceProducer = { it.availableControllerBalance },
        errorProducer = { RewardDestinationValidationFailure.CannotPayFees }
    )

    @Provides
    @FeatureScope
    fun controllerRequiredValidation(
        stakingSharedState: StakingSharedState,
        accountRepository: AccountRepository,
    ) = RewardDestinationControllerRequiredValidation(
        accountRepository = accountRepository,
        accountAddressExtractor = { it.stashState.controllerAddress },
        errorProducer = RewardDestinationValidationFailure::MissingController,
        sharedState = stakingSharedState
    )

    @FeatureScope
    @Provides
    fun provideRedeemValidationSystem(
        feeValidation: RewardDestinationFeeValidation,
        controllerRequiredValidation: RewardDestinationControllerRequiredValidation,
    ) = RewardDestinationValidationSystem(
        CompositeValidation(
            validations = listOf(
                feeValidation,
                controllerRequiredValidation
            )
        )
    )
}
