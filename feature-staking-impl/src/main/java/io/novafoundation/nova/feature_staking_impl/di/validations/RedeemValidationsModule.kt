package io.novafoundation.nova.feature_staking_impl.di.validations

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.CompositeValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.reedeem.RedeemFeeValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.reedeem.RedeemValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.validations.reedeem.RedeemValidationSystem

@Module
class RedeemValidationsModule {

    @FeatureScope
    @Provides
    fun provideFeeValidation() = RedeemFeeValidation(
        feeExtractor = { it.fee },
        availableBalanceProducer = { it.asset.transferable },
        errorProducer = { _, _ -> RedeemValidationFailure.CANNOT_PAY_FEES }
    )

    @FeatureScope
    @Provides
    fun provideRedeemValidationSystem(
        feeValidation: RedeemFeeValidation,
    ) = RedeemValidationSystem(
        CompositeValidation(
            validations = listOf(
                feeValidation
            )
        )
    )
}
