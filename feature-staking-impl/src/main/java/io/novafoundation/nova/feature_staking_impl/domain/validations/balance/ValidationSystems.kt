package io.novafoundation.nova.feature_staking_impl.domain.validations.balance

import io.novafoundation.nova.common.validation.ValidationSystem

const val SYSTEM_MANAGE_STAKING_REDEEM = "ManageStakingRedeem"
const val SYSTEM_MANAGE_STAKING_BOND_MORE = "ManageStakingBondMore"
const val SYSTEM_MANAGE_STAKING_UNBOND = "ManageStakingUnbond"
const val SYSTEM_MANAGE_STAKING_REBOND = "ManageStakingRebond"

typealias ManageStakingValidationSystem = ValidationSystem<ManageStakingValidationPayload, ManageStakingValidationFailure>
