package io.novafoundation.nova.feature_staking_impl.di.validations

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.CompositeValidation
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_impl.domain.validations.rebond.EnoughToRebondValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.rebond.NotZeroRebondValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.rebond.RebondFeeValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.rebond.RebondValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.validations.rebond.RebondValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.rebond.RebondValidationSystem
import io.novafoundation.nova.feature_wallet_api.domain.model.balanceCountedTowardsED
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughAmountToTransferValidationGeneric
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughBalanceToStayAboveEDValidation
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset

@Module
class RebondValidationsModule {

    @FeatureScope
    @Provides
    fun provideFeeValidation(): RebondFeeValidation = EnoughAmountToTransferValidationGeneric(
        feeExtractor = { it.fee },
        availableBalanceProducer = { it.controllerAsset.transferable },
        errorProducer = { RebondValidationFailure.CannotPayFee }
    )

    @FeatureScope
    @Provides
    fun provideNotZeroRebondValidation() = NotZeroRebondValidation(
        amountExtractor = { it.rebondAmount },
        errorProvider = { RebondValidationFailure.ZeroAmount }
    )

    @FeatureScope
    @Provides
    fun provideEnoughToRebondValidation() = EnoughToRebondValidation()

    @FeatureScope
    @Provides
    fun provideEnoughTotalToStayAboveEDValidation(
        enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
    ): EnoughBalanceToStayAboveEDValidation<RebondValidationPayload, RebondValidationFailure, Fee> {
        return enoughTotalToStayAboveEDValidationFactory.create(
            fee = { it.fee },
            balance = { it.controllerAsset.balanceCountedTowardsED() },
            chainWithAsset = { ChainWithAsset(it.chain, it.controllerAsset.token.configuration) },
            error = { payload, error -> RebondValidationFailure.NotEnoughBalanceToStayAboveED(payload.controllerAsset.token.configuration, error) }
        )
    }

    @FeatureScope
    @Provides
    fun provideRebondValidationSystem(
        rebondFeeValidation: RebondFeeValidation,
        notZeroRebondValidation: NotZeroRebondValidation,
        enoughToRebondValidation: EnoughToRebondValidation,
        enoughBalanceToStayAboveEDValidation: EnoughBalanceToStayAboveEDValidation<RebondValidationPayload, RebondValidationFailure, Fee>
    ) = RebondValidationSystem(
        CompositeValidation(
            validations = listOf(
                rebondFeeValidation,
                notZeroRebondValidation,
                enoughToRebondValidation,
                enoughBalanceToStayAboveEDValidation
            )
        )
    )
}
