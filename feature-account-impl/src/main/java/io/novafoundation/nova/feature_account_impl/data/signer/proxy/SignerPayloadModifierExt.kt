package io.novafoundation.nova.feature_account_impl.data.signer.proxy

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_account_api.domain.model.ProxyAccount
import io.novafoundation.nova.runtime.extrinsic.multi.SimpleCallBuilder
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.DictEnum
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.Extrinsic.EncodingInstance.CallRepresentation
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.instances.AddressInstanceConstructor
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic

suspend fun SignerPayloadExtrinsic.wrapIntoProxyPayload(
    proxyAccountId: AccountId,
    proxyType: ProxyAccount.ProxyType,
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

    return copy(accountId = proxyAccountId, call = CallRepresentation.Instance(callBuilder.calls.first()))
}
