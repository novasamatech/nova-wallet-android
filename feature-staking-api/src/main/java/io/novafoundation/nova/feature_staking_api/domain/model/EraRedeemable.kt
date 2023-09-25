package io.novafoundation.nova.feature_staking_api.domain.model

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigInteger

interface EraRedeemable {

    val redeemEra: EraIndex

    companion object
}

interface RedeemableAmount : EraRedeemable {

    val amount: Balance
}

fun EraRedeemable.isUnbondingIn(activeEraIndex: BigInteger) = redeemEra > activeEraIndex

fun EraRedeemable.isRedeemableIn(activeEraIndex: BigInteger) = redeemEra <= activeEraIndex

fun EraRedeemable.Companion.of(eraIndex: EraIndex): EraRedeemable = InlineEraRedeemable(eraIndex)

@JvmInline
private value class InlineEraRedeemable(override val redeemEra: EraIndex) : EraRedeemable
