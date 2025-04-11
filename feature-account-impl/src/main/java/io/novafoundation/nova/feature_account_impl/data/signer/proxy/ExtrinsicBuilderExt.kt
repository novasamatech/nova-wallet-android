package io.novafoundation.nova.feature_account_impl.data.signer.proxy

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.composeCall
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.definitions.types.instances.AddressInstanceConstructor
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder

fun ExtrinsicBuilder.wrapCallsIntoProxy(
    proxiedAccountId: AccountId,
    proxyType: ProxyType,
) {
    val call = getWrappedCall()

    val proxyCall = runtime.composeCall(
        moduleName = Modules.PROXY,
        callName = "proxy",
        arguments = mapOf(
            "real" to AddressInstanceConstructor.constructInstance(runtime.typeRegistry, proxiedAccountId),
            "force_proxy_type" to DictEnum.Entry(proxyType.name, null),
            "call" to call
        )
    )

    resetCalls()
    call(proxyCall)
}
