package io.novafoundation.nova.feature_staking_impl.di.validations

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.CompositeValidation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.common.validation.ProfitableActionValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.payout.MakePayoutPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.payout.PayoutFeeValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.payout.PayoutValidationFailure
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughAmountToTransferValidationGeneric

typealias ProfitablePayoutValidation = ProfitableActionValidation<MakePayoutPayload, PayoutValidationFailure>

@Module
class MakePayoutValidationsModule {

    @Provides
    @FeatureScope
    fun provideFeeValidation(): PayoutFeeValidation {
        return EnoughAmountToTransferValidationGeneric(
            feeExtractor = { it.fee },
            availableBalanceProducer = { it.asset.transferable },
            errorProducer = { PayoutValidationFailure.CannotPayFee }
        )
    }

    @FeatureScope
    @Provides
    fun provideProfitableValidation() = ProfitablePayoutValidation(
        fee = { it.fee },
        amount = { totalReward },
        error = { PayoutValidationFailure.UnprofitablePayout }
    )

    @Provides
    @FeatureScope
    fun provideValidationSystem(
        enoughToPayFeesValidation: PayoutFeeValidation,
        profitablePayoutValidation: ProfitablePayoutValidation,
    ) = ValidationSystem(
        CompositeValidation(
            listOf(
                enoughToPayFeesValidation,
                profitablePayoutValidation,
            )
        )
    )
}
