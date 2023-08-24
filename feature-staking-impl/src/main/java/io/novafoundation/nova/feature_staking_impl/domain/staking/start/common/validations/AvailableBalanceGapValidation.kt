package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_staking_impl.data.asset
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.stakingType
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingProperties
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository

class AvailableBalanceGapValidation(
    private val candidates: List<SingleStakingProperties>,
    private val locksRepository: BalanceLocksRepository,
) : StartMultiStakingValidation {

    override suspend fun validate(value: StartMultiStakingValidationPayload): ValidationStatus<StartMultiStakingValidationFailure> {
        val amount = value.selection.stake
        val stakingOption = value.selection.stakingOption

        val availableBalancesWithMinStakes = candidates.map {
            val availableBalance = it.availableBalance(value.asset) - value.fee.fee.amount

            availableBalance to it.minStake()
        }

        // check against global maximum
        val maxAvailable = availableBalancesWithMinStakes.maxOf { (availableBalance) -> availableBalance }
        if (amount > maxAvailable) {
            return validationError(StartMultiStakingValidationFailure.NotEnoughAvailableToStake)
        }

        // check against currently selected maximum
        val selectedCandidate = candidates.first { it.stakingType == stakingOption.stakingType }
        val selectedCandidateAvailableBalance = selectedCandidate.availableBalance(value.asset)
        if (amount > selectedCandidateAvailableBalance) {
            val biggestLock = locksRepository.getBiggestLock(stakingOption.chain, stakingOption.asset)
                // we fallback to simpler error in case we haven't found any locks
                ?: return validationError(StartMultiStakingValidationFailure.NotEnoughAvailableToStake)

            // we're sure such item exists due to global maximum check before
            val (_, matchingAlternativeMinStake) = availableBalancesWithMinStakes.first { (available, _) -> amount <= available }

            return validationError(
                StartMultiStakingValidationFailure.AvailableBalanceGap(
                    currentMaxAvailable = selectedCandidateAvailableBalance,
                    alternativeMinStake = matchingAlternativeMinStake,
                    chainAsset = stakingOption.asset,
                    biggestLockId = biggestLock.id
                )
            )
        }

        return valid()
    }
}

fun StartMultiStakingValidationSystemBuilder.availableBalanceGapValidation(
    candidates: List<SingleStakingProperties>,
    locksRepository: BalanceLocksRepository,
) {
    validate(AvailableBalanceGapValidation(candidates, locksRepository))
}
