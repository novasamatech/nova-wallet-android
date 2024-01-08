package io.novafoundation.nova.feature_account_impl.data.signer.proxy

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.runtime.extrinsic.multi.SimpleCallBuilder
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.DictEnum
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.DefaultSignedExtensions
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.Extrinsic.EncodingInstance.CallRepresentation
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.instances.AddressInstanceConstructor
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.encodeNonce
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.replaceBaseNone
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic
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

fun Map<String, Any?>.modifyNonce(runtimeSnapshot: RuntimeSnapshot, newNonce: BigInteger): Map<String, Any?> {
    return buildMap {
        putAll(this@modifyNonce)

        put(DefaultSignedExtensions.CHECK_NONCE, runtimeSnapshot.encodeNonce(newNonce))
    }
}
