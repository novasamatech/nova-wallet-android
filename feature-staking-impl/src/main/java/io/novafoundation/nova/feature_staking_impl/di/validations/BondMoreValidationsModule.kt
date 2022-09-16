package io.novafoundation.nova.feature_staking_impl.di.validations

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.CompositeValidation
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.validations.bond.BondMoreFeeValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.bond.BondMoreValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.validations.bond.BondMoreValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.bond.BondMoreValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.validations.bond.NotZeroBondValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingFeeValidation
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughToPayFeesValidation
import io.novafoundation.nova.feature_wallet_api.domain.validation.assetBalanceProducer

@Module
class BondMoreValidationsModule {

    @Provides
    @FeatureScope
    fun provideFeeValidation(
        stakingSharedState: StakingSharedState,
        walletRepository: WalletRepository,
    ): BondMoreFeeValidation {
        return EnoughToPayFeesValidation(
            feeExtractor = { it.fee },
            availableBalanceProducer = SetupStakingFeeValidation.assetBalanceProducer(
                walletRepository,
                originAddressExtractor = { it.stashAddress },
                chainAssetExtractor = { it.chainAsset },
                stakingSharedState = stakingSharedState
            ),
            errorProducer = { _, _ -> BondMoreValidationFailure.NOT_ENOUGH_TO_PAY_FEES },
            extraAmountExtractor = { it.amount }
        )
    }

    @Provides
    @FeatureScope
    fun provideNotZeroBondValidation() = NotZeroBondValidation(
        amountExtractor = BondMoreValidationPayload::amount,
        errorProvider = { BondMoreValidationFailure.ZERO_BOND }
    )

    @Provides
    @FeatureScope
    fun provideBondMoreValidationSystem(
        bondMoreFeeValidation: BondMoreFeeValidation,
        notZeroBondValidation: NotZeroBondValidation,
    ) = BondMoreValidationSystem(
        validation = CompositeValidation(
            validations = listOf(
                bondMoreFeeValidation,
                notZeroBondValidation
            )
        )
    )
}
