package io.novafoundation.nova.feature_staking_impl.domain.validations.setup

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.validations.bond.sufficientCommissionBalanceToStayAboveED
import io.novafoundation.nova.feature_wallet_api.domain.model.balanceCountedTowardsED
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.feature_wallet_api.domain.validation.validate
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset

typealias SetupStakingValidationSystem = ValidationSystem<SetupStakingPayload, SetupStakingValidationFailure>
typealias SetupStakingValidationSystemBuilder = ValidationSystemBuilder<SetupStakingPayload, SetupStakingValidationFailure>

fun ValidationSystem.Companion.changeValidators(
    stakingRepository: StakingRepository,
    stakingSharedComputation: StakingSharedComputation,
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
): SetupStakingValidationSystem = ValidationSystem {
    enoughToPayFee()

    minimumBondValidation(
        stakingRepository = stakingRepository,
        stakingSharedComputation = stakingSharedComputation,
        chainAsset = { it.controllerAsset.token.configuration },
        balanceToCheckAgainstRequired = { it.controllerAsset.bondedInPlanks },
        balanceToCheckAgainstRecommended = { null }, // while changing validators we don't check against recommended minimum
        error = SetupStakingValidationFailure::AmountLessThanMinimum
    )

    sufficientCommissionBalanceToStayAboveED(enoughTotalToStayAboveEDValidationFactory)
}

private fun SetupStakingValidationSystemBuilder.enoughToPayFee() {
    sufficientBalance(
        fee = { it.maxFee },
        available = { it.controllerAsset.transferable },
        error = { SetupStakingValidationFailure.CannotPayFee }
    )
}

fun SetupStakingValidationSystemBuilder.sufficientCommissionBalanceToStayAboveED(
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
) {
    enoughTotalToStayAboveEDValidationFactory.validate(
        fee = { it.maxFee },
        balance = { it.controllerAsset.balanceCountedTowardsED() },
        chainWithAsset = { ChainWithAsset(it.chain, it.controllerAsset.token.configuration) },
        error = { payload, error -> SetupStakingValidationFailure.NotEnoughFundToStayAboveED(payload.controllerAsset.token.configuration, error) }
    )
}
