package io.novafoundation.nova.feature_staking_impl.domain.validations.setup

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigDecimal

class SetupStakingPayload(
    val bondAmount: BigDecimal?,
    val maxFee: BigDecimal,
    val stashAsset: Asset,
    val controllerAsset: Asset,
)

val SetupStakingPayload.isOnlyChangingValidators: Boolean
    get() = bondAmount == null

val SetupStakingPayload.isControllerTransaction: Boolean
    get() = isOnlyChangingValidators

val SetupStakingPayload.isStashTransaction: Boolean
    get() = !isControllerTransaction

val SetupStakingPayload.stashFee: BigDecimal
    get() = maxFee.takeIf { isStashTransaction }.orZero()
