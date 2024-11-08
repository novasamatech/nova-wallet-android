package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.percentageToFraction
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigInteger

typealias Percent = BigInteger

@JvmInline
value class InflationDistributionConfig(val accounts: List<InflationDistributionAccount>)

class InflationDistributionAccount(
    // Account which receives funds intended for parachain bond
    val account: AccountId,

    // Percent of inflation set aside for parachain bond account
    // Will be integer number (30%)
    val percent: Percent
)

fun InflationDistributionConfig.totalPercentAsFraction(): Double {
    return accounts.sumOf { it.percent }.toDouble().percentageToFraction()
}

fun bindParachainBondConfig(decoded: Any?): InflationDistributionConfig {
    val distributionAccount = bindInflationDistributionAccount(decoded)
    return InflationDistributionConfig(listOf(distributionAccount))
}

fun bindInflationDistributionConfig(decoded: Any?): InflationDistributionConfig {
    return InflationDistributionConfig(bindList(decoded, ::bindInflationDistributionAccount))
}

private fun bindInflationDistributionAccount(decoded: Any?): InflationDistributionAccount = decoded.castToStruct().let {
    InflationDistributionAccount(
        account = bindAccountId(it["account"]),
        percent = bindPercent(it["percent"])
    )
}

private fun bindPercent(dynamicInstance: Any?): Percent = dynamicInstance.cast()
