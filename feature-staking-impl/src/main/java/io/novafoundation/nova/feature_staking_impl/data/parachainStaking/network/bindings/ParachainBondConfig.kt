package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigInteger

typealias Percent = BigInteger

class ParachainBondConfig(
    // Account which receives funds intended for parachain bond
    val account: AccountId,

    // Percent of inflation set aside for parachain bond account
    // Will be integer number (30%)
    val percent: Percent
)

fun bindParachainBondConfig(dynamicInstance: Any?): ParachainBondConfig = dynamicInstance.castToStruct().let {
    ParachainBondConfig(
        account = bindAccountId(it["account"]),
        percent = bindPercent(it["percent"])
    )
}

fun bindPercent(dynamicInstance: Any?): Percent = dynamicInstance.cast()
