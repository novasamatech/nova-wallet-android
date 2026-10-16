package io.novafoundation.nova.feature_staking_impl.di.validations

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.bittensorStakeValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorColdkeySwapValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorMinimumStakeValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorPriceImpactValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorQuoteAvailabilityValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorQuoteFreshnessValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorQuoteTermsValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorRegisteredValidatorValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorRootStakeHoldValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorSafeModeValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorSlippageValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorStakeFeeReserveValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorSubnetStakingEnabledValidation

@Module
class BittensorStakeValidationsModule {

    @Provides
    fun provideStakeValidationSystem(): BittensorStakeValidationSystem {
        val feeReserve = BittensorStakeFeeReserveValidation()

        val minimumStake = BittensorMinimumStakeValidation()

        val registeredValidator = BittensorRegisteredValidatorValidation()

        val stakingEnabled = BittensorSubnetStakingEnabledValidation()

        val rootHold = BittensorRootStakeHoldValidation()

        val coldkeySwap = BittensorColdkeySwapValidation()

        val safeMode = BittensorSafeModeValidation()

        val quoteAvailability = BittensorQuoteAvailabilityValidation()

        val quoteFreshness = BittensorQuoteFreshnessValidation()

        val quoteTerms = BittensorQuoteTermsValidation()

        val slippage = BittensorSlippageValidation()

        val priceImpact = BittensorPriceImpactValidation()

        return bittensorStakeValidationSystem(
            listOf(
                feeReserve,
                minimumStake,
                registeredValidator,
                stakingEnabled,
                rootHold,
                coldkeySwap,
                safeMode,
                quoteAvailability,
                quoteTerms,
                quoteFreshness,
                slippage,
                priceImpact
            )
        )
    }
}
