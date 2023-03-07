package io.novafoundation.nova.feature_staking_impl.domain.validations.setup

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.common.validation.validationWarning
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.common.minStake
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingValidationFailure.AmountLessThanAllowed
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingValidationFailure.AmountLessThanRecommended
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.coroutineContext

class MinimumStakeValidation(
    private val stakingRepository: StakingRepository,
    private val stakingSharedComputation: StakingSharedComputation,
) : SetupStakingValidation {

    override suspend fun validate(value: SetupStakingPayload): ValidationStatus<SetupStakingValidationFailure> {
        val chainAsset = value.stashAsset.token.configuration

        val scope = CoroutineScope(coroutineContext) // scope for cached execution == scope of coroutine

        val hardMinimum = chainAsset.amountFromPlanks(stakingRepository.minimumNominatorBond(chainAsset.chainId))
        val recommendedMinimum = chainAsset.amountFromPlanks(stakingSharedComputation.minStake(chainAsset.chainId, scope))

        // either first time bond or already existing bonded balance
        val amountToCheckAgainstHardMinimum = value.bondAmount ?: value.stashAsset.bonded

        return when {
            amountToCheckAgainstHardMinimum < hardMinimum -> validationError(AmountLessThanAllowed(hardMinimum))

            // we only check for recommended minimum if user bonds funds
            value.bondAmount != null && value.bondAmount < recommendedMinimum -> validationWarning(AmountLessThanRecommended(recommendedMinimum))

            else -> valid()
        }
    }
}

fun SetupStakingValidationSystemBuilder.minimumBondValidation(
    stakingRepository: StakingRepository,
    stakingSharedComputation: StakingSharedComputation,
) {
    validate(MinimumStakeValidation(stakingRepository, stakingSharedComputation))
}
