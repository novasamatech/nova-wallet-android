package io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.controller

import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee
import java.math.BigDecimal

class SetControllerValidationPayload(
    val stashAddress: String,
    val controllerAddress: String,
    val fee: DecimalFee,
    val transferable: BigDecimal
)
