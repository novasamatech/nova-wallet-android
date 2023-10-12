package io.novafoundation.nova.feature_staking_impl.domain.validations.main

import io.novafoundation.nova.common.validation.ValidationSystem

const val SYSTEM_MANAGE_STAKING_REDEEM = "ManageStakingRedeem"
const val SYSTEM_MANAGE_STAKING_BOND_MORE = "ManageStakingBondMore"
const val SYSTEM_MANAGE_STAKING_UNBOND = "ManageStakingUnbond"
const val SYSTEM_MANAGE_STAKING_REBOND = "ManageStakingRebond"
const val SYSTEM_MANAGE_REWARD_DESTINATION = "ManageStakingRewardDestination"
const val SYSTEM_MANAGE_PAYOUTS = "ManageStakingPayouts"
const val SYSTEM_MANAGE_VALIDATORS = "ManageStakingValidators"
const val SYSTEM_MANAGE_CONTROLLER = "ManageStakingController"
const val SYSTEM_MANAGE_STAKING_REBAG = "ManageStakingRebag"

typealias StakeActionsValidationSystem = ValidationSystem<StakeActionsValidationPayload, StakeActionsValidationFailure>
