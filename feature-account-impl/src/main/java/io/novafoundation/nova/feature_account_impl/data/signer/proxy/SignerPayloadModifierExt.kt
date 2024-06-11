package io.novafoundation.nova.feature_account_impl.data.signer.proxy

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.runtime.extrinsic.multi.SimpleCallBuilder
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.DefaultSignedExtensions
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.Extrinsic.EncodingInstance.CallRepresentation
import io.novasama.substrate_sdk_android.runtime.definitions.types.instances.AddressInstanceConstructor
import io.novasama.substrate_sdk_android.runtime.extrinsic.encodeNonce
import io.novasama.substrate_sdk_android.runtime.extrinsic.replaceBaseNone
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadExtrinsic
import java.math.BigInteger

fun SignerPayloadExtrinsic.wrapIntoProxyPayload(
    proxyAccountId: AccountId,
    currentProxyNonce: BigInteger,
    proxyType: ProxyType,
    callInstance: CallRepresentation.Instance
): SignerPayloadExtrinsic {
    val proxiedAccountId = accountId
    val callBuilder = SimpleCallBuilder(runtime)
    callBuilder.addCall(
        moduleName = Modules.PROXY,
        callName = "proxy",
        arguments = mapOf(
            "real" to AddressInstanceConstructor.constructInstance(runtime.typeRegistry, proxiedAccountId),
            "force_proxy_type" to DictEnum.Entry(proxyType.name, null),
            "call" to callInstance.call
        )
    )

    val newNonce = nonce.replaceBaseNone(currentProxyNonce)

    return copy(
        accountId = proxyAccountId,
        call = CallRepresentation.Instance(callBuilder.calls.first()),
        signedExtras = signedExtras.modifyNonce(runtime, newNonce.nonce),
        nonce = newNonce
    )
}

fun SignerPayloadExtrinsic.SignedExtras.modifyNonce(runtimeSnapshot: RuntimeSnapshot, newNonce: BigInteger): SignerPayloadExtrinsic.SignedExtras {
    val newIncludedInExtrinsic = buildMap {
        putAll(includedInExtrinsic)

        put(DefaultSignedExtensions.CHECK_NONCE, runtimeSnapshot.encodeNonce(newNonce))
    }

    return copy(includedInExtrinsic = newIncludedInExtrinsic)
}
