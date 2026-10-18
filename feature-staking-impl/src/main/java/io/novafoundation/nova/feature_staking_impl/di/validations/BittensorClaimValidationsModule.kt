package io.novafoundation.nova.feature_staking_impl.di.validations

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.from
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorClaimValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorClaimAvailabilityValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorClaimColdkeySwapValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorClaimFeeValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorClaimForfeitureValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorClaimRewardsValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorClaimSafeModeValidation

@Module
class BittensorClaimValidationsModule {

    @Provides
    fun provideClaimValidationSystem(): BittensorClaimValidationSystem {
        val claimAvailabilityValidation = BittensorClaimAvailabilityValidation()

        val claimRewardsValidation = BittensorClaimRewardsValidation()

        val claimForfeitureValidation = BittensorClaimForfeitureValidation()

        val claimFeeValidation = BittensorClaimFeeValidation()
        val claimColdkeySwapValidation = BittensorClaimColdkeySwapValidation()

        val claimSafeModeValidation = BittensorClaimSafeModeValidation()

        return ValidationSystem.from(
            listOf(
                claimColdkeySwapValidation,
                claimSafeModeValidation,
                claimAvailabilityValidation,
                claimRewardsValidation,
                claimForfeitureValidation,
                claimFeeValidation
            )
        )
    }
}
