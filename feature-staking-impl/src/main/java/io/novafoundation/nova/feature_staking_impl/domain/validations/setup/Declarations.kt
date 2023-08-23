package io.novafoundation.nova.feature_staking_impl.domain.validations.setup

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.common.stakeable
import io.novafoundation.nova.feature_staking_impl.domain.validations.maximumNominatorsReached
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

typealias SetupStakingValidationSystem = ValidationSystem<SetupStakingPayload, SetupStakingValidationFailure>
typealias SetupStakingValidationSystemBuilder = ValidationSystemBuilder<SetupStakingPayload, SetupStakingValidationFailure>

fun ValidationSystem.Companion.setupStaking(
    stakingRepository: StakingRepository,
    stakingSharedComputation: StakingSharedComputation,
): SetupStakingValidationSystem = ValidationSystem {
    enoughToPayFee()

    enoughStakeable()

    minimumBondValidation(
        stakingRepository = stakingRepository,
        stakingSharedComputation = stakingSharedComputation,
        chainAsset = { it.stashAsset.token.configuration },
        balanceToCheckAgainstRequired = {
            val decimals = it.bondAmount ?: it.stashAsset.bonded

            it.stashAsset.token.planksFromAmount(decimals)
        },
        balanceToCheckAgainstRecommended = {
            it.bondAmount?.let(it.stashAsset.token::planksFromAmount)
        },
        error = SetupStakingValidationFailure::AmountLessThanMinimum
    )

    maximumNominatorsReached(
        stakingRepository = stakingRepository,
        isAlreadyNominating = SetupStakingPayload::isOnlyChangingValidators,
        chainId = { it.stashAsset.token.configuration.chainId },
        errorProducer = { SetupStakingValidationFailure.MaxNominatorsReached }
    )
}

private fun SetupStakingValidationSystemBuilder.enoughToPayFee() {
    sufficientBalance(
        fee = { it.maxFee },
        available = {
            if (it.isControllerTransaction) {
                it.controllerAsset.transferable
            } else {
                it.stashAsset.transferable
            }
        },
        error = { _, _ -> SetupStakingValidationFailure.CannotPayFee }
    )
}

private fun SetupStakingValidationSystemBuilder.enoughStakeable() {
    sufficientBalance(
        available = { it.stashAsset.stakeable },
        amount = { it.bondAmount.orZero() },
        error = { _, _ -> SetupStakingValidationFailure.NotEnoughStakeable },
        fee = { it.stashFee }
    )
}
