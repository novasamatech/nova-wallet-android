package io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.controller

import io.novafoundation.nova.feature_account_api.data.model.Fee
import java.math.BigDecimal

class SetControllerValidationPayload(
    val stashAddress: String,
    val controllerAddress: String,
    val fee: Fee,
    val transferable: BigDecimal
)
