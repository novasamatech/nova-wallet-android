package io.novafoundation.nova.feature_staking_api.domain.model

import java.math.BigInteger

interface EraRedeemable {

    val redeemEra: EraIndex
}

fun EraRedeemable.isUnbondingIn(activeEraIndex: BigInteger) = redeemEra > activeEraIndex

fun EraRedeemable.isRedeemableIn(activeEraIndex: BigInteger) = redeemEra <= activeEraIndex

fun EraRedeemable(redeemEra: EraIndex): EraRedeemable = InlineEraRedeemable(redeemEra)

@JvmInline
private value class InlineEraRedeemable(override val redeemEra: EraIndex) : EraRedeemable
