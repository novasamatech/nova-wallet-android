package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootClaimPreview
import java.math.BigInteger

data class BittensorClaimValidationPayload(
    val reviewed: BittensorRootClaimPreview,
    val current: BittensorRootClaimPreview?,
    val transferable: BigInteger,
    val fee: BigInteger,
    val existentialDeposit: BigInteger,
    val coldkeySwapPending: Boolean = false,
    val safeModeActive: Boolean = false,
    val uneconomicClaimAccepted: Boolean = false,
    val claimPending: Boolean = false
)

enum class BittensorClaimValidationFailure {
    CLAIM_PENDING,
    NOT_CLAIMABLE,
    COLDKEY_SWAP_PENDING,
    SAFE_MODE_ACTIVE,
    REWARDS_DECREASED,
    FORFEITURE_INCREASED,
    INSUFFICIENT_FEE_BALANCE,
    UNECONOMIC_CLAIM
}

typealias BittensorClaimValidation = Validation<BittensorClaimValidationPayload, BittensorClaimValidationFailure>

typealias BittensorClaimValidationSystem = ValidationSystem<BittensorClaimValidationPayload, BittensorClaimValidationFailure>
