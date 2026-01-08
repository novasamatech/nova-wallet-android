package io.novafoundation.nova.feature_staking_impl.di.validations

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.CompositeValidation
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_impl.domain.validations.reedeem.RedeemFeeValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.reedeem.RedeemValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.validations.reedeem.RedeemValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.reedeem.RedeemValidationSystem
import io.novafoundation.nova.feature_wallet_api.domain.model.balanceCountedTowardsED
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughAmountToTransferValidationGeneric
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughBalanceToStayAboveEDValidation
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset

@Module
class RedeemValidationsModule {

    @FeatureScope
    @Provides
    fun provideFeeValidation(): RedeemFeeValidation = EnoughAmountToTransferValidationGeneric(
        feeExtractor = { it.fee },
        availableBalanceProducer = { it.asset.transferable },
        errorProducer = { RedeemValidationFailure.CannotPayFees }
    )

    @FeatureScope
    @Provides
    fun provideEnoughTotalToStayAboveEDValidation(
        enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
    ): EnoughBalanceToStayAboveEDValidation<RedeemValidationPayload, RedeemValidationFailure, Fee> {
        return enoughTotalToStayAboveEDValidationFactory.create(
            fee = { it.fee },
            balance = { it.asset.balanceCountedTowardsED() },
            chainWithAsset = { ChainWithAsset(it.chain, it.asset.token.configuration) },
            error = { payload, error -> RedeemValidationFailure.NotEnoughBalanceToStayAboveED(payload.asset.token.configuration, error) }
        )
    }

    @FeatureScope
    @Provides
    fun provideRedeemValidationSystem(
        feeValidation: RedeemFeeValidation,
        enoughTotalToStayAboveEDValidation: EnoughBalanceToStayAboveEDValidation<RedeemValidationPayload, RedeemValidationFailure, Fee>
    ) = RedeemValidationSystem(
        CompositeValidation(
            validations = listOf(
                feeValidation,
                enoughTotalToStayAboveEDValidation
            )
        )
    )
}
