package io.novafoundation.nova.feature_staking_impl.di.validations

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.CompositeValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.rebond.EnoughToRebondValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.rebond.NotZeroRebondValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.rebond.RebondFeeValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.rebond.RebondValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.validations.rebond.RebondValidationSystem

@Module
class RebondValidationsModule {

    @FeatureScope
    @Provides
    fun provideFeeValidation() = RebondFeeValidation(
        feeExtractor = { it.fee },
        availableBalanceProducer = { it.controllerAsset.transferable },
        errorProducer = { RebondValidationFailure.CANNOT_PAY_FEE }
    )

    @FeatureScope
    @Provides
    fun provideNotZeroUnbondValidation() = NotZeroRebondValidation(
        amountExtractor = { it.rebondAmount },
        errorProvider = { RebondValidationFailure.ZERO_AMOUNT }
    )

    @FeatureScope
    @Provides
    fun provideEnoughToRebondValidation() = EnoughToRebondValidation()

    @FeatureScope
    @Provides
    fun provideRebondValidationSystem(
        rebondFeeValidation: RebondFeeValidation,
        notZeroRebondValidation: NotZeroRebondValidation,
        enoughToRebondValidation: EnoughToRebondValidation,
    ) = RebondValidationSystem(
        CompositeValidation(
            validations = listOf(
                rebondFeeValidation,
                notZeroRebondValidation,
                enoughToRebondValidation
            )
        )
    )
}
