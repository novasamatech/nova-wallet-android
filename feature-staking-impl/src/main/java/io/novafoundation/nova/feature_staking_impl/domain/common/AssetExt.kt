package io.novafoundation.nova.feature_staking_impl.domain.common

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigDecimal

val Asset.totalStaked: BigDecimal
    get() = bonded + redeemable + unbonding

val Asset.stakeable: BigDecimal
    get() = free - totalStaked
