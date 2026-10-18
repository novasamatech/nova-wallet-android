package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitPreflight
import java.math.BigInteger

data class BittensorExitValidationPayload(
    val preflight: BittensorExitPreflight,
    val quote: BittensorExitQuote,
    val transferable: BigInteger,
    val fee: BigInteger,
    val existentialDeposit: BigInteger,
    val supportsFeeFromStake: Boolean,
    val expectedFeeFromStake: Boolean? = null
) {

    val feeFromStake: Boolean
        get() = transferable < fee + existentialDeposit
}

enum class BittensorExitValidationFailure {
    ROOT_LOCKED,
    SUBNET_TRADING_DISABLED,
    COLDKEY_SWAP_PENDING,
    SAFE_MODE_ACTIVE,
    INSUFFICIENT_STAKE,
    FULL_POSITION_CHANGED,
    BELOW_MINIMUM,
    REMAINDER_BELOW_MINIMUM,
    INSUFFICIENT_FEE_BALANCE,
    FEE_SOURCE_CHANGED
}

typealias BittensorExitValidation = Validation<BittensorExitValidationPayload, BittensorExitValidationFailure>

typealias BittensorExitValidationSystem = ValidationSystem<BittensorExitValidationPayload, BittensorExitValidationFailure>
