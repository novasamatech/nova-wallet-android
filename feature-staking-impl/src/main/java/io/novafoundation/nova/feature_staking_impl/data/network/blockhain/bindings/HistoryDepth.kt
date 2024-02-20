package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.UseCaseBinding
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.fromHexOrIncompatible
import io.novafoundation.nova.common.data.network.runtime.binding.storageReturnType
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import java.math.BigInteger

@UseCaseBinding
fun bindHistoryDepth(scale: String, runtime: RuntimeSnapshot): BigInteger {
    val type = runtime.metadata.storageReturnType("Staking", "HistoryDepth")

    return bindNumber(type.fromHexOrIncompatible(scale, runtime))
}
