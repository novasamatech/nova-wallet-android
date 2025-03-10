package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_account_api.data.model.amountByExecutingAccount
import io.novafoundation.nova.feature_staking_impl.data.asset
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.stakingType
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingProperties
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.maximumToStake
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import java.math.BigDecimal

class AvailableBalanceGapValidation(
    private val candidates: List<SingleStakingProperties>,
    private val locksRepository: BalanceLocksRepository,
) : StartMultiStakingValidation {

    override suspend fun validate(value: StartMultiStakingValidationPayload): ValidationStatus<StartMultiStakingValidationFailure> {
        val amount = value.selection.stake
        val stakingOption = value.selection.stakingOption
        val fee = value.fee.amountByExecutingAccount

        val maxToStakeWithMinStakes = candidates.map {
            val maximumToStake = it.maximumToStake(value.asset, fee)

            maximumToStake to it.minStake()
        }

        // check against global maximum
        val globalMaxToStake = maxToStakeWithMinStakes.maxOf { (maxToStake) -> maxToStake }
        if (globalMaxToStake == BigDecimal.ZERO || amount > globalMaxToStake) {
            return leaveForFurtherValidations()
        }

        // check against currently selected maximum
        val selectedCandidate = candidates.first { it.stakingType == stakingOption.stakingType }
        val selectedCandidateMaxToStakeBalance = selectedCandidate.maximumToStake(value.asset, fee)

        if (amount > selectedCandidateMaxToStakeBalance) {
            val biggestLock = locksRepository.getBiggestLock(stakingOption.chain, stakingOption.asset)
                // in case no locks we found we let type-specific validations handle it
                ?: return leaveForFurtherValidations()

            // we're sure such item exists due to global maximum check before
            val (_, matchingAlternativeMinStake) = maxToStakeWithMinStakes.first { (maxToSTake, _) -> amount <= maxToSTake }

            return validationError(
                StartMultiStakingValidationFailure.AvailableBalanceGap(
                    currentMaxAvailable = selectedCandidateMaxToStakeBalance,
                    alternativeMinStake = matchingAlternativeMinStake,
                    chainAsset = stakingOption.asset,
                    biggestLockId = biggestLock.id
                )
            )
        }

        return valid()
    }

    private fun leaveForFurtherValidations(): ValidationStatus<StartMultiStakingValidationFailure> {
        return valid()
    }
}

fun StartMultiStakingValidationSystemBuilder.availableBalanceGapValidation(
    candidates: List<SingleStakingProperties>,
    locksRepository: BalanceLocksRepository,
) {
    validate(AvailableBalanceGapValidation(candidates, locksRepository))
}
