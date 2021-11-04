package io.novafoundation.nova.feature_crowdloan_impl.data.network.api.acala

import java.math.BigInteger

class AcalaDirectContributeRequest(
    val address: String,
    val amount: BigInteger,
    val referral: String?,
    val signature: String,
)

class AcalaLiquidContributeRequest(
    val address: String,
    val amount: BigInteger,
    val referral: String?,
)
