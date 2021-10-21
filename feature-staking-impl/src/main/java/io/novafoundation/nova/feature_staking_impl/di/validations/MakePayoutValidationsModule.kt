package io.novafoundation.nova.feature_staking_impl.di.validations

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.CompositeValidation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.validations.payout.PayoutFeeValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.payout.PayoutValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.validations.payout.ProfitablePayoutValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingFeeValidation
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughToPayFeesValidation
import io.novafoundation.nova.feature_wallet_api.domain.validation.assetBalanceProducer

@Module
class MakePayoutValidationsModule {

    @Provides
    @FeatureScope
    fun provideFeeValidation(
        stakingSharedState: StakingSharedState,
        walletRepository: WalletRepository,
    ): PayoutFeeValidation {
        return EnoughToPayFeesValidation(
            feeExtractor = { it.fee },
            availableBalanceProducer = SetupStakingFeeValidation.assetBalanceProducer(
                walletRepository,
                originAddressExtractor = { it.originAddress },
                chainAssetExtractor = { it.chainAsset },
                stakingSharedState = stakingSharedState
            ),
            errorProducer = { PayoutValidationFailure.CannotPayFee }
        )
    }

    @FeatureScope
    @Provides
    fun provideProfitableValidation() = ProfitablePayoutValidation()

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
