package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.UseCaseBinding
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.fromHexOrIncompatible
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.Type
import java.math.BigInteger

@UseCaseBinding
fun bindTotalValidatorEraReward(scale: String?, runtime: RuntimeSnapshot, type: Type<*>): BigInteger {
    val result = scale?.let { bindNumber(type.fromHexOrIncompatible(it, runtime)) }
    return result ?: incompatible()
}
