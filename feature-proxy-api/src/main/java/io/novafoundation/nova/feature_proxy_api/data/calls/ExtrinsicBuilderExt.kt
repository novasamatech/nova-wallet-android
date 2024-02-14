package io.novafoundation.nova.feature_proxy_api.data.calls

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import java.math.BigInteger
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.DictEnum
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.instances.AddressInstanceConstructor
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder

fun ExtrinsicBuilder.addProxyCall(proxyAccountId: AccountId, proxyType: ProxyType): ExtrinsicBuilder {
    return call(
        Modules.PROXY,
        "add_proxy",
        argumentsForProxy(runtime, proxyAccountId, proxyType)
    )
}

fun ExtrinsicBuilder.removeProxyCall(proxyAccountId: AccountId, proxyType: ProxyType): ExtrinsicBuilder {
    return call(
        Modules.PROXY,
        "remove_proxy",
        argumentsForProxy(runtime, proxyAccountId, proxyType)
    )
}

private fun argumentsForProxy(runtime: RuntimeSnapshot, proxyAccountId: AccountId, proxyType: ProxyType): Map<String, Any> {
    return mapOf(
        "delegate" to AddressInstanceConstructor.constructInstance(runtime.typeRegistry, proxyAccountId),
        "proxy_type" to DictEnum.Entry(proxyType.name, null),
        "delay" to BigInteger.ZERO
    )
}
