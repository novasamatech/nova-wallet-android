package io.novafoundation.nova.feature_staking_impl.domain.common

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigDecimal

val Asset.totalStakedPlanks: Balance
    get() = bondedInPlanks + redeemableInPlanks + unbondingInPlanks
