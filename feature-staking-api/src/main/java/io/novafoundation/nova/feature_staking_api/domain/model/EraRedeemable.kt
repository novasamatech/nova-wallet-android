package io.novafoundation.nova.feature_staking_api.domain.model

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigInteger

interface EraRedeemable {

    val amount: Balance

    val redeemEra: EraIndex
}

fun EraRedeemable.isUnbondingIn(activeEraIndex: BigInteger) = redeemEra > activeEraIndex

fun EraRedeemable.isRedeemableIn(activeEraIndex: BigInteger) = redeemEra <= activeEraIndex
