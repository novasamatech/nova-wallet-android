package io.novafoundation.nova.feature_staking_impl.di.validations

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.CompositeValidation
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.domain.validations.unbond.EnoughToUnbondValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.unbond.NotZeroUnbondValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.unbond.UnbondFeeValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.unbond.UnbondLimitValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.unbond.UnbondValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.validations.unbond.UnbondValidationFailure.BondedWillCrossExistential
import io.novafoundation.nova.feature_staking_impl.domain.validations.unbond.UnbondValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.unbond.UnbondValidationSystem
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.validation.CrossMinimumBalanceValidation
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughAmountToTransferValidationGeneric

typealias RemainingUnbondValidation = CrossMinimumBalanceValidation<UnbondValidationPayload, UnbondValidationFailure>

@Module
class UnbondValidationsModule {

    @FeatureScope
    @Provides
    fun provideFeeValidation(): UnbondFeeValidation = EnoughAmountToTransferValidationGeneric(
        feeExtractor = { it.fee },
        availableBalanceProducer = { it.asset.transferable },
        errorProducer = { UnbondValidationFailure.CannotPayFees }
    )

    @FeatureScope
    @Provides
    fun provideNotZeroUnbondValidation() = NotZeroUnbondValidation(
        amountExtractor = { it.amount },
        errorProvider = { UnbondValidationFailure.ZeroUnbond }
    )

    @FeatureScope
    @Provides
    fun provideUnbondLimitValidation(
        stakingRepository: StakingRepository
    ) = UnbondLimitValidation(
        stakingRepository = stakingRepository,
        stashStateProducer = { it.stash },
        errorProducer = UnbondValidationFailure::UnbondLimitReached
    )

    @FeatureScope
    @Provides
    fun provideEnoughToUnbondValidation() = EnoughToUnbondValidation()

    @FeatureScope
    @Provides
    fun provideCrossExistentialValidation(
        walletConstants: WalletConstants
    ) = RemainingUnbondValidation(
        minimumBalance = { walletConstants.existentialDeposit(it.asset.token.configuration.chainId) },
        chainAsset = { it.asset.token.configuration },
        currentBalance = { it.asset.bonded },
        deductingAmount = { it.amount },
        error = ::BondedWillCrossExistential
    )

    @FeatureScope
    @Provides
    fun provideUnbondValidationSystem(
        unbondFeeValidation: UnbondFeeValidation,
        notZeroUnbondValidation: NotZeroUnbondValidation,
        unbondLimitValidation: UnbondLimitValidation,
        enoughToUnbondValidation: EnoughToUnbondValidation,
        remainingBondedAmountValidation: RemainingUnbondValidation
    ) = UnbondValidationSystem(
        CompositeValidation(
            validations = listOf(
                unbondFeeValidation,
                notZeroUnbondValidation,
                unbondLimitValidation,
                enoughToUnbondValidation,
                remainingBondedAmountValidation
            )
        )
    )
}
