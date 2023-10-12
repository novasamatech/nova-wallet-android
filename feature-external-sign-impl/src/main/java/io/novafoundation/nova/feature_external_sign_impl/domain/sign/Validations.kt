package io.novafoundation.nova.feature_external_sign_impl.domain.sign

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.validation.FeeChangeDetectedFailure
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee
import java.math.BigDecimal
import java.math.BigInteger

sealed class ConfirmDAppOperationValidationFailure {

    class FeeSpikeDetected(override val payload: FeeChangeDetectedFailure.Payload) : ConfirmDAppOperationValidationFailure(), FeeChangeDetectedFailure
}

data class ConfirmDAppOperationValidationPayload(
    val token: Token?,
    val decimalFee: DecimalFee?
)

inline fun ConfirmDAppOperationValidationPayload.convertingToAmount(planks: () -> BigInteger): BigDecimal {
    require(token != null) {
        "Invalid state - token should be present for validate transaction payload"
    }

    val feeInPlanks = planks()

    return token.amountFromPlanks(feeInPlanks)
}

typealias ConfirmDAppOperationValidationSystem = ValidationSystem<ConfirmDAppOperationValidationPayload, ConfirmDAppOperationValidationFailure>
