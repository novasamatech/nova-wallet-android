package io.novafoundation.nova.feature_staking_impl.di.validations

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.from
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorExitBalanceValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorExitColdkeySwapValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorExitFeeBalanceValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorExitFeeSourceValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorExitHoldValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorExitMinimumValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorExitRemainderValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorExitSafeModeValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorExitTradingEnabledValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorFullExitValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorExitPlanValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorExitQuoteValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorExitPriceImpactValidation

@Module
class BittensorExitValidationsModule {

    @Provides
    fun provideExitValidationSystem(): BittensorExitValidationSystem {
        val exitHoldValidation = BittensorExitHoldValidation()

        val exitBalanceValidation = BittensorExitBalanceValidation()

        val fullExitValidation = BittensorFullExitValidation()

        val exitMinimumValidation = BittensorExitMinimumValidation()

        val exitRemainderValidation = BittensorExitRemainderValidation()

        val exitFeeBalanceValidation = BittensorExitFeeBalanceValidation()

        val exitPlanValidation = BittensorExitPlanValidation()

        val exitQuoteValidation = BittensorExitQuoteValidation()

        val exitPriceImpactValidation = BittensorExitPriceImpactValidation()

        val exitFeeSourceValidation = BittensorExitFeeSourceValidation()
        val exitColdkeySwapValidation = BittensorExitColdkeySwapValidation()

        val exitSafeModeValidation = BittensorExitSafeModeValidation()

        val exitTradingEnabledValidation = BittensorExitTradingEnabledValidation()

        return ValidationSystem.from(
            listOf(
                exitColdkeySwapValidation,
                exitSafeModeValidation,
                exitTradingEnabledValidation,
                exitHoldValidation,
                exitBalanceValidation,
                fullExitValidation,
                exitFeeBalanceValidation,
                exitMinimumValidation,
                exitRemainderValidation,
                exitPlanValidation,
                exitQuoteValidation,
                exitPriceImpactValidation,
                exitFeeSourceValidation
            )
        )
    }
}
